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

import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.exceptions.NotFoundException;
import com.vsct.vboard.models.Role;
import com.vsct.vboard.models.User;
import com.vsct.vboard.models.VBoardException;
import com.vsct.vboard.parameterFormat.UserParams;
import com.vsct.vboard.parameterFormat.UserParamsUpdate;
import com.vsct.vboard.services.UploadsManager;
import com.vsct.vboard.utils.JavaUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@RestController
@RequestMapping(value = "/users")
public class UsersController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final UserDAO userDAO;
    private final UploadsManager uploadsManager;
    private final AuthenticationController permission;
    private final TeamsController teamsController;
    private final NotificationsController notificationsController;

    @Autowired
    public UsersController(JdbcTemplate jdbcTemplate, UserDAO userDAO,
                           UploadsManager uploadsManager, AuthenticationController permission,
                           TeamsController teamsController, NotificationsController notificationsController) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDAO = userDAO;
        this.uploadsManager = uploadsManager;
        this.permission = permission;
        this.teamsController = teamsController;
        this.notificationsController = notificationsController;
    }

    public void deleteAllUsers() {
        this.jdbcTemplate.execute("TRUNCATE TABLE users;");
    }

    // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
    // The string is well passed to angular and is understood as a json object.
    @RequestMapping(value = "/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public String getUserFromEmail(@PathVariable("email") String email) {
        User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user.toString();
    }

    @RequestMapping(value = "/getAll", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<User> getAllUsers() {
        return this.userDAO.findAll();
    }

    // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
    // The string is well passed to angular and is understood as a json object.
    // The x-msdownload extension allows spring to send a lot of data (files), as here the data is an base64 encoded image
    @RequestMapping(value = "/avatar/{email:.+}", method = RequestMethod.GET, produces = "application/x-msdownload;charset=UTF-8")
    @ResponseBody
    @Valid
    public String getAvatar(@PathVariable("email") String email) {
            return uploadsManager.getAvatar(email);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public User addNewUser(@Valid @RequestBody UserParams params) {
        final String email = params.getEmail();
        final String firstName = params.getFirst_name();
        final String lastName = params.getLast_name();
        final User newUser = new User(email, firstName, lastName);
        try {
            this.logger.debug("addNewUser: email={} - first_name={} - last_name={}",
                    email, firstName, lastName);
            return this.userDAO.save(newUser);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public User updateUser(@Valid @RequestBody UserParamsUpdate params) {
        permission.ensureEmailMatchesSessionUser(params.getEmail());
        this.logger.debug("Updating user {}", params.getEmail());
        final String email = params.getEmail();
        final String team = params.getTeam();
        final User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        List<String> previousList = Arrays.asList(user.getTeam().split(","));
        List<String> newList = Arrays.asList(team.split(","));
        List<String> removedTeam =  new ArrayList<>();
        if (!user.getTeam().isEmpty()) {
            for (String t : previousList) {
                if (!newList.contains(t)) {
                    removedTeam.add(t);
                }
            }
        }
        if (!removedTeam.isEmpty()) {
            for (String t : removedTeam) {
                teamsController.removeMember(t, permission.getSessionUser().getUserString());
            }
        }
        user.setTeam(team);
        // unchanged means that the avatar has not been changed by the user and thus no need to change it
        if (!"unchanged".equals(params.getAvatar())) {
            user.setHasCustomAvatar(!"default".equals(params.getAvatar()));
            uploadsManager.saveAvatar(params.getAvatar(), email);
        }

        final String info = params.getInfo();
        user.setInfo(info);
        user.setReceiveNlEmails(params.isReceiveNlEmails());
        user.setReceiveLeaderboardEmails(params.isReceiveLeaderboardEmails());
        user.setReceivePopularPinsEmails(params.isReceivePopularPins());
        user.setReceiveRecapEmails(params.isReceiveRecapEmails());

        try {
            this.logger.debug("User updated: email={} - team={} - info={}",
                    email, team, info);
            this.userDAO.save(user);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return user;
    }

    @RequestMapping(value = "/teams", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Set<String> getTeams() {
        Set<String> teams = new HashSet<>();
        Iterable<User> users = this.userDAO.findAll();
        for(User user: users) {
            String userTeam = user.getTeam();
            if(StringUtils.isNotEmpty(userTeam)) {
                String[] userTeams = userTeam.split(",");
                // Pattern.compile(",").splitAsStream(userTeam).collect(Collectors.toCollection(HashSet::new)); same code as below
                Collections.addAll(teams, userTeams);
            }
        }
        return teams;
    }

    @RequestMapping(value = "/favoriteLabels", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public User updateFavoriteLabels(@Valid @RequestBody String labels) {
        User user = permission.getSessionUserWithSyncFromDB();
        labels = JavaUtils.extractJSONObject(labels, "labels");
        user.setFavoriteLabels(labels);
        try {
            this.userDAO.save(user);
            this.logger.debug("User {} updated its favorite labels: {}", user.getNiceName(), labels);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return user;
    }

    // When the user quit the app, the date of its last connection is set
    @RequestMapping(value = "/setLastConnection", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public User setLastConnection() {
        final User user = permission.getSessionUserWithSyncFromDB();
        user.setLastConnection(new DateTime(DateTimeZone.UTC).toString());
        this.userDAO.save(user);
        return user;
    }

    @RequestMapping(value = "/setRole/{role}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public User setRole(@Valid @RequestBody String email, @PathVariable("role") String role) {
        permission.ensureCurrentUserIsAdmin();
        email = JavaUtils.extractJSONObject(email, "email");
        User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        user.setRoles(role);
        this.userDAO.save(user);
        this.notificationsController.addNotificationsFromRole(user, role, "mettre");
        this.logger.debug("User {} got his role set to: {}", user.getNiceName(), role);
        return user;
    }

    @RequestMapping(value = "/addRole/{role}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public User addRole(@Valid @RequestBody String email, @PathVariable("role") String role) {
        permission.ensureCurrentUserIsAdmin();
        email = JavaUtils.extractJSONObject(email, "email");
        final User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (user.getRoles().equals(Role.Utilisateur.toString())){
            user.removeRole(Role.Utilisateur);
        }
        user.addRole(Role.valueOf(role));
        this.userDAO.save(user);
        this.notificationsController.addNotificationsFromRole(user, role, "ajouter");
        this.logger.debug("User {} got a new role: {}", user.getNiceName(), role);
        return user;
    }

    @RequestMapping(value = "/removeRole/{role}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public User removeRole(@Valid @RequestBody String email, @PathVariable("role") String role) {
        permission.ensureCurrentUserIsAdmin();
        email = JavaUtils.extractJSONObject(email, "email");
        final User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        user.removeRole(Role.valueOf(role));
        if (user.getRoles().equals(Role.valueOf(role).toString())){
            user.addRole(Role.Utilisateur);
        }
        this.userDAO.save(user);
        this.notificationsController.addNotificationsFromRole(user, role, "enlever");
        this.logger.debug("User {} got the following role removed: {}", user.getNiceName(), role);
        return user;
    }

    @RequestMapping(value = "/getRole/{role}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<User> getRole(@PathVariable("role") String role) {
        return StreamSupport.stream(this.userDAO.findAll().spliterator(), false).filter(u -> u.getRoles() != null && u.getRoles().contains(role)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/nlLabel", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void setNewsletterLabel(@RequestBody String labelObject) { // For newsletter moderator, add the possibility to change their NL default tag
        String label = JavaUtils.extractJSONObject(labelObject, "label");
        if (!label.startsWith("#")) {
            label = "#" + label;
        }
        User user = permission.getSessionUserWithSyncFromDB();
        user.setNewsletterLabel(label);
        this.userDAO.save(user);
    }


}
