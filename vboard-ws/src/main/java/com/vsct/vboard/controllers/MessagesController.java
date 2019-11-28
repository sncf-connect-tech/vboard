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

package com.vsct.vboard.controllers;

import com.vsct.vboard.DAO.MessageDAO;
import com.vsct.vboard.DAO.NewsletterDAO;
import com.vsct.vboard.DAO.PinDAO;
import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.models.*;
import com.vsct.vboard.parameterFormat.EmailParams;
import com.vsct.vboard.parameterFormat.MessageParams;
import com.vsct.vboard.services.EmailSenderService;
import com.vsct.vboard.services.UploadsManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping(value = "/messages")
@SuppressFBWarnings({"CBX_CUSTOM_BUILT_XML", "ITC_INHERITANCE_TYPE_CHECKING", "STT_TOSTRING_STORED_IN_FIELD"})
// NOT normal, should be cleaned-up
public class MessagesController {
    static private final String LOCALHOST_IP = "0:0:0:0:0:0:0:1"; // NOPMD
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JdbcTemplate jdbcTemplate;
    private final AuthenticationController permission;
    private final MessageDAO messageDAO;
    private final UserDAO userDAO;
    private final PinDAO pinDAO;
    private final UploadsManager imageManager;
    private final GamificationController gamificationController;
    private final NewsletterDAO newsletterDAO;
    private final EmailSenderService emailSenderService;

    @Value("${com.vsct.vboard.hostname}")
    private String hostName;

    private String leaderBoardTemplate;
    private String leaderBoardHeaderTemplate;
    private String leaderTemplate;
    private String pinTemplate;
    private String footerTemplate;
    private String globalTemplate;
    private String htmlContent;
    private List<Pin> newsletterPin;
    private String tagNL = "#newsletter";
    private String tagNLHTML = "%23ancienNL";

    @Autowired
    public MessagesController(JdbcTemplate jdbcTemplate, AuthenticationController permission,
                              MessageDAO messageDAO, UserDAO userDAO, PinDAO pinDAO,
                              UploadsManager imageManager, GamificationController gamificationController, NewsletterDAO newsletterDAO,
                              EmailSenderService emailSenderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permission = permission;
        this.messageDAO = messageDAO;
        this.userDAO = userDAO;
        this.pinDAO = pinDAO;
        this.imageManager = imageManager;
        this.gamificationController = gamificationController;
        this.newsletterDAO = newsletterDAO;
        this.emailSenderService = emailSenderService;
    }

    /**
     * Admin VBoard displayable message
     */

    public void deleteAllMessages() {
        this.jdbcTemplate.execute("TRUNCATE TABLE messages;");
    }

    // Return the current displayable message
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Message getCurrentMessage() {
        Message msg = this.messageDAO.findByActive(true); // Only one message is active at the time
        if (msg == null) {
            msg = new Message(); // Empty message
        }
        return msg;
    }

    // Add a new displayable message
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Message addMessage(@Valid @RequestBody MessageParams params) {
        final String content = params.getContent();
        final String type = params.getType();
        permission.ensureCurrentUserIsAdmin();
        final Message message = new Message(type, content);
        try {
            this.logger.debug("addMessage: author={} - type={} -content={}", permission.getSessionUser().getUserString(), type, content);
            final Message previousMessage = this.messageDAO.findByActive(true); // Find the previous active message (if one if found)
            if (previousMessage != null) {
                previousMessage.setActive(false); // Deactivate the previous active message
                this.messageDAO.save(previousMessage);
            }
            this.messageDAO.save(message);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return message;
    }

    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Message hideMessage() {
        permission.ensureCurrentUserIsAdmin();
        Message previousMessage = this.messageDAO.findByActive(true);
        try {
            if (previousMessage != null) {
                previousMessage.setActive(false);
                this.messageDAO.save(previousMessage);
            }
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return previousMessage;
    }

    /**
     * Emails
     */

    // Send an email with the pins that user are following (favorite labels), published in the last month
    // A CRON job is running on the server, proceeding in a monthly curl.
    @RequestMapping(value = "/sendEmails/notification", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void sendEmailsNotification(HttpServletRequest request) {
        if (LOCALHOST_IP.equals(request.getRemoteAddr())) { // Check that method is only reached locally
            this.sendPinEmailsToAll(/*Notification=*/ true);
        }
    }

    // Send an email of the newsletter
    @RequestMapping(value = "/sendEmails/nl", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void sendEmailsNL(@Valid @RequestBody EmailParams emailParams) {
        permission.ensureUserHasNewsletterRole();
        DateTime lastNLDate = new DateTime(this.getLastNL().getPostDateUTC());
        if (lastNLDate.plusMinutes(10).isBeforeNow()) { // Prevent the newsletter to be sent twice withing 10 minutes
            this.sendNLEmailsToAll(emailParams, /*test=*/ false);
            this.logger.debug("Newsletter email not sent");
        } else {
            this.logger.debug("Newsletter email test not sent: last NL sent to recently");
        }
    }

    // Send an test email of the newsletter
    @RequestMapping(value = "/sendEmails/nltest", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void sendEmailsNLTest(@Valid @RequestBody EmailParams emailParams) {
        permission.ensureUserHasNewsletterRole();
        this.sendNLEmailsToAll(emailParams, /*test=*/ true);
        this.logger.debug("Newsletter test sent");
    }

    // Return an object of the last Newsletter sent
    @RequestMapping(value = "/getLastNL", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Newsletter getLastNL() {
        List<Newsletter> newsletterList = StreamSupport.stream(this.newsletterDAO.findAll().spliterator(), false).collect(Collectors.toList());
        if (newsletterList.isEmpty()) { // avoid returning null, so set a fake newsletter send 30 days (empty one)
            Newsletter nl = new Newsletter(new DateTime().minusDays(30).toString());
            this.newsletterDAO.save(nl);
            return nl;
        }
        Collections.sort(newsletterList, (Newsletter nl1, Newsletter nl2) -> nl2.getPostDateUTC().compareTo(nl1.getPostDateUTC())); // sort by date to get the last one
        return newsletterList.get(0);
    }

    // Send an email with the leaderboard
    // A CRON job is running on the server, proceeding in a monthly curl.
    @RequestMapping(value = "/sendEmails/leaderBoard", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void sendEmailsLeaderBoard(HttpServletRequest request) {
        if (LOCALHOST_IP.equals(request.getRemoteAddr())) { // Check that the method is only reached locally
            this.sendLeaderBoardEmailsToAll();
        }
    }

    // Send an email with the most likes pins from last month
    // A CRON job is running on the server, proceeding in a monthly curl.
    @RequestMapping(value = "/sendEmails/global", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void sendEmailsGlobal(HttpServletRequest request) {
        if (LOCALHOST_IP.equals(request.getRemoteAddr())) { // Check that the method is only reached locally
            this.sendPinEmailsToAll(/*notification=*/ false);
        }
    }

    public void setTemplate() {
        StringWriter writer = new StringWriter();
        StringWriter writer2 = new StringWriter();
        StringWriter writer3 = new StringWriter();
        try {
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("pinEmailTemplate.html"), writer, "UTF-8");
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("footer.html"), writer2, "UTF-8");
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("base_email_template.html"), writer3, "UTF-8");
        } catch (IOException e) {
            throw new VBoardException("Error retrieving HTML email templates", e);
        }
        this.pinTemplate = writer.toString();
        this.footerTemplate = writer2.toString();
        this.globalTemplate = writer3.toString();
    }

    // Create and Send notifications or global emails
    public void sendPinEmailsToAll(boolean sendNotifs) {
        this.setTemplate();
        if (!sendNotifs) {
            this.htmlContent = this.prepareEmailContentForGlobalPins();
        }
        // Send pins of the month to user that have authorized it and that have favorite labels
        // (to avoid empty emails) if the email is a notification one
        StreamSupport.stream(this.userDAO.findAll().spliterator(), false).parallel().forEach(user -> {
            if (sendNotifs) {
                if (!user.isReceiveRecapEmails() || isBlank(user.getFavoriteLabels())) {
                    return;
                }
            } else if (!user.isReceivePopularPinsEmails()) {
                return;
            }
            try {
                String firstMessage;
                String secondMessage;
                String title;
                String content;
                List<Pin> pins;
                if (sendNotifs) {
                    content = this.prepareEmailContentForSubscription(user.getFavoriteLabels());
                    firstMessage = "voici les épingles du mois que vous suivez.";
                    secondMessage = "Chaque mois, VBoard vous montre les épingles postées ayant un label que vous suivez.";
                    pins = this.getPins(user.getFavoriteLabels());
                    title = "[VBoard] Vos épingles du mois";
                } else {
                    content = this.htmlContent;
                    firstMessage = "voici les épingles populaires du mois.";
                    secondMessage = "Chaque mois, VBoard vous envoie un résumé des épingles les plus likées de VBoard.";
                    pins = this.getPins();
                    title = "[VBoard] Les épingles du mois";
                }
                if (!isBlank(content)) {
                    String globalTemplate = this.globalTemplate;
                    // Format the global template
                    globalTemplate = String.format(globalTemplate,
                            "http://" + hostName + "/#/", // Add Vboard Link
                            user.getFirstName() + ", " + firstMessage, // Add first message
                            "http://" + hostName + "/#/", // Add Vboard Link
                            "http://" + hostName + "/#/", // Add Vboard Link
                            secondMessage, // Add second message
                            content);
                    this.sendNotificationEmail(user, globalTemplate, pins, title); // Send the email
                }
            } catch (Exception e) {
                this.logger.error("Sending email failed to " + user.getFirstName() + " " + user.getLastName(), e.getCause());
            }
        });
    }

    // Create and Send the newsletter email to all user allowing it
    public void sendNLEmailsToAll(EmailParams emailParams, boolean test) {
        this.setTemplate(); // retrieve email templates
        User userNL = permission.getSessionUserWithSyncFromDB();
        if (userNL.getNewsletterLabel() != null && userNL.getNewsletterLabel().length() > 1) {
            this.tagNL = userNL.getNewsletterLabel();
            this.tagNLHTML = "%23" + userNL.getNewsletterLabel().substring(1);
        }
        this.htmlContent = this.prepareEmailContentForSubscription(this.tagNL); // Create the html content
        this.newsletterPin = this.getPins(this.tagNL); // Retrieve the pins in the newsletter
        if (test) {
            try {
                String firstMessage = emailParams.getTitle();
                String secondMessage = emailParams.getFirstMessage();
                String content = this.htmlContent;
                if (content != null) {
                    String globalTemplate = this.globalTemplate;
                    // Format the global template
                    globalTemplate = String.format(globalTemplate,
                            "http://" + hostName + "/#/?label=" + this.tagNLHTML, // Add Vboard Link
                            firstMessage, // Add first message
                            "http://" + hostName + "/#/?label=" + this.tagNLHTML, // Add Vboard Link
                            "http://" + hostName + "/#/?label=" + this.tagNL, // Add Vboard Link
                            secondMessage, // Add second message
                            content);
                    this.sendNotificationEmail(userNL, globalTemplate, this.newsletterPin, "[VBoard Test] " + emailParams.getTitle());
                }
            } catch (Exception e) {
                this.logger.error("Sending email failed to " + userNL.getFirstName() + " " + userNL.getLastName(), e.getCause());
            }
        } else {
            this.newsletterDAO.save(new Newsletter(userNL.getEmail(), this.newsletterPin.toString())); // Save the last newsletter sent
            StreamSupport.stream(this.userDAO.findAll().spliterator(), false).parallel().forEach(u -> {
                if (u.isReceiveNlEmails()) {
                    try {
                        String firstMessage = emailParams.getTitle();
                        String secondMessage = emailParams.getFirstMessage();
                        String content = this.htmlContent;
                        if (!isBlank(content)) {
                            String globalTemplate = this.globalTemplate;
                            // Format the global template
                            globalTemplate = String.format(globalTemplate,
                                    "http://" + hostName + "/#/?label=" + this.tagNLHTML, // Add Vboard Link
                                    firstMessage, // Add first message
                                    "http://" + hostName + "/#/?label=" + this.tagNLHTML, // Add Vboard Link
                                    "http://" + hostName + "/#/?label=" + this.tagNL, // Add Vboard Link
                                    secondMessage, // Add second message
                                    content);
                            this.sendNotificationEmail(u, globalTemplate, this.newsletterPin, "[VBoard] " + emailParams.getTitle());
                        }
                    } catch (Exception e) {
                        this.logger.error("Sending email failed to " + u.getFirstName() + " " + u.getLastName(), e.getCause());
                    }
                }
            });
        }
    }

    // Create and Send an email with the leaderboard of both users and teams
    public void sendLeaderBoardEmailsToAll() {

        /** Get all leaderboard templates **/
        StringWriter writer = new StringWriter();
        StringWriter writer2 = new StringWriter();
        StringWriter writer3 = new StringWriter();
        StringWriter writer4 = new StringWriter();
        try {
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("leaderBoardHeader.html"), writer, "UTF-8");
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("leader.html"), writer2, "UTF-8");
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("leaderBoard.html"), writer3, "UTF-8");
            IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("footer.html"), writer4, "UTF-8");
        } catch (IOException e) {
            throw new VBoardException("Error copying leaderboard HTML email templates", e);
        }
        this.leaderBoardHeaderTemplate = writer.toString();
        this.leaderTemplate = writer2.toString();
        this.leaderBoardTemplate = writer3.toString();
        this.footerTemplate = writer4.toString();

        // Create the leaderboards
        LeaderBoard leaderBoard = gamificationController.createLeaderBoard();
        LeaderBoard leaderBoardTeam = gamificationController.createLeaderBoardTeams();
        // Generate the html
        String leaderBoardHtml = this.generateLeaderBoardHTML(leaderBoard);
        String leaderBoardHtml2 = this.generateLeaderBoardHTML(leaderBoardTeam);

        StreamSupport.stream(this.userDAO.findAll().spliterator(), false).parallel().forEach(u -> {
            // send the emails only to user allowing it (and check if the leaderboard is not empty (should not occur)
            if (u.isReceiveLeaderboardEmails() && !leaderBoard.getAllLeaders().isEmpty()) {
                try {
                    this.sendLeaderBoardEmail(u, leaderBoardHtml + this.leaderBoardHeader(/* user= */ false) + leaderBoardHtml2, leaderBoard, leaderBoardTeam);
                } catch (Exception e) {
                    this.logger.error("Sending email failed to " + u.getFirstName() + " " + u.getLastName(), e.getCause());
                }
            }
        });
    }

    // Create the html content for notification emails
    public String prepareEmailContentForSubscription(String favoriteLabels) {
        if (isBlank(favoriteLabels)) {
            return "";
        }
        List<String> labels = Arrays.asList(favoriteLabels.split(",")); // Get its favorite labels (array)
        Stream<Pin> streamPin = StreamSupport.stream(this.pinDAO.findAll().spliterator(), false); // Get all labels
        // Filter labels posted less than a month ago, which contains labels and which labels matche the user's favorite ones
        List<Pin> pins = streamPin.filter(p -> p.getPostDate().isAfter(new DateTime(DateTimeZone.UTC).minusMonths(1).getMillis()) && !Collections.disjoint(p.getLabelsAsList(), labels)).collect(Collectors.toList());
        if (pins.isEmpty()) {
            return null; /* Do not send the email if no pins matches the favorite labels */
        }
        // Sort the pins according to posted date (recent to old)
        Collections.sort(pins, (Pin p1, Pin p2) -> p2.getPostDateUTC().compareTo(p1.getPostDateUTC()));
        // append the html pins
        return this.appendHtmlPin(pins);
    }

    // Create the html content for global emails
    public String prepareEmailContentForGlobalPins() {
        List<Pin> pins = getPins();
        if (pins.isEmpty()) {
            return null; /* Do not send the email if no pins  have been posted in the last month */
        }
        // append the html pins
        return this.appendHtmlPin(pins);
    }

    public String appendHtmlPin(List<Pin> pins) {
        final String content = pins.stream().map(this::toHtmlString).collect(Collectors.joining(""));
        return "<table width=\"1000\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" valign=\"top\" align=\"center\">" +
                "<tr>" +
                "<td width=\"610\">" +
                content +
                "</td>" +
                "</tr>" +
                "</table>";
    }

    // Get all the pins a user follow (using favorite labels) in the last month (pins used in the email)
    public List<Pin> getPins(String favoriteLabels) {
        if (isBlank(favoriteLabels)) {
            return Collections.emptyList();
        }
        final List<String> labels = Arrays.asList(favoriteLabels.split(","));
        final Stream<Pin> streamPin = StreamSupport.stream(this.pinDAO.findAll().spliterator(), false);
        final List<Pin> pins = streamPin
                .filter(p -> new DateTime(p.getPostDateUTC()).isAfter(new DateTime(DateTimeZone.UTC).minusMonths(1).getMillis()) && !Collections.disjoint(p.getLabelsAsList(), labels))
                .sorted((Pin p1, Pin p2) -> p2.getPostDateUTC().compareTo(p1.getPostDateUTC()))
                .collect(Collectors.toList());
        // Sort the pins according to posted date (recent to old)
        return pins;
    }

    // Get all the pins used for the global email
    public List<Pin> getPins() {
        // Filter labels posted less than a month ago, and sort them by likes
        Stream<Pin> streamPin = StreamSupport.stream(this.pinDAO.findAll().spliterator(), false);
        return streamPin
                .filter(p -> p.getPostDate().isAfter(new DateTime(DateTimeZone.UTC).minusMonths(1).getMillis()))
                .sorted((p1, p2) -> p2.getLikes() - p1.getLikes())
                .limit(10)
                .collect(Collectors.toList());
    }

    // Send notification and global emails
    public void sendNotificationEmail(User to, String content, List<Pin> pins, String title) throws Exception {
        // Javax mail used
        MimeMessage message = this.emailSenderService.setEmail(to);

        message.setSubject(title, "utf-8");
        String footer = this.footerTemplate;
        footer = String.format(footer, hostName);
        content = content + footer;

        message.setText(content, "utf-8", "html");

        // This mail has 2 part, the BODY and the embedded image
        MimeMultipart multipart = new MimeMultipart();

        // first part (the html)
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(content, "text/html; charset=UTF-8");
        multipart.addBodyPart(messageBodyPart);

        HashSet<Profil> users = new HashSet<>();

        // Images
        for (Pin pin : pins) {
            users.add(this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get()));
            if (!isBlank(pin.getImgType()) && !pin.getImgType().contains("<iframe") && !pin.getImgType().startsWith("https") && !pin.getImgType().endsWith("svg") && !pin.getImgType().endsWith("gif")) {
                try {
                    messageBodyPart = new MimeBodyPart();
                    String vblogUrl = "http://" + hostName + "/vblog/wp-content/uploads";
                    DataSource img;
                    if (pin.getImgType().contains(vblogUrl)) {
                        img = new FileDataSource(imageManager.getBlogImagesDirectory().resolve(pin.getImgType().substring(pin.getImgType().indexOf(vblogUrl) + vblogUrl.length())).toFile());
                    } else {
                        img = new FileDataSource(imageManager.getPinsImagesDirectory().resolve(pin.getPinId() + ".png").toFile());
                    }
                    messageBodyPart.setDataHandler(new DataHandler(img));
                    messageBodyPart.setHeader("Content-ID", "<" + pin.getPinId() + ">");

                    // add image to the multipart
                    multipart.addBodyPart(messageBodyPart);
                } catch (Exception e) {
                    this.logger.error("Image non trouvée sur le NAS:{}.png", pin.getPinId());
                }
            }
        }
        this.addLeadersImgs(multipart, users);
        messageBodyPart = new MimeBodyPart();
        DataSource img = new FileDataSource(imageManager.getAvatarImagesDirectory().resolve("default.png").toFile());
        messageBodyPart.setDataHandler(new DataHandler(img));
        messageBodyPart.setHeader("Content-ID", "<default>");
        multipart.addBodyPart(messageBodyPart);
        // put everything together
        message.setContent(multipart);
        // Send message
        Transport.send(message);
    }

    public void addLeadersImgs(MimeMultipart multipart, HashSet<Profil> leaders) throws Exception {
        for (Profil user : leaders) {
            if (user.hasCustomAvatar()) {
                final BodyPart messageBodyPart = new MimeBodyPart();
                final DataSource img = new FileDataSource(imageManager.getAvatarImagesDirectory().resolve(user.getId() + ".png").toFile());
                messageBodyPart.setDataHandler(new DataHandler(img));
                messageBodyPart.setHeader("Content-ID", "<" + user.getId() + ">");
                multipart.addBodyPart(messageBodyPart);
            }
        }
    }

    // Send leaderboard emails
    public void sendLeaderBoardEmail(User to, String content, LeaderBoard leaderBoard, LeaderBoard leaderBoardTeam) throws Exception {
        MimeMessage message = this.emailSenderService.setEmail(to);

        message.setSubject("[VBoard] LeaderBoard");

        content = this.leaderBoardHeader(true) + content;

        // Sets the body
        String footer = this.footerTemplate;
        footer = String.format(footer, hostName);
        content = content + footer;
        message.setText(content, "utf-8", "html");

        // This mail has 2 part, the BODY and the embedded image
        MimeMultipart multipart = new MimeMultipart();

        // first part (the html)
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(content, "text/html; charset=UTF-8");
        multipart.addBodyPart(messageBodyPart);
        // Images
        this.addLeadersImgs(multipart, leaderBoard.getAllLeaders());
        this.addLeadersImgs(multipart, leaderBoardTeam.getAllLeaders());

        messageBodyPart = new MimeBodyPart();
        DataSource img = new FileDataSource(imageManager.getAvatarImagesDirectory().resolve("default.png").toFile());
        messageBodyPart.setDataHandler(new DataHandler(img));
        messageBodyPart.setHeader("Content-ID", "<default>");

        // add image to the multipart
        multipart.addBodyPart(messageBodyPart);

        // put everything together
        message.setContent(multipart);

        // Send message
        Transport.send(message);
    }

    public String leaderBoardHeader(boolean user) {
        String html = this.leaderBoardHeaderTemplate;
        // Replace the "%s" in the template (which is in the ressources) with the corresponding elements
        if (user) { // Check if the leaderboard is the one with the user
            html = String.format(html, hostName, "LeaderBoard de VBoard",
                    "<br> <a href=\"http://" + hostName + "/#/leaderboard\" target=\"_blank\" style=\"text-decoration: none; font-size: 15px;\">http://" + hostName + "/#/leaderboard</a>" +
                            "<br> <p style=\"font-size: 15px;\">Chaque mois, VBoard vous envoie le classement des utilisateurs et équipes les plus actifs.<p>");
        } else { // Team leaderboard
            html = String.format(html, hostName, "LeaderBoard des équipes", "<br>");
        }
        return html;
    }

    // Return the html containing the pin image
    private String getHtmlImgType(Pin pin) {
        if (isBlank(pin.getImgType())) {
            return "";
        }
        final String img = "<img width=\"280\" src=\"" + pin.getImgType() + "\">";
        if (!pin.getImgType().contains("<iframe") && !pin.getImgType().startsWith("https") && !pin.getImgType().endsWith("svg") && !pin.getImgType().endsWith("gif")) {
            return "<td width=\"50%\"><a href=\"http://" + hostName + "/#/?id=" + pin.getPinId() + "\" target=\"_blank\" style=\"text-decoration: none;\">" +
                    img +
                    "</a></td>";
        }
        if (!pin.getImgType().contains("<iframe") && !pin.getImgType().endsWith("svg") && (pin.getImgType().startsWith("https") || pin.getImgType().endsWith("gif"))) {
            return "<td width=\"50%\"><a href=\"http://" + hostName + "/#/?id=" + pin.getPinId() + "\" target=\"_blank\" style=\"text-decoration: none;\">" +
                    img +
                    "</a></td>";
        }
        return "";
    }

    // Return the html containing the pin title
    private String getHtmlTitle(Pin pin) {
        if (isBlank(pin.getImgType())) {
            return "";
        }
        return "<tr><td style=\"font-weight: bold; border-bottom: 1px solid #e7e7e7; text-align: center; overflow: hidden; color: black; font-family: AvenirHeavy, AvenirBook, Arial,Helvetica, sans-serif; font-size: 14px;\">" +
                "<a href=\"http://" + hostName + "/#/?id=" + pin.getPinId() + "\" target=\"_blank\" style=\"text-decoration: none; color: #000000;\">" +
                pin.getPinTitle() +
                "</a></td></tr>";
    }

    // Return the html containing the pin labels
    private String getHtmlLabels(Pin pin) {
        return pin.getLabelsAsList().stream().map(label -> "<span><a style=\"text-decoration: none; color: #333;\" href=\"http://" + hostName + "/#/?label=" + escapeHtml4(label) + "\" target=\"_blank\">" + label + "&nbsp;</a></span>").collect(Collectors.joining(""));
    }

    // Return the html containing the pin's author avatar
    private String getHtmlAvatar(Pin pin) {
        if (this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get()).hasCustomAvatar()) {
            return "<img height=\"40\" src=\"cid:" + User.getEmailFromString(pin.getAuthor()).get() + "\" />";
        }
        return "<img height=\"40\" src=\"cid:default\" />";
    }

    // Return the body of the email
    public String toHtmlString(Pin pin) {
        return "<tr>" +
                "<td>" +
                "<br /><br />" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td>" +
                this.toHtmlContentStringPin(pin) +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td>" +
                "<br /><br />" +
                "</td>" +
                "</tr>";
    }

    public String toHtmlContentStringPin(Pin pin) {
        // Replace the "%s" in the template (which is in the ressources) with the corresponding elements
        final String userNiceName = pin.getAuthor().split(",")[0] + " " + pin.getAuthor().split(",")[1];
        return String.format(this.pinTemplate,
                this.getHtmlTitle(pin), // add title
                this.getHtmlImgType(pin), // add the image
                hostName, pin.getPinId(), // add info for url
                pin.getIndexableTextContent().replaceAll("(\r\n|\n)", "<br />"), // add content
                hostName, User.getEmailFromString(pin.getAuthor()).get(), // add info for author's url
                this.getHtmlAvatar(pin), // add author's avatar
                hostName, User.getEmailFromString(pin.getAuthor()).get(), userNiceName, // add info for author's url
                pin.getPostDate().getDayOfMonth() + "-" + pin.getPostDate().getMonthOfYear() + "-" + pin.getPostDate().getYear(), // add the posted date of the pin
                this.getHtmlLabels(pin) // add labels
        );
    }

    // Return the html of the leaderboard images
    public String generateUserImg(Profil user) {
        String html = "";
        if (user != null) {
            if (user.hasCustomAvatar()) {
                html = html + "<img height=\"50\" src=\"cid:" + user.getId() + "\" />";

            } else {
                html = html + "<img height=\"50\" src=\"cid:default\" />";
            }
        }
        return html;
    }

    // Generate the html of the leaderboard
    public String generateLeaderBoardHTML(LeaderBoard leaderBoard) {

        String[] leaders = new String[10];

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                leaders[i] = "<tr style=\"background:#e7e7e7;\">";
            } else {
                leaders[i] = "<tr>";
            }
        }

        // For each category, each leader is set
        int i = 0;
        for (Profil leader : leaderBoard.getPinsPosted()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getPinsPosted() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;

        }
        i = 0;

        for (Profil leader : leaderBoard.getCommentsPosted()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getCommentsPosted() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getLikesPosted()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getLikesPosted() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getConnexions()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getConnexionNumber() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getCommentReceived()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getCommentReceived() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getCommentsReceivedForOnePin()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getCommentsReceivedForOnePin() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getLikesReceived()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getLikesReceived() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        i = 0;
        for (Profil leader : leaderBoard.getLikesReceivedForOnePin()) {
            String leaderhtml = this.leaderTemplate;
            leaderhtml = String.format(leaderhtml, this.generateUserImg(leader), this.getPoints(leader).getLikesReceivedForOnePin() + " points", leader.getNiceName());
            leaders[i] += "<td>" + leaderhtml + "</td>";
            i++;
        }
        for (int j = 0; j < 10; j++) {
            leaders[j] += "</tr>";
        }
        final String leaderContent = String.join("", leaders);
        // Replace the "%s" in the template (which is in the ressources) with the corresponding element
        return String.format(this.leaderBoardTemplate, leaderContent);
    }

    // Get the points for each profil to be displayed on the leaderboard
    public Stats getPoints(Profil leader) {
        Stats stats = new Stats();
        if (leader instanceof User) {
            stats = gamificationController.getUserPointsStats(leader.getId());
        } else {
            if (leader instanceof Team) {
                stats = gamificationController.getTeamPointsStats(leader.getId());
            }
        }
        return stats;
    }

}
