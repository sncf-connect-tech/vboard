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

package com.vsct.vboard.controllers;

import com.vsct.vboard.DAO.*;
import com.vsct.vboard.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@RestController
@RequestMapping(value = "/notifications")
public class NotificationsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final NotificationDAO notificationDAO;
    private final AuthenticationController permission;
    private final PinDAO pinDAO;
    private final LikeDAO likeDAO;
    private final UserDAO userDAO;
    private final CommentDAO commentDAO;

    @Autowired
    public NotificationsController(JdbcTemplate jdbcTemplate, NotificationDAO notificationDAO,
                                   AuthenticationController permission, PinDAO pinDAO,
                                   LikeDAO likeDAO, UserDAO userDAO, CommentDAO commentDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationDAO = notificationDAO;
        this.permission = permission;
        this.pinDAO = pinDAO;
        this.likeDAO = likeDAO;
        this.userDAO = userDAO;
        this.commentDAO = commentDAO;
    }

    // Used in test
    public void deleteAllNotifications() {
        this.jdbcTemplate.execute("TRUNCATE TABLE notifications;");
    }

    // Get all notifications for the current user
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Notification> getAllNotifications() {
        return this.notificationDAO.findByEmail(permission.getSessionUser().getEmail());
    }

    // Get unclicked notifications for the current user
    @RequestMapping(value = "/unclicked", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Notification> getUnClickedNotifications() {
        return this.notificationDAO.findByEmail(permission.getSessionUser().getEmail()).stream().filter(n -> !n.isClicked()).collect(Collectors.toList());
    }

    // Get seen notifications for the current user
    @RequestMapping(value = "/seen", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Notification> getSeenNotifications() {
        return this.notificationDAO.findByEmail(permission.getSessionUser().getEmail()).stream().filter(Notification::isSeen).collect(Collectors.toList());
    }

    // Get unseen notifications for the current user
    @RequestMapping(value = "/unseen", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Notification> getUnSeenNotifications() {
        return this.notificationDAO.findByEmail(permission.getSessionUser().getEmail()).stream().filter(n -> !n.isSeen()).collect(Collectors.toList());
    }

    // Set a notification as seen
    @RequestMapping(value = "/seen/{id}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void setSeenNotification(@PathVariable("id") String id) {
        Notification notif = this.notificationDAO.findById(id).orElse(null);
        permission.ensureEmailMatchesSessionUser(notif.getEmail());
        notif.setSeen(true);
        this.notificationDAO.save(notif);
    }

    // Set a notification as clicked
    @RequestMapping(value = "/clicked/{id}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void setClickedNotification(@PathVariable("id") String id) {
        Notification notif = this.notificationDAO.findById(id).orElse(null);
        permission.ensureEmailMatchesSessionUser(notif.getEmail());
        notif.setClicked(true);
        this.notificationDAO.save(notif);
    }

    public void addNotification(String email, String link, String message, String type, String from) {
        Notification notif = new Notification(email, link, message, type, from, false, false);
        try {
            this.logger.debug("Notification: target={} - link={} -message={} -type={} -from={}", email, link, message, type, from);
            this.notificationDAO.save(notif);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
    }

    // Notification created when a pin with a label matches a user favorite label
    public void addNotificationsFromPin(String pinId, String message) {
        List<String> labels = this.pinDAO.findByPinId(pinId).getLabelsAsList(); // List of labels the a pin
        if (!this.pinDAO.findByPinId(pinId).getLabels().isEmpty()) {
            // false is here to manage data sequentially
            Stream<User> streamUsers = StreamSupport.stream(this.userDAO.findAll().spliterator(), false); // Get all users
            // Check if there are commun labels between the favorite ones of the user and the pin
            List<User> users = streamUsers.filter(u -> u.getFavoriteLabels() != null && !Collections.disjoint(Arrays.asList(u.getFavoriteLabels().split(",")), labels)
                    // Prevent the author of the pin getting a notification from it's own action
                    && !u.getEmail().equals(permission.getSessionUser().getEmail())).collect(Collectors.toList());
            users.forEach(u -> {
                try {
                    this.addNotification(u.getEmail(), "#/?id=" + pinId, message, "pin", permission.getSessionUser().getUserString());
                } catch (VBoardException e) {
                    this.logger.error("VBoardException: {}", e.getMessage());
                }
            });
        }

    }
    // Notification created when a comment is posted (to author of the pin and other user that commented or liked it)
    public void addNotificationsFromComment(String pinId) {
        Pin pin = this.pinDAO.findByPinId(pinId);
        if (pin != null) {
            User pinAuthor = this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get());
            // Send a notification to the author of the pin
            if (!pinAuthor.getEmail().equals(permission.getSessionUser().getEmail())){
                this.addNotification(pinAuthor.getEmail(), "#/?id=" + pinId, "a commenté sur une de vos épingles", "comment", permission.getSessionUser().getUserString());
            }
            List<Comment> comments = this.commentDAO.findByPin(pinId);
            // Send notifications to all user that have commented on that pin except the author of the pin (which already got a notification)
            Set<User> users = comments.stream().map(c -> this.userDAO.findByEmail(User.getEmailFromString(c.getAuthor()).get())).collect(Collectors.toSet());
            users.remove(pinAuthor);
            // Prevent the author of the comment getting a notification from it's own action
            users.stream().filter(u -> !u.getEmail().equals(permission.getSessionUser().getEmail())).forEach(u -> {
                try {
                    this.addNotification(u.getEmail(), "#/?id=" + pinId, "a commenté sur une épingle où vous avez vous même laissé un commentaire", "comment", permission.getSessionUser().getUserString());
                } catch (VBoardException e) {
                    this.logger.error("VBoardException: {}", e.getMessage());
                }
            });
            List<Like> likes = this.likeDAO.findByPin(pinId);
            // Send notifications to all user that have liked the pin except the users that already got a notifications
            likes.stream().forEach(l -> users.add(this.userDAO.findByEmail(l.getAuthor())));
            users.remove(pinAuthor);
            comments.stream().forEach(c -> users.remove(this.userDAO.findByEmail(User.getEmailFromString(c.getAuthor()).get())));
            users.stream().filter(u -> !u.getEmail().equals(permission.getSessionUser().getEmail())).forEach(u -> {
                try {
                    this.addNotification(u.getEmail(), "#/?id=" + pinId, "a commenté sur une épingle que vous aimez", "comment", permission.getSessionUser().getUserString());
                } catch (VBoardException e) {
                    this.logger.error("VBoardException: {}", e.getMessage());
                }
            });
        }
    }

    // Send a notification when a user get a badge
    public void addNotificationsFromBadges(User user, String badge) {
        try {
            this.addNotification(user.getEmail(), "#/profil", "venez de gagner le badge " + badge, "badge", user.getUserString());
        } catch (VBoardException e) {
            this.logger.error("VBoardException: {}", e.getMessage());
        }

    }

    // Send a notification when a user has his role changed
    public void addNotificationsFromRole(User user, String role, String verb) {
        try {
            if (!user.getEmail().equals(permission.getSessionUser().getEmail())) {
                this.addNotification(user.getEmail(), "#/profil", "vient de vous " + verb + " le role " + role, "role", permission.getSessionUser().getUserString());
            }
        } catch (VBoardException e) {
            this.logger.error("VBoardException: {}", e.getMessage());
        }

    }

}
