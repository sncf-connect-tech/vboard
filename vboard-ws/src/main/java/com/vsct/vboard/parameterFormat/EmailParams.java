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

public class EmailParams {
    // After some experimenting, annotating fields with @NotNull + annotating
    // parameters of this class with @Valid seems to be the only way Hibernate
    // validator can raise errors : neither @NotNull on constructors / method
    // parameters nor @Valid on constructors / class names would work.
    // And using @ValidateOnExecution(type=ExecutableType.ALL) does not make
    // any difference
    // Check PinsController for a usage example using @Valid
    @NotNull
    private final String title;
    @NotNull
    private final String firstMessage;

    @JsonCreator
    public EmailParams(@JsonProperty(value = "title", required = true) String title,
                       @JsonProperty(value = "firstMessage") String firstMessage) {
        this.title = title;
        this.firstMessage = firstMessage;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstMessage() {
        return firstMessage;
    }

}
