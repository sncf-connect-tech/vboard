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

package com.vsct.vboard.parameterFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class MessageParams {
    // After some experimenting, annotating fields with @NotNull + annotating
    // parameters of this class with @Valid seems to be the only way Hibernate
    // validator can raise errors : neither @NotNull on constructors / method
    // parameters nor @Valid on constructors / class names would work.
    // And using @ValidateOnExecution(type=ExecutableType.ALL) does not make
    // any difference
    // Check PinsController for a usage example using @Valid
    @NotNull
    private final String content;
    @NotNull
    private final String type;

    @JsonCreator
    public MessageParams(@JsonProperty(value = "content", required = true) String content,
                         @JsonProperty(value = "type", required = true) String type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }


}
