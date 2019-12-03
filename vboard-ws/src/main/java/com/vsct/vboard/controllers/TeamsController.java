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

import com.vsct.vboard.DAO.TeamDAO;
import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.exceptions.NotFoundException;
import com.vsct.vboard.models.Team;
import com.vsct.vboard.models.User;
import com.vsct.vboard.models.VBoardException;
import com.vsct.vboard.parameterFormat.TeamParamsUpdate;
import com.vsct.vboard.services.UploadsManager;
import com.vsct.vboard.utils.JavaUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping(value = "/teams")
public class TeamsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final UserDAO userDAO;
    private final TeamDAO teamDAO;
    private final UploadsManager uploadsManager;
    private final AuthenticationController permission;
    private final GamificationController gamificationController;

    @Autowired
    public TeamsController(JdbcTemplate jdbcTemplate, UserDAO userDAO, TeamDAO teamDAO, UploadsManager uploadsManager,
                           AuthenticationController permission, GamificationController gamificationController) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDAO = userDAO;
        this.uploadsManager = uploadsManager;
        this.permission = permission;
        this.teamDAO = teamDAO;
        this.gamificationController = gamificationController;
    }

    public void deleteAllTeams() {
        this.jdbcTemplate.execute("TRUNCATE TABLE teams;");
    }

    @RequestMapping(value = "/{name:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Team getTeam(@PathVariable("name") String name) {
        Team team = this.teamDAO.findByName(name);
        if (team == null) {
            throw new NotFoundException("Team not found: " + name);
        }
        return team;
        // {email:.+} allow SpringBoot not to remove the email extension, but the default object response does not suit angular
        // The string is well passed to angular and is understood as a json object.
    }

    @RequestMapping(value = "/avatar/{name:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public String getAvatar(@PathVariable("name") String name) {
        return uploadsManager.getAvatar(name);
    }


    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Team addNewTeam(@Valid @RequestBody String teamName) {
        teamName = JavaUtils.extractJSONObject(teamName, "name");
        Team team = this.teamDAO.findByName(teamName);
        if (team == null) {
            team = new Team(teamName);
        }
        // The member who created the team (or set the team if it already exists) is added to it as a member
        team.addMember(permission.getSessionUser().getUserString());
        try {
            this.logger.debug("addNewTeam: {}", teamName);
            this.teamDAO.save(team);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return team;
    }

    public void checkIfUserInTeam(Team team, String member) {
        final User currentUser = permission.getSessionUser();
        if (member != null && !team.getMembers().contains(currentUser.getUserString())
                && !permission.getSessionUser().getEmail().equals(member.split(";")[2])) {
            throw new VBoardException("Unauthorized Method - The user: " + currentUser.getEmail() + " is not part of the team: " + team.getName());
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    // Parsing the params in the JSON body requires using a dedicated @RequestBody annotated class instead of simple @RequestParam arguments
    public Team updateTeam(@Valid @RequestBody TeamParamsUpdate params) {
        final Team team = this.teamDAO.findByName(params.getName());
        this.checkIfUserInTeam(team, null);

        team.setEmail(params.getEmail());
        // unchanged means that the avatar has not been changed by the user and thus no need to change it
        if (!"unchanged".equals(params.getAvatar())) {
            team.setHasCustomAvatar(!"default".equals(params.getAvatar()));
            uploadsManager.saveAvatar(params.getAvatar(), params.getName());
        }
        team.setInfo(params.getInfo());
        team.setLatitude(params.getLatitude());
        team.setLongitude(params.getLongitude());
        team.setLocalisation(params.getLocalisation());
        team.setProject(params.getProject());

        try {
            this.teamDAO.save(team);
            this.logger.debug("Team updated: {}", params.getName());
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return team;
    }

    public void removeMember(String teamName, String member) {
        member = member.replace(',', ';');
        final Team team = this.teamDAO.findByName(teamName);
        this.checkIfUserInTeam(team, member); // Throw an error if the user is not part of the team (not authorized)
        team.removeMember(member);
        try {
            this.teamDAO.save(team);
            this.logger.debug("Removing member {} from team {}", member, teamName);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
    }

    public Team addMember(String teamName, String member) {
        member = member.replace(',', ';');
        final Team team = this.teamDAO.findByName(teamName);
        this.checkIfUserInTeam(team, member); // Throw an error if the user is not part of the team (not authorized)
        team.addMember(member);
        try {
            this.teamDAO.save(team);
            this.logger.debug("Adding member {} to team {}", member, teamName);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return team;
    }

    @RequestMapping(value = "/setMembers/{name}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public Team setMembers(@PathVariable("name") String teamName, @Valid @RequestBody String members) {
        members = JavaUtils.extractJSONObject(members, "members");
        Team team = this.teamDAO.findByName(teamName);
        this.checkIfUserInTeam(team, null);

        List<String> previousMembers = team.getMembers();
        List<String> actualMembers = Arrays.asList(members.split(","));
        for (String member : (Iterable<String>) CollectionUtils.disjunction(previousMembers, actualMembers)) {
            if (!member.isEmpty()) {
                if (previousMembers.contains(member)) { // If the member has been removed
                    team.removeMember(member); // Remove it from the team
                    User user = this.userDAO.findByEmail(member.split(";")[2]); // Get the member
                    ArrayList<String> teams = new ArrayList<>();
                    if (user != null && user.getTeam() != null && !user.getTeam().isEmpty()) {
                        teams = new ArrayList<>(Arrays.asList(user.getTeam().split(","))); // get the user teams
                    }
                    if (user != null && teams.contains(teamName)) { // if the user has this team set, it is removed
                        teams.remove(teamName);
                        user.setTeam(String.join(", ", teams));
                        try {
                            this.userDAO.save(user);
                            this.gamificationController.getStats(user);
                        } catch (UnexpectedRollbackException e) {
                            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
                        }
                    }
                } else { // If the member has been added
                    team.addMember(member); // Add it from the team
                    User user = this.userDAO.findByEmail(member.split(";")[2]);
                    ArrayList<String> teams = new ArrayList<>();
                    if (user != null && user.getTeam() != null && !user.getTeam().isEmpty()) {
                        teams = new ArrayList<>(Arrays.asList(user.getTeam().split(","))); // get the user teams
                    }
                    if (user != null && !teams.contains(teamName)) {
                        if (user.getTeam() != null && !user.getTeam().isEmpty()) {
                            user.setTeam(user.getTeam() + "," + teamName); // if the user has not yet this team as a team, it is added
                        } else {
                            user.setTeam(teamName);
                        }
                        try {
                            this.userDAO.save(user);
                        } catch (UnexpectedRollbackException e) {
                            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
                        }
                    }
                }
            }
        }
        try {
            this.teamDAO.save(team);
            this.logger.debug("Added members {} to team {}", members, team);
        } catch (UnexpectedRollbackException e) {
            throw new VBoardException(e.getMessage(), e.getMostSpecificCause());
        }
        return team;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Team> getTeams() {
        return this.teamDAO.findAll();
    }

}
