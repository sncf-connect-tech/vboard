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

import com.vsct.vboard.models.SavedPin;
import com.vsct.vboard.DAO.SavedPinDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/savedpins")
public class SavedPinController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final SavedPinDAO savedPinDAO;
    private final AuthenticationController permission;
    private final GamificationController gamification;

    @Autowired
    public SavedPinController (JdbcTemplate jdbcTemplate, SavedPinDAO savedPinDAO, AuthenticationController permission, GamificationController gamification) {
        this.jdbcTemplate = jdbcTemplate;
        this.savedPinDAO = savedPinDAO;
        this.permission = permission;
        this.gamification = gamification;
    }

    public void deleteAllSavedPins() {
        this.jdbcTemplate.execute("TRUNCATE TABLE saved_pins;");
    }

    // Return all the pins saved by the current user (MySQL search)
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public List<SavedPin> getSavedPin() {
        return this.savedPinDAO.findByUserEmail(permission.getSessionUser().getEmail());
    }

    // Save the current pin (current user)
    @RequestMapping(value = "/{pin_id}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public SavedPin postSavedPin(@PathVariable("pin_id") String pinId) {
        SavedPin savedPin = new SavedPin(pinId, permission.getSessionUser().getEmail());
        this.savedPinDAO.save(savedPin);
        this.logger.debug("savedPin {} created by {}", pinId, permission.getSessionUser().getUserString());
        this.gamification.updateStats(permission.getSessionUser());
        return savedPin;
    }

    // Delete the current pin (current user)
    @RequestMapping(value = "/{pin_id}", method = RequestMethod.DELETE)
    @ResponseBody
    @Valid
    public void deleteSavedPin(@PathVariable("pin_id") String pinId) {
        SavedPin savedPin = this.savedPinDAO.findById(pinId+permission.getSessionUser().getEmail()).orElse(null);
        this.savedPinDAO.delete(savedPin);
        this.gamification.updateStats(permission.getSessionUser());
        this.logger.debug("savedPin {} deleted by: {}", pinId, permission.getSessionUser().getUserString());
    }

}
