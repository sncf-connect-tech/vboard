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
import org.joda.time.DateTime;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "saved_pins")
public class SavedPin {
// Link between a user and a pin that he wants to save (can seen as FavoritePins).
    @Id
    @NotNull
    private final String id; // NOPMD
    @NotNull
    private final String pinId;
    @NotNull
    private final String userEmail;
    @Column(name = "saved_date_utc")
    private final String savedDateUTC;

    // Default constructor for Hibernate
    public SavedPin() {
        this("0", "@");
    }

    public SavedPin(String pinId,
                    String userEmail) {
        this(pinId+userEmail, pinId, userEmail, new DateTime().toString());
    }

    public SavedPin(String pinId,
                    String userEmail,
                    DateTime savedDateUTC) {
        this(pinId+userEmail, pinId, userEmail, savedDateUTC.toString());
    }

    public SavedPin(String id,
                    String pinId,
                    String userEmail,
                    String savedDateUTC) {
        this.id = id;
        this.pinId = pinId;
        this.userEmail = userEmail;
        this.savedDateUTC = savedDateUTC;
    }

    public String getPinId() {
        return pinId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSavedDateUTC() {
        return savedDateUTC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SavedPin savedPin = (SavedPin) o;

        if (id != null ? !id.equals(savedPin.id) : savedPin.id != null) return false;
        if (pinId != null ? !pinId.equals(savedPin.pinId) : savedPin.pinId != null) return false;
        if (userEmail != null ? !userEmail.equals(savedPin.userEmail) : savedPin.userEmail != null) return false;
        return savedDateUTC != null ? savedDateUTC.equals(savedPin.savedDateUTC) : savedPin.savedDateUTC == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (pinId != null ? pinId.hashCode() : 0);
        result = 31 * result + (userEmail != null ? userEmail.hashCode() : 0);
        result = 31 * result + (savedDateUTC != null ? savedDateUTC.hashCode() : 0);
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