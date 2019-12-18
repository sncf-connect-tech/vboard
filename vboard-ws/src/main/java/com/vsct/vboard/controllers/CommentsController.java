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

import com.vsct.vboard.DAO.CommentDAO;
import com.vsct.vboard.DAO.PinDAO;
import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.models.Comment;
import com.vsct.vboard.models.Pin;
import com.vsct.vboard.models.User;
import com.vsct.vboard.models.VBoardException;
import com.vsct.vboard.parameterFormat.CommentParams;
import com.vsct.vboard.services.ElasticSearchClient;
import com.vsct.vboard.utils.JavaUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/comments")
public class CommentsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final CommentDAO commentDAO;
    private final PinDAO pinDAO;
    private final UserDAO userDAO;
    private final ElasticSearchClient elsClient;
    private final AuthenticationController permission;
    private final GamificationController gamification;
    private final NotificationsController notifications;

    @Autowired
    public CommentsController(JdbcTemplate jdbcTemplate, CommentDAO commentDAO, PinDAO pinDAO,
                              UserDAO userDAO, ElasticSearchClient elsClient, AuthenticationController permission,
                              GamificationController gamification, NotificationsController notifications) {
        this.jdbcTemplate = jdbcTemplate;
        this.commentDAO = commentDAO;
        this.pinDAO = pinDAO;
        this.userDAO = userDAO;
        this.elsClient = elsClient;
        this.permission = permission;
        this.gamification = gamification;
        this.notifications = notifications;
    }


    public void deleteAllComments() {
        this.jdbcTemplate.execute("TRUNCATE TABLE comments;");
    }



    @RequestMapping(value = "/{pinId}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Comment> getCommentsFromPin(@PathVariable("pinId") String pinId) {
        // List of pins -> string to avoid Mime problems between front and back end for List
        return this.commentDAO.findByPin(pinId);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Comment addComment(@Valid @RequestBody CommentParams params) {
        String author = params.getAuthor();
        String pinId = params.getPinId();
        String text = params.getText();
        DateTime postDateUTC = new DateTime(DateTimeZone.UTC);
        Comment comment = new Comment(pinId, author, text, postDateUTC);
        // Check if the author given is the same as the session user (the author param could be deleted and replaced with the session one)
        permission.ensureNewEntityAuthorMatchesSessionUser(author);
        try {
            this.logger.debug("addComment: author={} - pin={} -text={}", author, pinId, text);
            this.commentDAO.save(comment);
            Pin pin = this.pinDAO.findByPinId(pinId);
            if (pin != null) {
                pin.increaseCommentsNumber();
                this.pinDAO.save(pin);
            }
            // Increase the number of comments for the given pin in elasticsearch
            this.elsClient.addComment(comment.getPinId());
            // Update the stats (for the user, and for the author of the pin where the comment has been added)
            final User sessionUser = permission.getSessionUser();
            this.gamification.updateStats(sessionUser);
            if (pin != null && User.getEmailFromString(pin.getAuthor()).isPresent()) {
                User userAuthor = this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get());
                if (userAuthor != null && userAuthor != sessionUser) {
                    this.gamification.updateStats(userAuthor);
                }
            }
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        // Send comment notifications
        notifications.addNotificationsFromComment(pinId);
        return comment;
    }

    // Comments posted from VBlog (wordpress)
    @RequestMapping(value = "/vblog", method = RequestMethod.POST, consumes = {"application/x-www-form-urlencoded"})
    @ResponseBody
    @Valid
    public Comment addCommentFromVblog(@RequestParam("text") final String text,
                                       @RequestParam("pinId") String pinId,
                                       @RequestParam("author") String author,
                                       @RequestParam("ID") final String ID, HttpServletRequest request) {

        Comment comment = this.commentDAO.findById("vblog-" + ID);
        // Should restrict the host name from wordpress (vblog)
        /*if (!request.getRemoteHost().equals(hostName.getHostName())) {
            throw new VBoardException("Unknown web site - The hostname that is using this method is not authorized: hostname: " + request.getRemoteHost());
        }*/
        User user = this.userDAO.findByEmail(author);
        if (user != null){
            author = user.getUserString();
        }
        if (comment == null) {
            DateTime postDateUTC = new DateTime(DateTimeZone.UTC);
            comment = new Comment("vblog-" + ID, "vblog-" + pinId, author, text, postDateUTC.toString());
            Pin pin = this.pinDAO.findByPinId("vblog-" + pinId);
            if (pin != null) {
                pin.increaseCommentsNumber();
                this.pinDAO.save(pin);
                if (User.getEmailFromString(pin.getAuthor()).isPresent()) {
                    User userAuthor = this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get());
                    if (userAuthor != null && userAuthor != permission.getSessionUser()) {
                        // Update the pin's author stats
                        this.gamification.updateStats(userAuthor);
                    }
                }
            }
        } else {
            comment.setText(text);
            comment.setAuthor(author);
        }
        try {
            this.logger.debug("addComment: author={} - pin={} -text={}", author, "vblog-" + pinId, text);
            this.commentDAO.save(comment);
            // Increase the number of comments for the given pin in elasticsearch
            this.elsClient.addComment(comment.getPinId());
            // Send comment notifications
            notifications.addNotificationsFromComment(comment.getPinId());
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        if (user != null) {
            // Update the user's stats
            this.gamification.updateStats(user);
        }
        return comment;
    }

    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Comment updateComment(@Valid @RequestBody String text, @PathVariable("id") String id) {
        text = JavaUtils.extractJSONObject(text, "text");
        Comment comment = this.commentDAO.findById(id);
        // Check if the user can update this comment (or throw an exception)
        permission.ensureUserHasRightsToAlterComment(comment.getAuthor());
        comment.setText(text);
        this.logger.debug("updateComment: id={} - text={}", id, text);
        this.commentDAO.save(comment);

        return comment;
    }

    @RequestMapping(value = "", method = RequestMethod.DELETE)
    @ResponseBody
    @Valid
    public Comment removeComment(@RequestParam(value = "id") String id)  {
        Comment comment;
        try {
            comment = this.commentDAO.findById(id);
            if (comment != null) {
                // Check if the user can update this comment (or throw an exception)
                permission.ensureUserHasRightsToAlterPin(comment.getAuthor());
                this.commentDAO.delete(comment);
                String pinId = comment.getPinId();
                Pin pin = this.pinDAO.findByPinId(pinId);
                if (pin != null) {
                    pin.decreaseCommentsNumber();
                    this.pinDAO.save(pin);
                }
                // Decrease the number of comments for the given pin in elasticsearch
                this.elsClient.removeComment(pinId);

                this.logger.debug("deleteComment: id={}", id);
                // Update the stats
                this.gamification.updateStats(permission.getSessionUser());
            } else {
                throw new VBoardException("Comment does not exist or already deleted");
            }

        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return comment;
    }

}
