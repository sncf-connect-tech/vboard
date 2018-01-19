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
import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.controllers.AuthenticationController;
import com.vsct.vboard.controllers.NotificationsController;
import com.vsct.vboard.controllers.TeamsController;
import com.vsct.vboard.controllers.UsersController;
import com.vsct.vboard.models.User;
import com.vsct.vboard.parameterFormat.UserParams;
import com.vsct.vboard.parameterFormat.UserParamsUpdate;
import com.vsct.vboard.services.UploadsManager;
import org.apache.http.HttpStatus;
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
import java.util.Iterator;
import java.util.Set;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.vsct.vboard.TestUtil.createTestDB;
import static com.vsct.vboard.TestUtil.dummyUserGenerator;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class UserControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    private UsersController userController;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDAO userDAO;
    @Mock
    private UploadsManager uploadsManager;
    @Mock
    private AuthenticationController permission;
    @Mock
    private TeamsController teamsController;
    @Mock
    private NotificationsController notificationsController;
    @Mock
    HttpSession session;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);
        this.userController = new UsersController(jdbcTemplate, userDAO, uploadsManager, permission, teamsController, notificationsController);
        userController.deleteAllUsers();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    @Test
    public void search() throws Exception {
        Iterator<User> dummyUserIterator = dummyUserGenerator();
        this.userDAO.save(new User("test@voyages-sncf.com", "fistname", "lastname"));
        this.userDAO.save(new User("test@VOYAGES-SNCF.COM", "fistname", "lastname"));
        for (int i = 0; i < 10; i++) {
            User dummyUser = dummyUserIterator.next();
            this.userDAO.save(dummyUser);
        }

        given().when().get("/users/test@voyages-sncf.com").then().statusCode(HttpStatus.SC_OK);
        given().when().get("/users/test@VOYAGES-SNCF.COM").then().statusCode(HttpStatus.SC_OK);
        given().when().get("/users/unknown@voyages-sncf.com").then().statusCode(HttpStatus.SC_OK);

        String response = userController.getUserFromEmail("test@voyages-sncf.com");
        Assert.assertNotNull(response);

        response = userController.getUserFromEmail("test@VOYAGES-SNCF.COM");
        Assert.assertNotNull(response);

        response = userController.getUserFromEmail("unknown@voyages-sncf.com");
        Assert.assertNull(response);
    }

    @Test
    public void add() throws Exception {

        String response = userController.getUserFromEmail("newAddEmail@vsct");
        Assert.assertNull(response);

        UserParams params = new UserParams("newAddEmail@vsct", "firstname", "lastname");
        userController.addNewUser(params);

        User user = new User("newAddEmail@vsct", "firstname", "lastname");
        user.setLastConnection(this.userDAO.findByEmail("newAddEmail@vsct").getLastConnection());
        response = userController.getUserFromEmail("newAddEmail@vsct");
        Assert.assertNotNull(response);
        Assert.assertEquals(response, user.toString());

    }

    @Test
    public void update() throws Exception {
        Mockito.doReturn("ok").when(session).getAttribute("User");
        User user = new User("newAddEmail@vsct", "firstname", "lastname");
        this.userDAO.save(user);
        UserParamsUpdate params = new UserParamsUpdate("newAddEmail@vsct", "avatar", "team", false, false, false, false, "info");
        userController.updateUser(params);
        User getUser = this.userDAO.findByEmail("newAddEmail@vsct");
        Assert.assertEquals(user.getFirstName(), getUser.getFirstName());
        Assert.assertEquals(user.getLastName(), getUser.getLastName());
        Assert.assertNotEquals(user.getTeam(), getUser.getTeam());
        Assert.assertNotEquals(user.isReceiveLeaderboardEmails(), getUser.isReceiveLeaderboardEmails());
        Assert.assertNotEquals(user.isReceiveNlEmails(), getUser.isReceiveNlEmails());
        Assert.assertNotEquals(user.isReceivePopularPinsEmails(), getUser.isReceivePopularPinsEmails());
        Assert.assertNotEquals(user.isReceiveRecapEmails(), getUser.isReceiveRecapEmails());
        Assert.assertNotEquals(user.getInfo(), getUser.getInfo());
        Assert.assertEquals(getUser.getInfo(), "info");
        Assert.assertEquals(getUser.getTeam(), "team");
        Assert.assertEquals(getUser.hasCustomAvatar(), true);

        params = new UserParamsUpdate("newAddEmail@vsct", "default", "team", false, false, false, false, "info");
        userController.updateUser(params);
        getUser = this.userDAO.findByEmail("newAddEmail@vsct");
        Assert.assertEquals(getUser.hasCustomAvatar(), false);
    }

    @Test
    public void teamGetAll() {
        Set<String> teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 0);
        this.userDAO.save(new User("email", "fname", "lname", false, "team1", "info"));
        teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 1);
        this.userDAO.save(new User("email2", "fname", "lname", false, "team1", "info"));
        teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 1);
        this.userDAO.save(new User("email3", "fname", "lname", false, "team2", "info"));
        teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 2);
        this.userDAO.save(new User("email4", "fname", "lname", false, "team1,team2,team3", "info"));
        teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 3);
        this.userDAO.save(new User("email5", "fname", "lname", false, "team4,team5,team6", "info"));
        teams = userController.getTeams();
        Assert.assertEquals(teams.size(), 6);
    }

    @Test
    public void favoriteLabels() {
        this.userDAO.save(new User("email", "fname", "lname", false, "team1", "info"));
        this.userDAO.save(new User("email2", "fname", "lname", false, "team1", "info"));
        User user = this.userDAO.findByEmail("email");
        Mockito.doReturn(user).when(permission).getSessionUserWithSyncFromDB();
        User user2 = this.userDAO.findByEmail("email2");
        Assert.assertEquals("", user.getFavoriteLabels());
        Assert.assertEquals("", user2.getFavoriteLabels());
        this.userController.updateFavoriteLabels("{\"labels\": \"#label\"}");
        Mockito.doReturn(user2).when(permission).getSessionUserWithSyncFromDB();
        this.userController.updateFavoriteLabels("{\"labels\": \"#label1,#label2\"}");
        user = this.userDAO.findByEmail("email");
        user2 = this.userDAO.findByEmail("email2");
        Assert.assertEquals(user.getFavoriteLabels(), "#label");
        Assert.assertEquals(user2.getFavoriteLabels(), "#label1,#label2");
    }

}