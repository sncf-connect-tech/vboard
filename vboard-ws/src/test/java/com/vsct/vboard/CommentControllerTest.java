/*
 * This file is part of the vboard distribution.
 * (https://github.com/voyages-sncf-technologies/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsct.vboard;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import com.vsct.vboard.DAO.*;
import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.controllers.*;
import com.vsct.vboard.models.Comment;
import com.vsct.vboard.models.Pin;
import com.vsct.vboard.models.User;
import com.vsct.vboard.parameterFormat.CommentParams;
import com.vsct.vboard.services.ElasticSearchClient;
import com.vsct.vboard.services.UploadsManager;
import org.apache.catalina.connector.Request;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static com.vsct.vboard.TestUtil.createTestDB;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class CommentControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private PinDAO pinDAO;
    @Autowired
    private LikeDAO likeDAO;
    @Autowired
    private CommentDAO commentDAO;
    @Autowired
    private LabelDAO labelDAO;
    @Autowired
    private SavedPinDAO savedPinDAO;
    private PinsController pinsController;
    @Mock
    private UploadsManager uploadsManager;
    @Mock
    private ElasticSearchClient elsClient;
    private CommentsController commentsController;
    @Mock
    private AuthenticationController permission;
    @Mock
    private GamificationController gamification;
    @Mock
    private NotificationsController notifications;
    @Mock
    private ProxyConfig proxyConfig;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);
        this.pinsController = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elsClient, uploadsManager, permission, gamification, notifications, proxyConfig);
        this.commentsController = new CommentsController(jdbcTemplate, commentDAO, pinDAO, userDAO, elsClient, permission, gamification, notifications);
        this.pinsController.deleteAllPins();
        this.commentsController.deleteAllComments();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(commentsController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    @Test
    public void getCommentsFromPin() {
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        Pin pin = pinDAO.findAll().iterator().next();
        Comment comment = new Comment("3", pin.getPinId(), "auth", "comment", new DateTime().toString());
        ArrayList<Comment> listComment = new ArrayList<>();
        listComment.add(comment);
        this.commentDAO.save(comment);
        this.commentDAO.save(new Comment("5", "2", "auth", "comment2", new DateTime().toString()));
        this.commentDAO.save(new Comment("2", "3", "auth3", "comment2", new DateTime().toString()));
        Assert.assertEquals(listComment.toString(), commentsController.getCommentsFromPin(pin.getPinId()));
    }

    @Test
    public void addComment() {
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        Pin pin = pinDAO.findAll().iterator().next();
        Assert.assertEquals(0, this.commentDAO.findByPin("0").size());
        User.getEmailFromString(Mockito.anyString());
        commentsController.addComment(new CommentParams("auth", pin.getPinId(), "text"));
        Assert.assertEquals(1, this.commentDAO.findByPin("0").size());
        commentsController.addComment(new CommentParams("auth", pin.getPinId(), "text"));
        commentsController.addComment(new CommentParams("auth", pin.getPinId(), "text"));
        commentsController.addComment(new CommentParams("auth", "unknownpinid", "text"));
        Assert.assertEquals(3, this.commentDAO.findByPin("0").size());
        Assert.assertEquals(3, this.pinDAO.findByPinId("0").getCommentsNumber());
    }

    @Test
    public void addCommentFromVblog() {
        User.getEmailFromString(Mockito.anyString());
        this.pinDAO.save(new Pin("vblog-0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        Assert.assertEquals(0, this.commentDAO.findByPin("vblog-0").size());
        commentsController.addCommentFromVblog("text", "0", "author", "ID", new Request());
        Assert.assertEquals(1, this.commentDAO.findByPin("vblog-0").size());
        commentsController.addCommentFromVblog("text", "0", "author", "ID", new Request());
        commentsController.addCommentFromVblog("text", "4", "author", "ID", new Request()); // other pinID
        commentsController.addCommentFromVblog("text", "0", "author", "ID2", new Request());
        commentsController.addCommentFromVblog("text", "0", "author", "ID3", new Request());
        commentsController.addCommentFromVblog("text", "0", "author", "ID", new Request());  // update
        Assert.assertEquals(3, this.commentDAO.findByPin("vblog-0").size());
        Assert.assertEquals(3, this.pinDAO.findByPinId("vblog-0").getCommentsNumber());
    }

    @Test
    public void updateComment() {
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        Comment comment = new Comment("id", "0", "auth", "comment", new DateTime().toString());
        this.commentDAO.save(comment);
        this.commentsController.updateComment("{\"text\": \"comment update\"}", "id");
        Comment updateComment = this.commentDAO.findById("id");
        Assert.assertNotEquals(comment, updateComment);
        Assert.assertEquals(comment.getAuthor(), updateComment.getAuthor());
        Assert.assertEquals(comment.getPostDateUTC(), updateComment.getPostDateUTC());
        Assert.assertEquals("comment update", updateComment.getText());
        Assert.assertEquals(0, this.pinDAO.findByPinId("0").getCommentsNumber());
    }

    @Test
    public void deleteComment() {
        User.getEmailFromString(Mockito.anyString());
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime().toString(), 1));
        Comment comment = new Comment("id", "0", "auth", "comment", new DateTime().toString());
        Assert.assertFalse(this.commentDAO.findAll().iterator().hasNext());
        this.commentDAO.save(comment);
        Assert.assertTrue(this.commentDAO.findAll().iterator().hasNext());
        this.commentsController.removeComment("id");
        Assert.assertFalse(this.commentDAO.findAll().iterator().hasNext());
        Assert.assertEquals(0, this.pinDAO.findByPinId("0").getCommentsNumber());

        this.pinDAO.delete("0");
        comment = new Comment("id", "0", "auth", "comment", new DateTime().toString());
        Assert.assertFalse(this.commentDAO.findAll().iterator().hasNext());
        this.commentDAO.save(comment);
        Assert.assertTrue(this.commentDAO.findAll().iterator().hasNext());
        this.commentsController.removeComment("id");
        Assert.assertFalse(this.commentDAO.findAll().iterator().hasNext());
        Assert.assertNull(this.pinDAO.findByPinId("0"));

        this.pinDAO.save(new Pin("0", "title", "", 1, "", "", "content", "auth", new DateTime()));
        this.commentDAO.save(comment);
        Assert.assertTrue(this.commentDAO.findAll().iterator().hasNext());
        ArrayList<Pin> pins = new ArrayList<>();
        pins.add(this.pinDAO.findByPinId("0"));
        Mockito.doReturn(pins).when(elsClient).searchPinsById("0");
        this.pinsController.deletePinFromId("0");
        Assert.assertFalse(this.commentDAO.findAll().iterator().hasNext());

    }


}