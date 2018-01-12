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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import com.vsct.vboard.DAO.*;
import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.controllers.*;
import com.vsct.vboard.models.*;
import com.vsct.vboard.parameterFormat.LikeParams;
import com.vsct.vboard.services.*;
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

import javax.servlet.http.HttpSession;
import java.util.ArrayList;

import static com.vsct.vboard.TestUtil.createTestDB;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class LikesControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    @Autowired
    private ObjectMapper jsonMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private LikeDAO likeDAO;
    @Autowired
    private PinDAO pinDAO;
    @Mock
    private ElasticSearchClient elkClient;
    @Mock
    private UploadsManager uploadsManager;
    @Autowired
    private CommentDAO commentDAO;
    @Autowired
    private LabelDAO labelDAO;
    @Autowired
    private SavedPinDAO savedPinDAO;
    private LikesController likesController;
    @Mock
    private AuthenticationController permission;
    @Mock
    private GamificationController gamification;
    @Mock
    private NotificationsController notifications;
    @Mock
    HttpSession session;
    @Mock
    private ProxyConfig proxyConfig;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);
        PinsController pinsController = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elkClient, uploadsManager, permission, gamification, notifications, proxyConfig);
        this.likesController = new LikesController(jdbcTemplate, likeDAO, pinDAO, userDAO, elkClient, permission, gamification);
        pinsController.deleteAllPins();
        this.likesController.deleteAllLikes();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(likesController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    @Test
    public void getLikesFromAuthor() {
        Like like = new Like("id", "pin", "author");
        Like like2 = new Like("id2", "pin", "author");
        Like like3 = new Like("id3", "pin", "author2");
        ArrayList<Like> list = new ArrayList<>();
        Assert.assertEquals(list.toString(), likesController.getLikesFromAuthor("author"));
        this.likeDAO.save(like);
        list.add(like);
        Assert.assertEquals(list.toString(), likesController.getLikesFromAuthor("author"));
        this.likeDAO.save(like2);
        list.add(like2);
        this.likeDAO.save(like3);
        Assert.assertEquals(list.toString(), likesController.getLikesFromAuthor("author"));
    }

    @Test
    public void getLikesFromPin() {
        Like like = new Like("id1", "pin", "author");
        Like like1 = new Like("id2", "pin", "author");
        Like like2 = new Like("id3", "pin2", "author");
        Like like3 = new Like("id4", "pin", "author2");
        ArrayList<Like> list = new ArrayList<>();
        Assert.assertEquals(list.toString(), likesController.getLikesFromPin("pin"));
        this.likeDAO.save(like);
        list.add(like);
        Assert.assertEquals(list.toString(), likesController.getLikesFromPin("pin"));
        this.likeDAO.save(like1);
        list.add(like1);
        this.likeDAO.save(like2);
        this.likeDAO.save(like3);
        list.add(like3);
        Assert.assertEquals(list.toString(), likesController.getLikesFromPin("pin"));
    }

    @Test
    public void addNewLikeInLikesDB() {
        User.getEmailFromString(Mockito.anyString());
        this.pinDAO.save(new Pin("vboard-id", "", "", 0, "", "", "content", "", new DateTime()));
        Assert.assertEquals(0, this.pinDAO.findByPinId("vboard-id").getLikes());
        Assert.assertNull(this.likeDAO.findById("vboard-idemail"));
        Assert.assertEquals(0, this.likeDAO.findByPin("vboard-id").size());
        Assert.assertEquals(0, this.likeDAO.findByAuthor("email").size());
        this.likesController.addNewLikeInLikesDB(new LikeParams("email", "vboard-id"));
        Assert.assertNotNull(this.likeDAO.findById("vboard-idemail"));
        Assert.assertEquals(1, this.likeDAO.findByPin("vboard-id").size());
        Assert.assertEquals(1, this.likeDAO.findByAuthor("email").size());
        Assert.assertEquals(1, this.pinDAO.findByPinId("vboard-id").getLikes());
        this.likesController.addNewLikeInLikesDB(new LikeParams("email", "vboard-id"));
        Assert.assertEquals(1, this.pinDAO.findByPinId("vboard-id").getLikes());
    }

    @Test
    public void removeLike() {
        User.getEmailFromString(Mockito.anyString());
        this.pinDAO.save(new Pin("vboard-id", "", "", 0, "", "", "content", "", new DateTime()));
        Assert.assertEquals(0, this.pinDAO.findByPinId("vboard-id").getLikes());
        Assert.assertEquals(0, this.likeDAO.findByPin("vboard-id").size());
        Assert.assertEquals(0, this.likeDAO.findByAuthor("email").size());
        this.likesController.addNewLikeInLikesDB(new LikeParams("email", "vboard-id"));
        Assert.assertEquals(1, this.likeDAO.findByPin("vboard-id").size());
        Assert.assertEquals(1, this.likeDAO.findByAuthor("email").size());
        Assert.assertEquals(1, this.pinDAO.findByPinId("vboard-id").getLikes());
        this.likesController.removeLike("vboard-id", "email");
        Assert.assertEquals(0, this.likeDAO.findByPin("vboard-id").size());
        Assert.assertEquals(0, this.likeDAO.findByAuthor("email").size());
        Assert.assertEquals(0, this.pinDAO.findByPinId("vboard-id").getLikes());
        this.likesController.removeLike("vboard-id", "email");
        Assert.assertEquals(0, this.pinDAO.findByPinId("vboard-id").getLikes());

    }

}