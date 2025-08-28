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

public class UserParamsUpdate {
    // After some experimenting, annotating fields with @NotNull + annotating
    // parameters of this class with @Valid seems to be the only way Hibernate
    // validator can raise errors : neither @NotNull on constructors / method
    // parameters nor @Valid on constructors / class names would work.
    // And using @ValidateOnExecution(type=ExecutableType.ALL) does not make
    // any difference
    // Check PinsController for a usage example using @Valid
    @NotNull
    private final String email;
    @NotNull
    private final String avatar;
    @NotNull
    private final String team;
    @NotNull
    private final String info;
    @NotNull
    private final boolean receiveNlEmails;
    @NotNull
    private final boolean receivePopularPins;
    @NotNull
    private final boolean receiveLeaderboardEmails;
    @NotNull
    private final boolean receiveRecapEmails;

    @JsonCreator
    public UserParamsUpdate(@JsonProperty(value = "email", required = true) String email,
                            @JsonProperty(value = "avatar", required = true) String avatar,
                            @JsonProperty(value = "team", required = true) String team,
                            @JsonProperty(value = "receiveNlEmails", required = true) boolean receiveNlEmails,
                            @JsonProperty(value = "receivePopularPinsEmails", required = true) boolean receivePopularPins,
                            @JsonProperty(value = "receiveLeaderboardEmails", required = true) boolean receiveLeaderboardEmails,
                            @JsonProperty(value = "receiveRecapEmails", required = true) boolean receiveRecapEmails,
                            @JsonProperty(value = "info", required = true) String info) {
        this.email = email;
        this.avatar = avatar;
        this.team = team;
        this.info = info;
        this.receiveNlEmails = receiveNlEmails;
        this.receivePopularPins = receivePopularPins;
        this.receiveLeaderboardEmails = receiveLeaderboardEmails;
        this.receiveRecapEmails = receiveRecapEmails;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getTeam() {
        return team;
    }

    public String getInfo() { return info; }

    public boolean isReceiveNlEmails() {
        return receiveNlEmails;
    }

    public boolean isReceivePopularPins() {
        return receivePopularPins;
    }

    public boolean isReceiveLeaderboardEmails() {
        return receiveLeaderboardEmails;
    }

    public boolean isReceiveRecapEmails() {
        return receiveRecapEmails;
    }
}
