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

package com.vsct.vboard.models;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.utils.SerializationError;
import com.vsct.vboard.utils.StaticContextAccessor;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;


@Entity
@Table(name = "likes")
public class Like {

    @Id
    private final String id; // NOPMD
    @NotBlank
    private final String pin;
    @NotBlank
    private final String author;

    // Default constructor for Hibernate
    private Like() {
        this("", "", "");
    }

    public Like(String id, String pin, String author) {
        this.pin = pin;
        this.author = author;
        this.id = id;
    }

    public String getPin() {
        return this.pin;
    }

    public String getAuthor() {
        return this.author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Like like = (Like) o;

        if (id != null ? !id.equals(like.id) : like.id != null) return false;
        if (pin != null ? !pin.equals(like.pin) : like.pin != null) return false;
        return author != null ? author.equals(like.author) : like.author == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (pin != null ? pin.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
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