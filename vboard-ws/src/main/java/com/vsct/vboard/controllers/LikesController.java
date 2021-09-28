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

import com.vsct.vboard.exceptions.NotFoundException;
import com.vsct.vboard.models.*;
import com.vsct.vboard.parameterFormat.LikeParams;
import com.vsct.vboard.services.ElasticSearchClient;
import com.vsct.vboard.DAO.LikeDAO;
import com.vsct.vboard.DAO.PinDAO;
import com.vsct.vboard.DAO.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping(value = "/likes")
public class LikesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final LikeDAO likeDAO;
    private final PinDAO pinDAO;
    private final UserDAO userDAO;
    private final ElasticSearchClient elsClient;
    private final AuthenticationController permission;
    private final GamificationController gamification;

    @Autowired
    public LikesController(JdbcTemplate jdbcTemplate, LikeDAO likeDAO, PinDAO pinDAO, UserDAO userDAO,
                           ElasticSearchClient elsClient, AuthenticationController permission, GamificationController gamification) {
        this.jdbcTemplate = jdbcTemplate;
        this.likeDAO = likeDAO;
        this.pinDAO = pinDAO;
        this.userDAO = userDAO;
        this.elsClient = elsClient;
        this.permission = permission;
        this.gamification = gamification;
    }


    public void deleteAllLikes() {
        this.jdbcTemplate.execute("TRUNCATE TABLE likes;");
    }

    // Get all likes emitted from a given user
    // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
    @RequestMapping(value = "/by_user/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Like> getLikesFromAuthor(@PathVariable("email") String email) {
        return this.likeDAO.findByAuthor(email);
    }

    // Get all likes on a given pin
    @RequestMapping(value = "/by_pin/{pinId}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<Like> getLikesFromPin(@PathVariable("pinId") String pinId) {
        return this.likeDAO.findByPin(pinId);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Like addNewLikeInLikesDB(@Valid @RequestBody LikeParams params) {
        final String authorEmail = params.getEmail();
        final String pinId = params.getPinId();
        // This kind of ID is to avoid duplicates
        final Like like = new Like(pinId+authorEmail, pinId, authorEmail);
        Like previousLike;
        // Check user identity
        permission.ensureEmailMatchesSessionUser(authorEmail);
        try {
            previousLike = this.likeDAO.findById(pinId + authorEmail).orElse(null);
            this.logger.debug("addNewLike: author={} - pin={}", authorEmail, pinId);
            // Save in DB (if the like already exist, it will just be updated by the exact same values (same id))
            this.likeDAO.save(like);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        if (previousLike == null) {
            // If the like was not there before, it is effectively added in the MySQL and ElasticSearch DB
            this.addLikeToPin(pinId);
            // Update the user's stats
            this.gamification.updateStats(permission.getSessionUser());
        }
        return like;
    }


    private void addLikeToPin(String pinId) {
        if (pinId.contains("vboard-") || pinId.contains("vblog-")) { // Check if the element starts with vboard or vblog and so if it is in the DB
            // (other kind of pins can be seen with Elasticsearch but be base on an other DB (facebook, tweeter pins, ..., if implemented)
            Pin pin = this.pinDAO.findByPinId(pinId);
            // Increase the number of likes on the pin
            pin.addLike();
            try {
                this.pinDAO.save(pin);
                // Add the like in elasticSearch
                this.elsClient.addLike(pinId);
            } catch (UnexpectedRollbackException e) {
                throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
            }
            if (User.getEmailFromString(pin.getAuthor()).isPresent()) {
                User userAuthor = this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get());
                if (userAuthor != null && userAuthor != permission.getSessionUser()) {
                    // If the author of the pin is known its stats are updated
                    this.gamification.updateStats(userAuthor);
                }
            }
        }
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    @ResponseBody
    @Valid
    public Like removeLike(@RequestParam(value = "pinId") String pinId, @RequestParam(value = "email") String email) {
        permission.ensureEmailMatchesSessionUser(email);
        this.logger.debug("deleteLike: id={} - email={}", pinId, email);
        Pin pin = this.pinDAO.findByPinId(pinId); // decrease the like count on the pin
        if (pin == null) {
            throw new NotFoundException("No pin found for ID: " + pinId);
        }
        Like like = this.likeDAO.findById(pinId + email).orElse(null);
        if (like == null) {
            throw new NotFoundException("No like found for user: " + email);
        }
        this.likeDAO.delete(like);
        pin.removeLike();
        this.pinDAO.save(pin);
        // Remove the like in elasticsearch
        this.elsClient.removeLike(pinId);
        // Update the stats for the current user
        final User sessionUser = permission.getSessionUser();
        this.gamification.updateStats(sessionUser);
        if (User.getEmailFromString(pin.getAuthor()).isPresent()) {
            User userAuthor = this.userDAO.findByEmail(User.getEmailFromString(pin.getAuthor()).get());
            if (userAuthor != null && userAuthor.getEmail().equals(sessionUser.getEmail())) {
                // Update the stats for the author of the pin
                this.gamification.updateStats(userAuthor);
            }
        }
        return like;
    }


}
