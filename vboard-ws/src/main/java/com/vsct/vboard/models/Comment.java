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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment implements Comparable<Comment>{
    @Id
    private final String id;
    @NotBlank
    private final String pin;
    @NotBlank
    private String author;
    @NotBlank
    private String text;
    @Column(name = "post_date_utc")
    private final String postDateUTC;

    // Default constructor for Hibernate
    public Comment() {
        this("", "", "", "", "");
    }

    public Comment(String id, String pin, String author, String text, String postDateUTC) {
        this.pin = pin;
        this.author = author;
        this.id = id;
        this.text = text;
        this.postDateUTC = postDateUTC;
    }

    public Comment(String pin, String author, String text, DateTime postDateUTC) {
        this(UUID.randomUUID().toString(), pin, author, text, postDateUTC.toString());
    }

    public String getId() {
        return this.id;
    }

    public String getPinId() {
        return this.pin;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getPostDateUTC() {
        return this.postDateUTC;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setAuthor(String author) { this.author = author; }


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

    @Override
    public int compareTo(Comment c) {
        return getPostDateUTC().compareTo(c.getPostDateUTC());
    }
}
