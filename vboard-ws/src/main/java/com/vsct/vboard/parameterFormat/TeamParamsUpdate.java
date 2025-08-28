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

package com.vsct.vboard.parameterFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class TeamParamsUpdate {
    // After some experimenting, annotating fields with @NotNull + annotating
    // parameters of this class with @Valid seems to be the only way Hibernate
    // validator can raise errors : neither @NotNull on constructors / method
    // parameters nor @Valid on constructors / class names would work.
    // And using @ValidateOnExecution(type=ExecutableType.ALL) does not make
    // any difference
    // Check PinsController for a usage example using @Valid
    @NotNull
    private final String name;

    private final String email;

    private final String avatar;

    private final String info;

    private final double latitude;

    private final double longitude;

    private final String localisation;

    private final String project;

    @JsonCreator
    public TeamParamsUpdate(@JsonProperty(value = "name", required = true) String name,
                            @JsonProperty(value = "email") String email,
                            @JsonProperty(value = "avatar") String avatar,
                            @JsonProperty(value = "info") String info,
                            @JsonProperty(value = "latitude") double latitude,
                            @JsonProperty(value = "longitude") double longitude,
                            @JsonProperty(value = "localisation") String localisation,
                            @JsonProperty(value = "project") String project) {
        this.name = name;
        this.email = email;
        this.avatar = avatar;
        this.info = info;
        this.latitude = latitude;
        this.longitude = longitude;
        this.localisation = localisation;
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getInfo() {
        return info;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLocalisation() {
        return localisation;
    }

    public String getProject() {
        return project;
    }
}
