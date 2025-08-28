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

import com.vsct.vboard.DAO.KonamiDAO;
import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.models.Konami;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController
@RequestMapping(value = "/konami")
public class KonamiController {
    private final KonamiDAO konamiDAO;
    private final AuthenticationController permission;
    private final UserDAO userDAO;

    @Autowired
    public KonamiController(KonamiDAO konamiDAO, AuthenticationController permission, UserDAO userDAO) {
        this.konamiDAO = konamiDAO;
        this.permission = permission;
        this.userDAO = userDAO;
    }

    @RequestMapping(value = "/{email:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Konami getKonamiResult(@PathVariable("email") String email) {
        return this.konamiDAO.findByUser(this.userDAO.findByEmail(email).getUserString());
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public Iterable<Konami> getKonamiResults() {
        return this.konamiDAO.findAll();
    }

    @RequestMapping(value = "/new/{points}", method = RequestMethod.POST)
    @ResponseBody
    @Valid
    public void addKonami(@PathVariable("points") int points) {
        Konami konami = this.konamiDAO.findByUser(permission.getSessionUser().getUserString());
        if (konami != null) {
            if (konami.getPoints() < points) {
                konami.setPoints(points);
                konami.setDate(new DateTime().toString());
                this.konamiDAO.save(konami);
            } else {
                konami.setDate(new DateTime().toString());
                this.konamiDAO.save(konami);
            }
        } else {
            this.konamiDAO.save(new Konami(permission.getSessionUser().getUserString(), 0));
        }
    }



}
