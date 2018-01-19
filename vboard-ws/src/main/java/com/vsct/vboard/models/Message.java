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
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @NotNull
    private final String id; // NOPMD
    @NotNull
    private final String type;
    @NotNull
    private final String content;
    private boolean active;
    @NotBlank
    @Column(name = "post_date_utc")
    private final String postDateUTC;

    // Default constructor for Hibernate
    public Message() { this("-1", "", "", false); }

    public Message(String id, String type, String content, boolean active) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.active = active;
        this.postDateUTC = new DateTime(DateTimeZone.UTC).toString();
    }

    public Message(String type, String content) {
        this(UUID.randomUUID().toString(), type, content, true);
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getPostDateUTC() {
        return postDateUTC;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (active != message.active) return false;
        if (id != null ? !id.equals(message.id) : message.id != null) return false;
        if (type != null ? !type.equals(message.type) : message.type != null) return false;
        if (content != null ? !content.equals(message.content) : message.content != null) return false;
        return postDateUTC != null ? postDateUTC.equals(message.postDateUTC) : message.postDateUTC == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (postDateUTC != null ? postDateUTC.hashCode() : 0);
        return result;
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
