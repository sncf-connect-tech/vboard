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

package com.vsct.vboard.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.utils.SerializationError;
import com.vsct.vboard.utils.StaticContextAccessor;
import org.elasticsearch.common.lang3.builder.EqualsBuilder;
import org.elasticsearch.common.lang3.builder.HashCodeBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @NotNull
    private final String id;
    @NotNull
    private final String email; // Email of the targeted notification
    private final String link;
    private String message;
    private String type;
    private final String fromUser; // FirstName, Name, Email of the author
    private boolean seen;
    private boolean clicked;
    @NotBlank
    private String date;

    // Default constructor for Hibernate
    private Notification() { this("@", "", "", "", "@", false, false); }

    public Notification(String email, String link, String message, String type, String fromUser, boolean seen, boolean clicked) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.link = link;
        this.message = message;
        this.type = type;
        this.fromUser = fromUser;
        this.seen = seen;
        this.clicked = clicked;
        this.date = new DateTime(DateTimeZone.UTC).toString();
    }

    public Notification(String email, String link, String message, String type, String fromUser) {
        this(email, link, message, type, fromUser, false, false);
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getLink() {
        return link;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFromUser() {
        return fromUser;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        try {
            return StaticContextAccessor.getBean(ObjectMapper.class).writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SerializationError(e);
        }
    }

}
