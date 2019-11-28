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
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import com.vsct.vboard.DAO.*;
import com.vsct.vboard.config.UploadsConfig;
import com.vsct.vboard.controllers.AuthenticationController;
import com.vsct.vboard.controllers.GamificationController;
import com.vsct.vboard.controllers.NotificationsController;
import com.vsct.vboard.controllers.PinsController;
import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.models.VBoardException;
import com.vsct.vboard.parameterFormat.AddNewPinParams;
import com.vsct.vboard.models.Pin;
import com.vsct.vboard.services.*;
import org.apache.http.HttpStatus;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.vsct.vboard.TestUtil.createTestDB;
import static com.vsct.vboard.TestUtil.dummyPinGenerator;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class PinsControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    private ProxyConfig proxyConfig = new ProxyConfig();
    private UploadsManager uploadsManager = new UploadsManager(new UploadsConfig(), proxyConfig);
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PinDAO pinDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private LikeDAO likeDAO;
    @Autowired
    private CommentDAO commentDAO;
    @Autowired
    private LabelDAO labelDAO;
    @Autowired
    private SavedPinDAO savedPinDAO;
    @Mock
    private ElasticSearchClient elsClient;
    @Mock
    private AuthenticationController permission;
    @Mock
    private GamificationController gamification;
    @Mock
    private NotificationsController notifications;
    private PinsController pinsController;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);
        this.pinsController = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elsClient, uploadsManager, permission, gamification, notifications, proxyConfig);
        pinsController.deleteAllPins();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(pinsController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    @Test
    public void searchAll() {
        Iterator<Pin> dummyPinIterator = dummyPinGenerator();
        for (int i = 0; i < 10; i++) {
            Pin dummyPin = dummyPinIterator.next();
            this.pinDAO.save(dummyPin);
        }

        //@formatter:off
        //String json =
        given()
            .param("text", "")
            .param("offset", 0)
            .param("from", "")
        .when()
            .get("/pins")
        .then()
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void addNewPinWithUrlencParamsFails() {
        //@formatter:off
        given()
            .contentType(ContentType.URLENC)
            .param("url", "http://twitter.com/dummy/status/0")
            .param("labels", "labels")
        .when()
            .post("/pins")
        .then()
            .statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        //@formatter:on
    }

    @Test
    public void addNewPinWithJsonParamsFails() {
        //@formatter:off
        given()
            .contentType(ContentType.JSON)
            .param("url", "http://twitter.com/dummy/status/0")
            .param("labels", "labels")
        .when()
            .post("/pins")
        .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
        //@formatter:on
    }

    @Test
    public void addNewPinWithMissingParamsFails() {
        //@formatter:off
        Map<String , String> params = new HashMap<String , String>() {{
            put("labels", "tag1,tag2");
        }};
        given()
            .contentType("application/json")
            .body(params)
        .when()
            .post("/pins")
        .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
        //@formatter:on
    }

    @Test
    public void addNewPinWithUtf8JsonContentType() {
        //@formatter:off
        Mockito.doReturn(null).when(permission).getSessionUserWithSyncFromDB();
        Map<String , Object> params = new HashMap<String , Object>() {{
            put("title", "title");
            put("url", "http://twitter.com/dummy/status/0");
            put("description", "content");
            put("author", "authorEmail,Fname,Lname");
            put("labels", new String[] {"unused-labels"});
        }};
        given()
            .contentType("application/json; charset=UTF-8")
            .body(params)
        .when()
            .post("/pins")
        .then()
            .statusCode(HttpStatus.SC_OK);
        Pin pin =  this.pinDAO.findAll().iterator().next();
        assertEquals(pin.getPinTitle(), params.get("title"));
        assertEquals(pin.getHrefUrl(), params.get("url"));
        assertEquals(pin.getIndexableTextContent(), params.get("description"));
        //@formatter:on
    }

    @Test
    public void delete() {
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        ArrayList<Pin> pins = new ArrayList<>();
        pins.add(this.pinDAO.findByPinId("0"));
        Mockito.doReturn(pins).when(elsClient).searchPinsById("0");
        Assert.assertNotNull(this.pinDAO.findByPinId("0"));
        this.pinsController.deletePinFromId("0");
        Assert.assertNull(this.pinDAO.findByPinId("0"));
    }

    @Test
    public void update() throws IOException{
        this.pinDAO.save(new Pin("0", "title", "", 0, "", "", "content", "auth", new DateTime()));
        String [] labels = {"label"};
        ArrayList<Pin> pins = new ArrayList<>();
        pins.add(this.pinDAO.findByPinId("0"));
        Mockito.doReturn(pins).when(elsClient).searchPinsById("0");
        try {
            this.pinsController.updatePin(new AddNewPinParams("titleupdate", "url", "imgtype", "contentupdate", labels, "auth"), "notfound");
            Assert.fail("Epingle non trouvee, sans erreur");
        } catch (VBoardException e) {
        }

        Assert.assertEquals(this.pinDAO.findByPinId("0").getPinTitle(), "title");

        this.pinsController.updatePin(new AddNewPinParams("titleupdate", "url", "imgtype", "contentupdate", labels, "author"), "0");
        Pin pin = this.pinDAO.findByPinId("0");
        Assert.assertEquals("titleupdate", pin.getPinTitle());
        Assert.assertEquals("url", pin.getHrefUrl());
        Assert.assertEquals("/pinImg/0.png", pin.getImgType());
        Assert.assertEquals("contentupdate", pin.getIndexableTextContent());
        Assert.assertEquals("label", pin.getLabels());
        Assert.assertEquals("auth", pin.getAuthor());

        this.pinsController.updatePin(new AddNewPinParams("titleupdate", "url", null, "contentupdate", labels, "author"), "0");
        pin = this.pinDAO.findByPinId("0");
        Assert.assertNull(pin.getImgType());
    }


}