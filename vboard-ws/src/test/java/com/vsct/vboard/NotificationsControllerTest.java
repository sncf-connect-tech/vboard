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
import com.vsct.vboard.config.UploadsConfig;
import com.vsct.vboard.controllers.*;
import com.vsct.vboard.models.*;
import com.vsct.vboard.parameterFormat.AddNewPinParams;
import com.vsct.vboard.parameterFormat.CommentParams;
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.vsct.vboard.TestUtil.createTestDB;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT, classes = MainController.class)
@ActiveProfiles(profiles = "test")
public class NotificationsControllerTest {

    @Value("${local.server.port}")
    public int webServerPort;

    private ProxyConfig proxyConfig = new ProxyConfig();
    private UploadsManager uploadsManager = new UploadsManager(new UploadsConfig(), proxyConfig);
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
    private NotificationDAO notificationDAO;
    @Autowired
    private GamificationService gamificationService;
    @Autowired
    private NotificationsController notificationsController;
    @Mock
    private AuthenticationController permission;
    @Mock
    private ElasticSearchClient elsClient;
    private PinsController pinsController;
    private CommentsController commentsController;
    private GamificationController gamificationController;

    @Before
    public void setUp() {
        createTestDB();

        MockitoAnnotations.initMocks(this);
        this.pinsController = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elsClient, uploadsManager, permission, gamificationController, notificationsController, proxyConfig);
        this.commentsController = new CommentsController(jdbcTemplate, commentDAO, pinDAO, userDAO, elsClient, permission, gamificationController, notificationsController);
        LikesController likesController = new LikesController(jdbcTemplate, likeDAO, pinDAO, userDAO, elsClient, permission, gamificationController);
        this.notificationsController = new NotificationsController(jdbcTemplate, notificationDAO, permission, pinDAO, likeDAO, userDAO, commentDAO);
        this.gamificationController = new GamificationController(gamificationService, jdbcTemplate, likeDAO, commentDAO, pinDAO, badgesDAO, statsDAO, userDAO, teamDAO, savedPinDAO, permission, notificationsController);
        this.pinsController.deleteAllPins();
        this.commentsController.deleteAllComments();
        likesController.deleteAllLikes();
        this.notificationsController.deleteAllNotifications();
        this.gamificationController.deleteAllBadges();
        this.gamificationController.deleteAllStats();

        RestAssuredMockMvc.mockMvc = MockMvcBuilders.standaloneSetup(notificationsController).build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.port = webServerPort;
    }

    public User createUser(String email, String firstName, String lastName) {
        User u = new User(email, firstName, lastName);
        this.userDAO.save(u);
        return u;
    }

    @Test
    public void getAllNotifications() {
        User u = this.createUser("email", "firstname", "lastname");
        Mockito.doReturn(u).when(permission).getSessionUser();
        Notification notif1 = new Notification("email", "link", "message", "type", "from", false, false);
        Notification notif2 = new Notification("email", "link2", "message2", "type2", "from2", false, false);
        Notification notif3 = new Notification("email3", "link3", "message3", "type3", "from3", false, false);
        this.notificationDAO.save(notif1);
        this.notificationDAO.save(notif2);
        this.notificationDAO.save(notif3);
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notif1);
        notifications.add(notif2);
        Collections.sort(notifications, Comparator.comparing(Notification::getId));
        Assert.assertEquals(notifications.toString(), this.notificationsController.getAllNotifications());
    }

    @Test
    public void getUnClickedNotifications() {
        User u = this.createUser("email", "firstname", "lastname");
        Mockito.doReturn(u).when(permission).getSessionUser();
        Notification notif1 = new Notification("email", "link", "message", "type", "from", true, true);
        Notification notif2 = new Notification("email", "link2", "message2", "type2", "from2", true, true);
        Notification notif3 = new Notification("email", "link3", "message3", "type3", "from3", false, false);
        Notification notif4 = new Notification("email", "link4", "message4", "type4", "from4", false, false);
        Notification notif5 = new Notification("email5", "link5", "message5", "type5", "from5", false, false);
        Notification notif6 = new Notification("email6", "link", "message", "type", "fro5", false, true);
        this.notificationDAO.save(notif1);
        this.notificationDAO.save(notif2);
        this.notificationDAO.save(notif5);
        this.notificationDAO.save(notif6);
        Assert.assertEquals(new ArrayList<>().toString(), this.notificationsController.getUnClickedNotifications());
        this.notificationDAO.save(notif3);
        this.notificationDAO.save(notif4);
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notif3);
        notifications.add(notif4);
        Collections.sort(notifications, Comparator.comparing(Notification::getId));
        Assert.assertEquals(notifications.toString(), this.notificationsController.getUnClickedNotifications());
    }

    @Test
    public void getUnSeenNotifications() {
        User u = this.createUser("email2", "firstname", "lastname");
        Mockito.doReturn(u).when(permission).getSessionUser();
        Notification notif1 = new Notification("email2", "link", "message", "type", "from", true, true);
        Notification notif2 = new Notification("email2", "link2", "message2", "type2", "from2", true, true);
        Notification notif3 = new Notification("email2", "link3", "message3", "type3", "from3", false, false);
        Notification notif4 = new Notification("email2", "link4", "message4", "type4", "from4", false, false);
        Notification notif5 = new Notification("email2", "link5", "message5", "type5", "from5", true, false);
        Notification notif6 = new Notification("email6", "link", "message", "type", "from", false, false);
        this.notificationDAO.save(notif1);
        this.notificationDAO.save(notif2);
        this.notificationDAO.save(notif5);
        this.notificationDAO.save(notif6);
        Assert.assertEquals(new ArrayList<>().toString(), this.notificationsController.getUnSeenNotifications());
        this.notificationDAO.save(notif3);
        this.notificationDAO.save(notif4);
        List<Notification> notifications = new ArrayList<>();
        notifications.add(notif3);
        notifications.add(notif4);
        Collections.sort(notifications, Comparator.comparing(Notification::getId));
        Assert.assertEquals(notifications.toString(), this.notificationsController.getUnSeenNotifications());
    }

    @Test
    public void setSeenNotification() {
        User u = this.createUser("email3", "firstname", "lastname");
        Mockito.doReturn(u).when(permission).getSessionUser();
        Notification notif1 = new Notification("email3", "link", "message", "type", "from", false, false);
        this.notificationDAO.save(notif1);
        Assert.assertEquals(notif1, this.notificationDAO.findById(notif1.getId()));
        this.notificationsController.setSeenNotification(notif1.getId());
        notif1.setSeen(true);
        Assert.assertEquals(notif1, this.notificationDAO.findById(notif1.getId()));
    }

    @Test
    public void setClickedNotification() {
        User u = this.createUser("email4", "firstname", "lastname");
        Mockito.doReturn(u).when(permission).getSessionUser();
        Notification notif1 = new Notification("email4", "link", "message", "type", "from", false, false);
        this.notificationDAO.save(notif1);
        Assert.assertEquals(notif1, this.notificationDAO.findById(notif1.getId()));
        this.notificationsController.setClickedNotification(notif1.getId());
        notif1.setClicked(true);
        Assert.assertEquals(notif1, this.notificationDAO.findById(notif1.getId()));
    }

    @Test
    public void addNotificationsFromPin() {
        User u = this.createUser("emailNotif", "firstname", "lastname");
        User from = this.createUser("emailNotifFrom", "firstname", "lastname");
        u.setFavoriteLabels("#follow");
        this.userDAO.save(u);
        String[] labels = {"#follow"};

        this.checkCleanNotifications(u);

        this.postPinByAuthor(labels);

        Pin pinNotif = this.pinDAO.findAll().iterator().next();
        Notification notif = this.createPinNotificationAsAuthor(from, pinNotif);

        this.assertNotification(notif, from, "emailNotif", "#/?id=" + pinNotif.getPinId(), "a ajouté une épingle avec un label que vous suivez", "pin");
    }

    public void assertNotification(Notification notif, User from, String email, String link, String message, String type) {
        Assert.assertEquals(email , notif.getEmail());
        Assert.assertEquals(link , notif.getLink());
        Assert.assertEquals(message , notif.getMessage());
        Assert.assertEquals(type , notif.getType());
        Assert.assertEquals(from.getUserString() , notif.getFromUser());
        Assert.assertTrue(new DateTime(notif.getDate()).minusMinutes(2).isBefore(new DateTime()));
    }

    public void checkCleanNotifications(User u) {
        Mockito.doReturn(u).when(permission).getSessionUser();
        Mockito.doReturn(u).when(permission).getSessionUserWithSyncFromDB();
        Assert.assertEquals("[]", this.notificationsController.getAllNotifications());
        Assert.assertEquals(0, this.notificationDAO.findByEmail(u.getEmail()).size());
    }

    public void postPinByAuthor(String[] labels) {
        // Impossible to test the addNotificationsFromPin call from this.pinController while mocking this.permission.
        // this.pinsController Test. Check if the right method is called with the right arguments, then we test the method called itself with the same arguments.
        NotificationsController notificationsControllerSpy = spy(this.notificationsController);
        PinsController pinsControllerSpy = new PinsController(jdbcTemplate, pinDAO, userDAO, commentDAO, likeDAO, labelDAO, savedPinDAO, elsClient, uploadsManager, permission, gamificationController, notificationsControllerSpy, proxyConfig);
        this.pinsController.deleteAllPins();
        pinsControllerSpy.addNewPin(new AddNewPinParams("title","url","im","description", labels, "emailNotifFrom,firstname,lastname"));
        verify(notificationsControllerSpy).addNotificationsFromPin(eq(this.pinDAO.findAll().iterator().next().getPinId()), eq("a ajouté une épingle avec un label que vous suivez"));
    }

    public Notification createPinNotificationAsAuthor(User from, Pin pinNotif) {
        // Simulation as if it was the author that was sending the notification.
        Mockito.doReturn(from).when(permission).getSessionUser();
        this.notificationsController.addNotificationsFromPin(pinNotif.getPinId(), "a ajouté une épingle avec un label que vous suivez");
        Assert.assertEquals(1, this.notificationDAO.findByEmail("emailNotif").size());
        return this.notificationDAO.findByEmail("emailNotif").get(0);
    }

    public void cleanAll() {
        this.pinsController.deleteAllPins();
        this.commentsController.deleteAllComments();
        this.notificationsController.deleteAllNotifications();
    }

    @Test
    public void addPinNotificationsFromComment() {
        User u = this.createUser("emailNotif", "firstname", "lastname");
        User from = this.createUser("emailNotifFrom", "firstname", "lastname");

        this.cleanAll();

        Mockito.doReturn(u).when(permission).getSessionUser();
        Mockito.doNothing().when(permission).ensureNewEntityAuthorMatchesSessionUser(anyString());
        Mockito.doReturn(u).when(permission).getSessionUserWithSyncFromDB();
        User.getEmailFromString("firstname,lastname,emailNotif");
        User.getEmailFromString("firstname,lastname,emailNotifFrom");

        this.checkCleanNotifications(u);
        Pin pin = new Pin("title", "", 0, null, "", "content", "firstname,lastname,emailNotif", new DateTime());
        this.pinDAO.save(pin);

        this.postCommentByAuthor(from, pin);

        Notification notif = this.createCommentNotificationAsAuthor(from, pin);

        this.assertNotification(notif, from, "emailNotif", "#/?id=" + pin.getPinId(), "a commenté sur une de vos épingles", "comment");
    }

    public void postCommentByAuthor(User from, Pin pin) {
        // Impossible to test the addNotificationsFrom call from this.commentController while mocking this.permission.
        // this.commentController Test. Check if the right method is called with the right arguments, then we test the method called itself with the same arguments.
        NotificationsController notificationsControllerSpy = spy(this.notificationsController);
        CommentsController commentsControllerSpy = new CommentsController(jdbcTemplate, commentDAO, pinDAO, userDAO, elsClient, permission, gamificationController, notificationsControllerSpy);
        commentsControllerSpy.addComment(new CommentParams(from.getUserString(), pin.getPinId(), "Comment message"));
        verify(notificationsControllerSpy).addNotificationsFromComment(eq(pin.getPinId()));
    }

    public Notification createCommentNotificationAsAuthor(User from, Pin pin) {
        // Simulation as if it was the author that was sending the notification.
        Mockito.doReturn(from).when(permission).getSessionUser();
        this.notificationsController.addNotificationsFromComment(pin.getPinId());
        Assert.assertEquals(1, this.notificationDAO.findByEmail("emailNotif").size()); // fail sometimes: 0
        return this.notificationDAO.findByEmail("emailNotif").get(0);
    }

    @Test
    public void addCommentNotificationsFromComment() {
        User u = this.createUser("emailNotif", "firstname", "lastname");
        User from = this.createUser("emailNotifFrom", "firstname", "lastname");
        User uAuthor = this.createUser("emailOther", "firstname", "lastname");

        this.cleanAll();

        Mockito.doNothing().when(permission).ensureNewEntityAuthorMatchesSessionUser(anyString());
        Mockito.doReturn(u).when(permission).getSessionUserWithSyncFromDB();
        User.getEmailFromString("firstname,lastname,emailNotif");
        User.getEmailFromString("firstname,lastname,emailNotifFrom");
        User.getEmailFromString("firstname,lastname,emailOther");

        this.checkCleanNotifications(u);
        Pin pin = new Pin("title", "", 0, null, "", "content", "firstname,lastname,emailOther", new DateTime());
        this.pinDAO.save(pin);
        this.commentDAO.save(new Comment(pin.getPinId(), "firstname,lastname,emailNotif", "text", new DateTime()));

        this.postCommentByAuthor(from, pin);

        Notification notifComment = this.createCommentNotificationAsAuthor(from, pin);

        this.assertNotification(notifComment, from, "emailNotif", "#/?id=" + pin.getPinId(), "a commenté sur une épingle où vous avez vous même laissé un commentaire", "comment");
    }

    @Test
    public void addLikeNotificationsFromComment() {
        User u = this.createUser("emailNotif", "firstname", "lastname");
        User from = this.createUser("emailNotifFrom", "firstname", "lastname");
        User uAuthor = this.createUser("emailOther", "firstname", "lastname");

        this.cleanAll();

        Mockito.doReturn(u).when(permission).getSessionUser();
        Mockito.doNothing().when(permission).ensureNewEntityAuthorMatchesSessionUser(anyString());
        Mockito.doReturn(u).when(permission).getSessionUserWithSyncFromDB();
        User.getEmailFromString("firstname,lastname,emailNotif");
        User.getEmailFromString("firstname,lastname,emailNotifFrom");
        User.getEmailFromString("firstname,lastname,emailOther");

        this.checkCleanNotifications(u);

        Pin pin = new Pin("title", "", 0, null, "", "content", "firstname,lastname,emailOther", new DateTime());
        this.pinDAO.save(pin);
        this.likeDAO.save(new Like(pin.getPinId()+u.getEmail(), pin.getPinId(), u.getEmail()));

        this.postCommentByAuthor(from, pin);

        Notification notifComment = this.createCommentNotificationAsAuthor(from, pin); // fail sometimes

        this.assertNotification(notifComment, from, "emailNotif", "#/?id=" + pin.getPinId(), "a commenté sur une épingle que vous aimez", "comment");
    }

    @Test
    public void addConnectionBadgeNotification() {
        User u = this.createUser("emailNotif", "firstname", "lastname");

        this.notificationsController.deleteAllNotifications();
        this.gamificationController.getStats(u);

        Assert.assertEquals(0, this.notificationDAO.findByEmail("emailNotif").size());
        Stats stats = this.statsDAO.findByEmail(u.getEmail());
        stats.setConnexionNumber(24);
        stats.setLastConnexion(new DateTime().minusDays(2).toString());
        this.statsDAO.save(stats);
        Mockito.doReturn(u).when(permission).getSessionUser();
        Mockito.doReturn(u).when(permission).getSessionUserWithSyncFromDB();
        this.gamificationController.trackConnection();
        Assert.assertEquals(1, this.notificationDAO.findByEmail("emailNotif").size());
        Notification notif = this.notificationDAO.findByEmail("emailNotif").get(0);

        this.assertNotification(notif, u, "emailNotif", "#/profil", "venez de gagner le badge \"Lecteur " + gamificationService.badgesMessageUser(gamificationService.getLevel((stats.getConnexionNumber()+1)*GamificationController.CONNEXIONS_WEIGHT)) + "\"", "badge");
    }

    public void addBadgeNotificationBefore(User u) {
        this.notificationsController.deleteAllNotifications();
        this.gamificationController.getStats(u);
        Assert.assertEquals(0, this.notificationDAO.findByEmail("emailNotif").size());
    }

    public void addBadgeNotificationAfter(User u, String message) {
        this.gamificationController.getStats(u);
        Assert.assertEquals(1, this.notificationDAO.findByEmail(u.getEmail()).size());
        Notification notif = this.notificationDAO.findByEmail(u.getEmail()).get(0);
        this.assertNotification(notif, u, u.getEmail(), "#/profil", "venez de gagner le badge " + message, "badge");
    }

    @Test
    public void addBadgeNotification() {
        User u =this.createUser("emailNotif", "firstname", "lastname");
        this.addBadgeNotificationBefore(u);
        for(int i=0; i<10; i++) {
            this.pinDAO.save(new Pin("", "", 0, "", "", "content", "firstname,lastname,emailNotif", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Chroniqueur " + gamificationService.badgesMessageUser(gamificationService.getLevel(10*GamificationController.PINS_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        for(int i=0; i<40; i++) {
            this.pinDAO.save(new Pin("", "", 0, "", "", "content", "firstname,lastname,emailNotif", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Chroniqueur " + gamificationService.badgesMessageUser(gamificationService.getLevel(50*GamificationController.PINS_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        this.pinDAO.save(new Pin("", "", 0, "", "", "content", "firstname,lastname,emailNotif2", new DateTime()));
        for(int i=0; i<66; i++) {
            this.likeDAO.save(new Like("id:" + i, this.pinDAO.findByAuthor("firstname,lastname,emailNotif2").get(0).getPinId(), "emailNotif"));
        }
        this.addBadgeNotificationAfter(u, "\"Fan " + gamificationService.badgesMessageUser(gamificationService.getLevel(66*GamificationController.LIKES_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        this.pinDAO.save(new Pin("", "", 0, "", "", "content", "firstname,lastname,emailNotif2", new DateTime()));
        for(int i=0; i<50; i++) {
            this.likeDAO.save(new Like("id:" + i + 66, this.pinDAO.findByAuthor("firstname,lastname,emailNotif").get(i).getPinId(), "emailNotif2"));
        }
        this.addBadgeNotificationAfter(u, "\"Orateur " + gamificationService.badgesMessageUser(gamificationService.getLevel(50*GamificationController.LIKES_NUMBER_GET_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        for(int i=0; i<25; i++) {
            this.commentDAO.save(new Comment(this.pinDAO.findByAuthor("firstname,lastname,emailNotif").get(i).getPinId(), "firstname,lastname,emailNotif2", "text", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Animateur " + gamificationService.badgesMessageUser(gamificationService.getLevel(25*GamificationController.COMMENTS_NUMBER_GET_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        for(int i=0; i<150; i++) {
            this.commentDAO.save(new Comment(this.pinDAO.findByAuthor("firstname,lastname,emailNotif").get(i%50).getPinId(), "firstname,lastname,emailNotif2", "text", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Animateur " + gamificationService.badgesMessageUser(gamificationService.getLevel(175*GamificationController.COMMENTS_NUMBER_GET_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        this.pinDAO.save(new Pin("", "", 0, "", "", "content", "firstname,lastname,emailNotif2", new DateTime()));
        for(int i=0; i<33; i++) {
            this.commentDAO.save(new Comment(this.pinDAO.findByAuthor("firstname,lastname,emailNotif2").get(0).getPinId(), "firstname,lastname,emailNotif", "text", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Commentateur " + gamificationService.badgesMessageUser(gamificationService.getLevel(33*GamificationController.COMMENTS_WEIGHT)) + "\"");

        this.addBadgeNotificationBefore(u);
        for(int i=0; i<50; i++) {
            this.likeDAO.save(new Like("id:" + i, this.pinDAO.findByAuthor("firstname,lastname,emailNotif").get(0).getPinId(), "emailNotif2"));
        }
        this.addBadgeNotificationAfter(u, "\"Epingle " + gamificationService.badgesMessageElement(gamificationService.getLevel(50*GamificationController.LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT)) + " appréciée\"");

        this.addBadgeNotificationBefore(u);
        for(int i=0; i<50; i++) {
            this.commentDAO.save(new Comment(this.pinDAO.findByAuthor("firstname,lastname,emailNotif").get(0).getPinId(), "firstname,lastname,emailNotif2", "text", new DateTime()));
        }
        this.addBadgeNotificationAfter(u, "\"Epingle " + gamificationService.badgesMessageElement(gamificationService.getLevel(50*GamificationController.COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT)) + " polémique\"");
    }

}