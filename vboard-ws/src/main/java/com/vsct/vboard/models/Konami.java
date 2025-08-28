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
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "konami")
public class Konami {
    @Id
    private final String user;
    private int points;
    @NotBlank
    private String date;
    @NotBlank
    private final String firstAccess;

    // Default constructor for Hibernate
    public Konami() {
        this("", 0, new DateTime().toString(), new DateTime().toString());
    }

    public Konami(String user, int points, String date, String firstAccess) {
        this.user = user;
        this.points = points;
        this.date = date;
        this.firstAccess = firstAccess;
    }

    public Konami(String email, int points) {
        this(email, points, new DateTime().toString(), new DateTime().toString());
    }

    public String getUser() {
        return user;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFirstAccess() {
        return firstAccess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Konami konami = (Konami) o;

        if (points != konami.points) return false;
        if (user != null ? !user.equals(konami.user) : konami.user != null) return false;
        if (date != null ? !date.equals(konami.date) : konami.date != null) return false;
        return firstAccess != null ? firstAccess.equals(konami.firstAccess) : konami.firstAccess == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + points;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (firstAccess != null ? firstAccess.hashCode() : 0);
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
