/*
 * This file is part of the vboard distribution.
 * (https://github.com/sncf-connect-tech/vboard)
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
import com.vsct.vboard.config.AdministratorsConfig;
import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.config.WebSecurityConfig;
import com.vsct.vboard.controllers.*;
import com.vsct.vboard.models.*;
import com.vsct.vboard.services.ElasticSearchClient;
import com.vsct.vboard.services.GamificationService;
import com.vsct.vboard.services.UploadsManager;
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
import java.util.List;

import static com.vsct.vboard.TestUtil.createTestDB;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class GamificationControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private LikeDAO likeDAO;
    @Autowired
    private PinDAO pinDAO;
    @Autowired
    private CommentDAO commentDAO;
    @Autowired
    private BadgesDAO badgesDAO;
    @Autowired
    private StatsDAO statsDAO;
    @Autowired
    private LabelDAO labelDAO;
    @Autowired
    private TeamDAO teamDAO;
    @Autowired
    private SavedPinDAO savedPinDAO;
    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private AdministratorsConfig administratorsConfig;

    @Mock
    private HttpSession session;
    @Mock
    private ElasticSearchClient elsClient;
    @Mock
    private UploadsManager uploadsManager;
    @Mock
    private NotificationsController notificationsController;
    @Mock
    private  WebSecurityConfig webSecurityConfig;

    private GamificationController gamificationController;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);

        final AuthenticationController authController = new AuthenticationController(userDAO, administratorsConfig, webSecurityConfig, session);
        PinsController pinsController = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elsClient, uploadsManager, authController, gamificationController, notificationsController, new ProxyConfig());
        CommentsController commentsController = new CommentsController(jdbcTemplate, commentDAO, pinDAO, userDAO, elsClient, authController, gamificationController, notificationsController);
        this.gamificationController = new GamificationController(gamificationService, jdbcTemplate, likeDAO, commentDAO, pinDAO, badgesDAO, statsDAO, userDAO, teamDAO, savedPinDAO, authController, notificationsController);
        LikesController likesController = new LikesController(jdbcTemplate, likeDAO, pinDAO, userDAO, elsClient, authController, gamificationController);
        pinsController.deleteAllPins();
        commentsController.deleteAllComments();
        likesController.deleteAllLikes();
        this.notificationsController.deleteAllNotifications();
        this.gamificationController.deleteAllBadges();
        this.gamificationController.deleteAllStats();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(notificationsController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    public Stats createGivenStat(User u, int pinNb, int commentNb) {
        String author = u.getUserString();
        for (int i = 0; i < pinNb; i++) {
            this.pinDAO.save(new Pin("title", "", 0, "", "", "content", author, new DateTime()));
        }
        Pin pin0 = this.pinDAO.findByAuthor(author).get(0);
        for (int i = 0; i < commentNb; i++) {
            this.commentDAO.save(new Comment(pin0.getPinId(), author, "text", new DateTime()));
        }
        for (Pin pin : this.pinDAO.findAll()) {
            this.likeDAO.save(new Like(pin.getPinId() + u.getEmail(), pin.getPinId(), u.getEmail()));
        }
        return this.gamificationController.getStats(u);
    }

    @Test
    public void getUserStats() {
        User user = new User("email", "firstname", "lastname");
        this.userDAO.save(user);
        Stats stats = this.createGivenStat(user, 51, 71);
        stats.setConnexionNumber(1000);
        this.statsDAO.save(stats);
        Stats expectedStats = new Stats("email", "", 51, 51, 1, 51, 71, 71, 71, 0, 0, 1000, stats.getLastConnexion());
        Assert.assertEquals(expectedStats, this.statsDAO.findByEmail("email"));
    }

    @Test
    public void getUserBadges() {
        User user = new User("email", "firstname", "lastname");
        this.userDAO.save(user);
        Stats stats = this.createGivenStat(user, 51, 71);
        stats.setConnexionNumber(1000);
        this.statsDAO.save(stats);
        Badges badges = new Badges("email", "", 5, 3, 1, 3, 4, 5, 4, 0, 0, 7, 0);
        Assert.assertEquals(badges, this.gamificationController.getBadges(user));
    }

    @Test
    public void getUserPoints() {
        User u = new User("email", "firstname", "lastname");
        this.userDAO.save(u);
        Stats stats = this.createGivenStat(u, 51, 71);
        stats.setConnexionNumber(1000);
        this.statsDAO.save(stats);
        stats = this.gamificationController.getUserPointsStats("email");
        Stats points = new Stats("email", "", (int) (51 * GamificationController.PINS_WEIGHT * 10), (int) (51 * GamificationController.LIKES_NUMBER_GET_WEIGHT * 10),
                (int) (1 * GamificationController.LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10), (int) (51 * GamificationController.LIKES_WEIGHT * 10),
                (int) (71 * GamificationController.COMMENTS_NUMBER_GET_WEIGHT * 10), (int) (71 * GamificationController.COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10),
                (int) (71 * GamificationController.COMMENTS_WEIGHT * 10), 0, 0, (int) (1000 * GamificationController.CONNEXIONS_WEIGHT * 10), stats.getLastConnexion());
        Assert.assertEquals(points, stats);
    }

    @Test
    public void getStatsPercentage() {
        User user = new User("email", "firstname", "lastname");
        this.userDAO.save(user);

        Stats stats = this.createGivenStat(user, 51, 71);
        stats.setConnexionNumber(1000);
        this.statsDAO.save(stats);

        Stats expectedStats = new Stats("email", "", 2, 53, 44, 53, 28, 98, 75, 0, 0, 66, stats.getLastConnexion());

        stats = this.gamificationController.getStatsPercentage(user);
        Assert.assertEquals(expectedStats, stats);
    }

    @Test
    public void trackAnonymousConnection() {
        Assert.assertEquals(1, this.gamificationController.trackConnection());
    }

    @Test
    public void trackAuthentifiedConnection() {
        final String userEmail = "dummy@no.nym";
        final User user = new User(userEmail, "First name", "Last name");
        this.userDAO.save(user);
        Mockito.doReturn(user).when(session).getAttribute("User");

        Assert.assertEquals(user, this.userDAO.findByEmail(userEmail));

        Assert.assertEquals(1, this.gamificationController.trackConnection());
        Assert.assertNotNull(this.statsDAO.findByEmail(userEmail));

        final DateTime userLastConnexion = new DateTime(this.statsDAO.findByEmail(userEmail).getLastConnexion());
        Assert.assertTrue(new DateTime().minusMinutes(1).isBefore(userLastConnexion));
    }

    @Test
    public void getLeaderBoard() {
        this.gamificationController.deleteAllStats();
        User[] users = new User[11];
        for (int i = 0; i < 11; i++) {
            users[i] = new User("email" + i, "firstname" + i, "lastname" + i);
            this.userDAO.save(users[i]);
            Stats stats = this.createGivenStat(users[i], 5 + i, 50 - i);
            stats.setConnexionNumber(i % 5);
            this.statsDAO.save(stats);
            Pin pin = this.pinDAO.findByAuthor(users[i].getUserString()).get(0);
            for (int j = 0; j < 50 * i; j++) {
                this.likeDAO.save(new Like(pin.getPinId() + j, pin.getPinId(), j + ""));
            }
        }
        List<Profil> pinsPosted = new ArrayList<>();
        List<Profil> likesReceived = new ArrayList<>();
        List<Profil> likesReceivedForOnePin = new ArrayList<>();
        List<Profil> likesPosted = new ArrayList<>();
        List<Profil> commentReceived = new ArrayList<>();
        List<Profil> commentsReceivedForOnePin = new ArrayList<>();
        List<Profil> commentsPosted = new ArrayList<>();
        List<Profil> connexions = new ArrayList<>();
        for (int i = 10; i > 0; i--) {
            pinsPosted.add(users[i]);
            likesReceived.add(users[i]);
            likesReceivedForOnePin.add(users[i]);
            likesPosted.add(users[i]);
        }
        for (int i = 0; i < 10; i++) {
            commentReceived.add(users[i]);
            commentsReceivedForOnePin.add(users[i]);
            commentsPosted.add(users[i]);
        }
        for (int i = 9; i >= 6; i--) {
            connexions.add(users[i]);
            connexions.add(users[i - 5]);
        }
        connexions.add(users[10]);
        connexions.add(users[5]);

        LeaderBoard leaderBoard = new LeaderBoard(pinsPosted, likesReceived, likesReceivedForOnePin, likesPosted,
                commentReceived, commentsReceivedForOnePin, commentsPosted, connexions, null, null);
        Assert.assertEquals(leaderBoard, this.gamificationController.getLeaderBoard());
    }

}
