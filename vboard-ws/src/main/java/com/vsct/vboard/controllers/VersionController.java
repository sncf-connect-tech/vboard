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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by lucas_cimon on 11/01/2018.
 */
@RestController
@RequestMapping("/version")
public class VersionController {
    @Value("${application.name}")
    private String name;
    @Value("${application.version}")
    private String version;
    @Value("${application.buildDate}")
    private String buildDate;

    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Version getVersion() {
        return new Version(name, version, buildDate);
    }

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    static class Version {
        public final String name;
        public final String version;
        public final String buildDate;

        Version(String name, String version, String buildDate) {
            this.name = name;
            this.version = version;
            this.buildDate = buildDate;
        }
    }
}
