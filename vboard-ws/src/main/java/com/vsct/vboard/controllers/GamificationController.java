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

import com.vsct.vboard.DAO.*;
import com.vsct.vboard.exceptions.NotFoundException;
import com.vsct.vboard.models.*;
import com.vsct.vboard.services.GamificationService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@RestController
@RequestMapping(value = "/gamification")
public class GamificationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final LikeDAO likeDAO;
    private final CommentDAO commentDAO;
    private final PinDAO pinDAO;
    private final BadgesDAO badgesDAO;
    private final StatsDAO statsDAO;
    private final UserDAO userDAO;
    private final TeamDAO teamDAO;
    private final SavedPinDAO savedPinDAO;
    private final GamificationService gamification;
    private final AuthenticationController permission;
    private final NotificationsController notifications;
    public final static double LIKES_WEIGHT = 1.5; // Likes count for 1.5
    public final static double COMMENTS_WEIGHT = 3; // Comments count for 3
    public final static double PINS_WEIGHT = 5; // Pins posted count for 5
    public final static double CONNEXIONS_WEIGHT = 2; // Connexions count for 2
    public final static double COMMENTS_NUMBER_GET_WEIGHT = 2; // Comments received count for 2
    public final static double COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT = 7; // Comments max received count for 7
    public final static double LIKES_NUMBER_GET_WEIGHT = 1.5; // Likes received count for 1.5
    public final static double LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT = 5; // Likes max received count for 5
    public final static double PIN_SAVED_WEIGHT = 20; // Pins saved count for 20

    @Autowired
    public GamificationController(GamificationService gamification, JdbcTemplate jdbcTemplate,
                                  LikeDAO likeDAO, CommentDAO commentDAO, PinDAO pinDAO, BadgesDAO badgesDAO,
                                  StatsDAO statsDAO, UserDAO userDAO, TeamDAO teamDAO, SavedPinDAO savedPinDAO,
                                  AuthenticationController permission, NotificationsController notifications) {
        this.gamification = gamification;
        this.jdbcTemplate = jdbcTemplate;
        this.likeDAO = likeDAO;
        this.commentDAO = commentDAO;
        this.pinDAO = pinDAO;
        this.badgesDAO = badgesDAO;
        this.statsDAO = statsDAO;
        this.userDAO = userDAO;
        this.permission = permission;
        this.notifications = notifications;
        this.teamDAO = teamDAO;
        this.savedPinDAO = savedPinDAO;
    }

    public void deleteAllStats() {
        this.jdbcTemplate.execute("TRUNCATE TABLE Stats;");
    }

    public void deleteAllBadges() {
        this.jdbcTemplate.execute("TRUNCATE TABLE badges;");
    }


    // Get all the stats for the current user
    @RequestMapping(value = "/getStats", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getUserStats() {
        User user = permission.getSessionUserWithSyncFromDB();
        return this.getStats(user);
    }

    // Get all the stats for a user
    @RequestMapping(value = "/getStats/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getProfilStats(@PathVariable("email") String email) {
        User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return this.getStats(user);
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.
    }

    // Get the percentage done between the current level and the next one (see class gamificationService for more details) for the current
    @RequestMapping(value = "/getStatsPercentage", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getUserStatsPercentage() {
        User user = permission.getSessionUserWithSyncFromDB();
        return this.getStatsPercentage(user);
    }

    // Get the percentage done between the current level and the next one (see class gamificationService for more details) for a given user
    @RequestMapping(value = "/getStatsPercentage/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getProfilStatsPercentage(@PathVariable("email") String email) {
        User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return this.getStatsPercentage(user);
    }

    // Get the percentage done between the current level and the next one (see class gamificationService for more details) for a given team
    @RequestMapping(value = "/getTeamStatsPercentage/{name:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getTeamProfilStatsPercentage(@PathVariable("name") String name) {
        Team team = this.teamDAO.findByName(name);
        return this.getStatsPercentage(team);
    }

    // Return the gamification points in each category for a given user
    @RequestMapping(value = "/getPoints/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getUserPoints(@PathVariable("email") String email) {
        return this.getUserPointsStats(email);
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.
    }

    // Return the gamification points for a given team
    @RequestMapping(value = "/getPoints/team/{name:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Stats getTeamPoints(@PathVariable("name") String name) {
        return this.getTeamPointsStats(name);
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.
    }

    // Return the badges for the current user
    @RequestMapping(value = "/getBadges", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Badges getUserBadges() {
        User user = permission.getSessionUserWithSyncFromDB();
        return this.getBadges(user);
    }

    // Return the badges for a given user
    @RequestMapping(value = "/getBadges/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Badges getProfilBadges(@PathVariable("email") String email) {
        User user = this.userDAO.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return this.getBadges(user);
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.

    }

    // Return the badges for a given team
    @RequestMapping(value = "/getBadges/team/{name:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Badges getTeamBadges(@PathVariable("name") String name) {
        Team team = this.teamDAO.findByName(name);
        return this.getBadges(team);
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.

    }

    // Return the least of the most active user for each gamification category
    @RequestMapping(value = "/getLeaders", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public LeaderBoard getLeaderBoard() {
        return this.createLeaderBoard();
    }

    // Return the least of the most active team for each gamification category
    @RequestMapping(value = "/getLeaders/teams", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public LeaderBoard getLeaderBoardTeams() {
        return this.createLeaderBoardTeams();
    }

    // Add a new connexion to the user's stats
    @RequestMapping(value = "/connexion", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public int trackConnection() {
        final User user = permission.getSessionUserWithSyncFromDB();
        Stats stats = this.statsDAO.findByEmail(user.getEmail());
        // If the user has no stats yet, a new one is created
        if (stats == null) {
            stats = new Stats(user.getEmail(), user.getTeam());
        }
        // Get the last stat connexion
        DateTime lastConnexion = new DateTime(DateTimeZone.UTC);
        String date = stats.getLastConnexion();
        if (date == null) { // Used for version compatibility (new stats should get a default one)
            stats.setLastConnexion(new DateTime(DateTimeZone.UTC).toString());
        } else {
            try {
                lastConnexion = new DateTime(date);
        } catch (IllegalArgumentException e) {
                logger.debug("Date parsing error", e);
            }
        }
        // Gamification connexion can only be increased once a day.
        if (lastConnexion.plusDays(1).isBeforeNow()) {
            stats = this.getStats(user); // Update all the stats before
            stats.setLastConnexion(new DateTime(DateTimeZone.UTC).toString());
            // If the connexion level increased enough for the user to get a badge, a notification is sent.
            if (gamification.getLevel(stats.getConnexionNumber() * CONNEXIONS_WEIGHT) < gamification.getLevel((stats.getConnexionNumber() + 1) * CONNEXIONS_WEIGHT) && gamification.levels().contains(gamification.getLevel((stats.getConnexionNumber() + 1) * CONNEXIONS_WEIGHT))) {
                notifications.addNotificationsFromBadges(user, "\"Lecteur " + gamification.badgesMessageUser(gamification.getLevel((stats.getConnexionNumber() + 1) * CONNEXIONS_WEIGHT)) + "\"");
            }
            stats.incrementConnexionNumber();
        }
        try {
            this.statsDAO.save(stats);
            this.logger.debug("Connexion: User: {}, connexion number: {}", user.getNiceName(), stats.getConnexionNumber());
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return stats.getConnexionNumber();
    }


    // Return the least of the most active team for each gamification category
    public LeaderBoard createLeaderBoardTeams() {
        Iterable<Team> teams = this.teamDAO.findAll();
        List<Stats> allTeamStats = new ArrayList<>();
        teams.forEach(t -> allTeamStats.add(this.getTeamStats(t)));

        return this.setLeaderBoard(allTeamStats, true);
    }

    // Get and create the set of badges based on the user's stats
    public Badges getBadges(User user) {
        Badges badges = this.badgesDAO.findByEmail(user.getEmail());
        if (badges == null) {
            badges = new Badges(user.getEmail(), user.getTeam());
        }
        Stats stats = this.statsDAO.findByEmail(user.getEmail());
        if (stats == null) {
            stats = new Stats(user.getEmail(), user.getTeam());
        }
        badges.setCommentsPostedLevel(gamification.getLevel(stats.getCommentsPosted() * COMMENTS_WEIGHT));
        badges.setLikesPostedLevel(gamification.getLevel(stats.getLikesPosted() * LIKES_WEIGHT));
        badges.setPinsPostedLevel(gamification.getLevel(stats.getPinsPosted() * PINS_WEIGHT));
        badges.setCommentReceivedLevel(gamification.getLevel(stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT));
        badges.setCommentsReceivedForOnePinLevel(gamification.getLevel(stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        badges.setLikesReceivedLevel(gamification.getLevel(stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT));
        badges.setLikesReceivedForOnePinLevel(gamification.getLevel(stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        badges.setConnexionNumberLevel(gamification.getLevel(stats.getConnexionNumber() * CONNEXIONS_WEIGHT));
        badges.setSavedPinsLevel(gamification.getLevel(stats.getSavedPins() * PIN_SAVED_WEIGHT));
        int previousLevel = badges.getGlobalLevel();
        badges.setGlobalLevel();

        // If the champion badge reached the level 3, 5, 7 or 10, a notification is sent
        if (previousLevel < gamification.getLevel(badges.getGlobalLevel()) && gamification.levels().contains(gamification.getLevel(badges.getGlobalLevel()))) {
            notifications.addNotificationsFromBadges(user, "\"Champion " + gamification.badgesMessageUser(gamification.getLevel(badges.getGlobalLevel())) + "\"");
        }
        try {
            this.badgesDAO.save(badges);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return badges;
    }

    // Combine all the stats given
    private void combineStats(Stats stats, List<Stats> allStats) {
        double teamPointsModulator = allStats.size() > 3 ? 1 + .5 * (allStats.size() - 3) : 1;
        for (Stats singleStats : allStats) {
            stats.setCommentsPosted(stats.getCommentsPosted() + singleStats.getCommentsPosted());
            stats.setLikesPosted(stats.getLikesPosted() + singleStats.getLikesPosted());
            stats.setPinsPosted(stats.getPinsPosted() + singleStats.getPinsPosted());
            stats.setCommentReceived(stats.getCommentReceived() + singleStats.getCommentReceived());
            stats.setCommentsReceivedForOnePin(Math.max(stats.getCommentsReceivedForOnePin(), singleStats.getCommentsReceivedForOnePin()));
            stats.setLikesReceived(stats.getLikesReceived() + singleStats.getLikesReceived());
            stats.setLikesReceivedForOnePin(Math.max(stats.getLikesReceivedForOnePin(), singleStats.getLikesReceivedForOnePin()));
            stats.setConnexionNumber(stats.getConnexionNumber() + singleStats.getConnexionNumber());
            stats.setSavedPins(stats.getSavedPins() + singleStats.getSavedPins());
            stats.setSecret(stats.getSecret() + (singleStats.getSecret() > 0 ? 1 : 0));
        }
        stats.setCommentsPosted(((int) (stats.getCommentsPosted() / teamPointsModulator)));
        stats.setLikesPosted(((int) (stats.getLikesPosted() / teamPointsModulator)));
        stats.setPinsPosted(((int) (stats.getPinsPosted() / teamPointsModulator)));
        stats.setCommentReceived(((int) (stats.getCommentReceived() / teamPointsModulator)));
        stats.setLikesReceived(((int) (stats.getLikesReceived() / teamPointsModulator)));
        stats.setConnexionNumber(((int) (stats.getConnexionNumber() / teamPointsModulator)));
        stats.setSavedPins(((int) (stats.getSavedPins() / teamPointsModulator)));
    }

    // Get and create the set of badges based on the team's stats
    public Badges getBadges(Team team) {
        final Badges badges = new Badges(team.getName(), team.getName());

        final Stats stats = new Stats(team.getName(), team.getName());

        final List<Stats> allStats = StreamSupport.stream(this.statsDAO.findAll().spliterator(), false).filter(s -> s.getTeam() != null && s.getTeam().contains(team.getName())).collect(Collectors.toList());

        this.combineStats(stats, allStats);

        badges.setCommentsPostedLevel(gamification.getLevel(stats.getCommentsPosted() * COMMENTS_WEIGHT));
        badges.setLikesPostedLevel(gamification.getLevel(stats.getLikesPosted() * LIKES_WEIGHT));
        badges.setPinsPostedLevel(gamification.getLevel(stats.getPinsPosted() * PINS_WEIGHT));
        badges.setCommentReceivedLevel(gamification.getLevel(stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT));
        badges.setCommentsReceivedForOnePinLevel(gamification.getLevel(stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        badges.setLikesReceivedLevel(gamification.getLevel(stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT));
        badges.setLikesReceivedForOnePinLevel(gamification.getLevel(stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        badges.setConnexionNumberLevel(gamification.getLevel(stats.getConnexionNumber() * CONNEXIONS_WEIGHT));
        badges.setSavedPinsLevel(gamification.getLevel(stats.getSavedPins() * PIN_SAVED_WEIGHT));
        int secret = stats.getSecret() / (allStats.size() > 0 ? allStats.size() : 1);
        if (secret == 1) {
            badges.setSecretLevel(stats.getSecret());
        } else {
            badges.setSecretLevel(0);
        }
        badges.setGlobalLevel();

        return badges;
    }

    // Asynchronous method to update the stats
    public void updateStats(User user) {
        new Thread(() -> this.getStats(user)).start();
    }

    // Return and calculate the stats of a given user
    public Stats getStats(User user) {
        Stats stats = this.statsDAO.findByEmail(user.getEmail());
        if (stats == null) {
            stats = new Stats(user.getEmail(), user.getTeam());
        }

        int comments = this.commentDAO.findByAuthor(user.getUserString()).size(); // Comments number posted by the user
        int likes = this.likeDAO.findByAuthor(user.getEmail()).size();// Likes number posted by the user
        List<Pin> pins = this.pinDAO.findByAuthor(user.getUserString()); // Pins posted by the user
        int pinsCount = pins.size(); // Pins number posted by the user
        int commentsCount = 0;
        int commentsMaxOnOnePinCount = 0;
        int receivedLikesCount = 0;
        int likesMaxOnOnePinCount = 0;

        for (Pin pin : pins) {
            int pinCommentCount = this.commentDAO.findByPin(pin.getPinId()).size(); // Comment received on the user's pin
            commentsCount = commentsCount + pinCommentCount;
            if (commentsMaxOnOnePinCount < pinCommentCount) {
                commentsMaxOnOnePinCount = pinCommentCount; // Comment max received on one of the user's pin
            }

            int numberlikes = this.likeDAO.findByPin(pin.getPinId()).size(); // Likes received on the user's pin
            receivedLikesCount = receivedLikesCount + numberlikes;
            if (likesMaxOnOnePinCount < numberlikes) {
                likesMaxOnOnePinCount = numberlikes; // Likes max received on one of the user's pin
            }
        }
        stats.setTeam(user.getTeam());

        int savedPins = this.savedPinDAO.findByUserEmail(user.getEmail()).size();

        // Notification sent when a certain badge level is reached
        if (gamification.getLevel(stats.getCommentsPosted() * COMMENTS_WEIGHT) < gamification.getLevel(comments * COMMENTS_WEIGHT) && gamification.levels().contains(gamification.getLevel(comments * COMMENTS_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Commentateur " + gamification.badgesMessageUser(gamification.getLevel(comments * COMMENTS_WEIGHT)) + "\"");
        }
        if (gamification.getLevel(stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT) < gamification.getLevel(commentsCount * COMMENTS_NUMBER_GET_WEIGHT) && gamification.levels().contains(gamification.getLevel(commentsCount * COMMENTS_NUMBER_GET_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Animateur " + gamification.badgesMessageUser(gamification.getLevel(commentsCount * COMMENTS_NUMBER_GET_WEIGHT)) + "\"");
        }
        if (gamification.getLevel(stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT) < gamification.getLevel(commentsMaxOnOnePinCount * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT) && gamification.levels().contains(gamification.getLevel(commentsMaxOnOnePinCount * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Epingle " + gamification.badgesMessageElement(gamification.getLevel(commentsMaxOnOnePinCount * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT)) + " polémique\"");
        }
        if (gamification.getLevel(stats.getLikesPosted() * LIKES_WEIGHT) < gamification.getLevel(likes * LIKES_WEIGHT) && gamification.levels().contains(gamification.getLevel(likes * LIKES_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Fan " + gamification.badgesMessageUser(gamification.getLevel(likes * LIKES_WEIGHT)) + "\"");
        }
        if (gamification.getLevel(stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT) < gamification.getLevel(receivedLikesCount * LIKES_NUMBER_GET_WEIGHT) && gamification.levels().contains(gamification.getLevel(receivedLikesCount * LIKES_NUMBER_GET_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Orateur " + gamification.badgesMessageUser(gamification.getLevel(receivedLikesCount * LIKES_NUMBER_GET_WEIGHT)) + "\"");
        }
        if (gamification.getLevel(stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT) < gamification.getLevel(likesMaxOnOnePinCount * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT) && gamification.levels().contains(gamification.getLevel(likesMaxOnOnePinCount * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Epingle " + gamification.badgesMessageElement(gamification.getLevel(likesMaxOnOnePinCount * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT)) + " appréciée\"");
        }
        if (gamification.getLevel(stats.getPinsPosted() * PINS_WEIGHT) < gamification.getLevel(pinsCount * PINS_WEIGHT) && gamification.levels().contains(gamification.getLevel(pinsCount * PINS_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Chroniqueur " + gamification.badgesMessageUser(gamification.getLevel(pinsCount * PINS_WEIGHT)) + "\"");
        }
        if (gamification.getLevel(stats.getSavedPins() * PIN_SAVED_WEIGHT) < gamification.getLevel(savedPins * PIN_SAVED_WEIGHT) && gamification.levels().contains(gamification.getLevel(savedPins * PIN_SAVED_WEIGHT))) {
            notifications.addNotificationsFromBadges(user, "\"Collectionneur " + gamification.badgesMessageUser(gamification.getLevel(savedPins * PIN_SAVED_WEIGHT)) + "\"");
        }
        // End of notifications

        // Set the stats object
        stats.setCommentsPosted(comments);
        stats.setCommentReceived(commentsCount);
        stats.setCommentsReceivedForOnePin(commentsMaxOnOnePinCount);
        stats.setLikesPosted(likes);
        stats.setPinsPosted(pinsCount);
        stats.setLikesReceived(receivedLikesCount);
        stats.setLikesReceivedForOnePin(likesMaxOnOnePinCount);
        stats.setSavedPins(savedPins);

        try {
            this.statsDAO.save(stats);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return stats;
    }

    // Get the stats of a team by combining all the stats of its members
    public Stats getTeamStats(Team team) {
        final List<Stats> allStats = StreamSupport.stream(this.statsDAO.findAll().spliterator(), false).filter(s -> s.getTeam() != null && s.getTeam().contains(team.getName())).collect(Collectors.toList());
        final Stats stats = new Stats(team.getName(), team.getName());
        this.combineStats(stats, allStats);
        return stats;
    }

    // Get the percentage done between the current level and the next one (see class gamificationService for more details) for a user
    public Stats getStatsPercentage(Profil profil) {
        Stats stats;
        if (profil instanceof Team) {
            stats = this.getTeamStats((Team) profil);
        } else {
            stats = this.statsDAO.findByEmail(profil.getEmail());
        }

        stats.setCommentsPosted(gamification.getPercentage(stats.getCommentsPosted() * COMMENTS_WEIGHT));
        stats.setLikesPosted(gamification.getPercentage(stats.getLikesPosted() * LIKES_WEIGHT));
        stats.setPinsPosted(gamification.getPercentage(stats.getPinsPosted() * PINS_WEIGHT));
        stats.setSavedPins(gamification.getPercentage(stats.getSavedPins() * PIN_SAVED_WEIGHT));
        stats.setCommentReceived(gamification.getPercentage(stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT));
        stats.setCommentsReceivedForOnePin(gamification.getPercentage(stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        stats.setLikesReceived(gamification.getPercentage(stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT));
        stats.setLikesReceivedForOnePin(gamification.getPercentage(stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT));
        stats.setConnexionNumber(gamification.getPercentage(stats.getConnexionNumber() * CONNEXIONS_WEIGHT));

        return stats;
    }

    // Return the gamification points in each category for a given profil
    public Stats getUserPointsStats(String email) {
        Stats stats = new Stats(this.statsDAO.findByEmail(email));
        stats.setCommentsPosted((int) (stats.getCommentsPosted() * COMMENTS_WEIGHT * 10));
        stats.setCommentReceived((int) (stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT * 10));
        stats.setCommentsReceivedForOnePin((int) (stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10));
        stats.setLikesPosted((int) (stats.getLikesPosted() * LIKES_WEIGHT * 10));
        stats.setPinsPosted((int) (stats.getPinsPosted() * PINS_WEIGHT * 10));
        stats.setLikesReceived((int) (stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT * 10));
        stats.setLikesReceivedForOnePin((int) (stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10));
        stats.setConnexionNumber((int) (stats.getConnexionNumber() * CONNEXIONS_WEIGHT * 10));
        stats.setSavedPins((int) (stats.getSavedPins() * PIN_SAVED_WEIGHT * 10));
        return stats;
    }

    // Return the gamification points for a given team
    public Stats getTeamPointsStats(String name) {
        final Team team = this.teamDAO.findByName(name);
        if (team == null) {
            return new Stats();
        }
        final List<Stats> allStats = StreamSupport.stream(this.statsDAO.findAll().spliterator(), false).filter(s -> s.getTeam() != null && s.getTeam().contains(team.getName())).collect(Collectors.toList());
        final Stats stats = new Stats(team.getName(), team.getName());
        this.combineStats(stats, allStats);
        stats.setCommentsPosted((int) (stats.getCommentsPosted() * COMMENTS_WEIGHT * 10));
        stats.setCommentReceived((int) (stats.getCommentReceived() * COMMENTS_NUMBER_GET_WEIGHT * 10));
        stats.setCommentsReceivedForOnePin((int) (stats.getCommentsReceivedForOnePin() * COMMENTS_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10));
        stats.setLikesPosted((int) (stats.getLikesPosted() * LIKES_WEIGHT * 10));
        stats.setPinsPosted((int) (stats.getPinsPosted() * PINS_WEIGHT * 10));
        stats.setLikesReceived((int) (stats.getLikesReceived() * LIKES_NUMBER_GET_WEIGHT * 10));
        stats.setLikesReceivedForOnePin((int) (stats.getLikesReceivedForOnePin() * LIKES_NUMBER_GET_MAX_ON_ONE_PIN_WEIGHT * 10));
        stats.setConnexionNumber((int) (stats.getConnexionNumber() * CONNEXIONS_WEIGHT * 10));
        return stats;
    }

    // Return the first 10 most active users in each category
    public List<Profil> getFirsts(List<Stats> stats) {
        return stats.stream().map(Stats::getEmail).map(this.userDAO::findByEmail).limit(10).collect(Collectors.toList());
    }

    // Return the first 10 most active teams in each category
    public List<Profil> getFirstsTeams(List<Stats> stats) {
        return stats.stream().map(Stats::getTeam).map(this.teamDAO::findByName).limit(10).collect(Collectors.toList());
    }

    /**
     * Get First leaders in Pin posted category
     */
    private List<Profil> getPinPostedLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.pinsPosted);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Comment Posted category
     */
    private List<Profil> getCommentsPostedLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.commentsPosted);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Comment Received category
     */
    private List<Profil> getCommentsReceivedLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.commentReceived);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Comment Received on a single Pin category
     */
    private List<Profil> getCommentsReceivedUniqueLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.commentsReceivedForOnePin);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Likes Posted category
     */
    private List<Profil> getLikesPostedLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.likesPosted);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Likes Received category
     */
    private List<Profil> getLikesReceivedLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.likesReceived);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Likes Received on a single Pin category
     */
    private List<Profil> getLikesReceivedUniqueLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.likesReceivedForOnePin);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }

    /**
     * Get First leaders in Connexion Number category
     */
    private List<Profil> getConnexionsLeaders(List<Stats> stats, boolean team) {
        Collections.sort(stats, Stats.Comparators.connexionNumber);
        return team ? this.getFirstsTeams(stats) : this.getFirsts(stats);
    }


    public LeaderBoard setLeaderBoard(List<Stats> stats, boolean team) {
        final LeaderBoard leaderBoard = LeaderBoard.emptyOne();

        /** PinsPosted */
        leaderBoard.setPinsPosted(this.getPinPostedLeaders(stats, team));

        /** CommentsPosted */
        leaderBoard.setCommentsPosted(this.getCommentsPostedLeaders(stats, team));

        /** CommentsReceived */
        leaderBoard.setCommentReceived(this.getCommentsReceivedLeaders(stats, team));

        /** CommentsReceivedOnOnePin */
        leaderBoard.setCommentsReceivedForOnePin(this.getCommentsReceivedUniqueLeaders(stats, team));

        /** LikesPosted */
        leaderBoard.setLikesPosted(this.getLikesPostedLeaders(stats, team));

        /** LikesReceived */
        leaderBoard.setLikesReceived(this.getLikesReceivedLeaders(stats, team));

        /** LikesReceivedOnOnePin */
        leaderBoard.setLikesReceivedForOnePin(this.getLikesReceivedUniqueLeaders(stats, team));

        /** Connexion number */
        leaderBoard.setConnexions(this.getConnexionsLeaders(stats, team));


        return leaderBoard;
    }

    public LeaderBoard createLeaderBoard() {
        final Iterable<Stats> allStats = this.statsDAO.findAll();
        final List<Stats> stats = new ArrayList<>();
        allStats.forEach(stats::add);
        return this.setLeaderBoard(stats, false);
    }

    @RequestMapping(value = "/secret", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void setSecret() {
        User user = permission.getSessionUserWithSyncFromDB();
        Badges badges = this.badgesDAO.findByEmail(user.getEmail());
        Stats stats = this.statsDAO.findByEmail(user.getEmail());
        stats.setSecret(stats.getSecret() + 1);
        this.statsDAO.save(stats);
        if (badges.getSecretLevel() == 0) {
            badges.setSecretLevel(badges.getSecretLevel() + 1);
            this.badgesDAO.save(badges);
            notifications.addNotificationsFromBadges(user, "\"Curieux ! (Secret badge)\"");
        }
    }

}
