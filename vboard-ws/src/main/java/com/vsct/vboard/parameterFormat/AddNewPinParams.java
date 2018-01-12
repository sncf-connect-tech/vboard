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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddNewPinParams {
    // After some experimenting, annotating fields with @NotNull + annotating
    // parameters of this class with @Valid seems to be the only way Hibernate
    // validator can raise errors : neither @NotNull on constructors / method
    // parameters nor @Valid on constructors / class names would work.
    // And using @ValidateOnExecution(type=ExecutableType.ALL) does not make
    // any difference
    // Check PinsController for a usage example using @Valid
    private final String title;
    private final String url;
    private final String imgType;
    @NotNull
    private final String description;
    private final String[] labels;
    @NotNull
    private final String author;

    @JsonCreator
    public AddNewPinParams(@JsonProperty(value = "title", required = false) String title,
            @JsonProperty(value = "url", required = false) String url,
            @JsonProperty(value = "imgType", required = false) String imgType,
            @JsonProperty(value = "description", required = true) String description,
            @JsonProperty(value = "labels", required = false) String[] labels,
            @JsonProperty(value = "author", required = true) String author) {
        this.title = title;
        this.url = url;
        this.imgType = imgType;
        this.description = description;
        this.labels = labels.clone();
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getImgType() {
        return imgType;
    }

    public String[] getLabels() {
        return labels.clone();
    }

    public String getDescription() { return description; }

    public String getAuthor() { return author; }
}
