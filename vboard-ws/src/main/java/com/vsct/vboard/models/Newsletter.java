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
import org.joda.time.DateTimeZone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "newsletters")
public class Newsletter {

    @Id
    @NotNull
    private final String id; // NOPMD
    @NotNull
    private final String senderEmail;
    @NotNull
    @Column(columnDefinition = "TEXT") // To use sql TEXT rather than VARCHAR (limited to 255 characters)
    private final String contentPin;
    @NotBlank
    @Column(name = "post_date_utc")
    private final String postDateUTC;

    // Default constructor for Hibernate
    private Newsletter() { this("-1", "", ""); }

    public Newsletter(String id, String sender, String contentPin) {
        this.id = id;
        this.senderEmail = sender;
        this.contentPin = contentPin;
        this.postDateUTC = new DateTime(DateTimeZone.UTC).toString();
    }

    public Newsletter(String sender, String contentPin) {
        this(UUID.randomUUID().toString(), sender, contentPin);
    }

    public Newsletter(String date) {
        this.id = "0";
        this.senderEmail = "";
        this.contentPin = "";
        this.postDateUTC = date;
    }

    public String getContentPin() {
        return contentPin;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getPostDateUTC() {
        return postDateUTC;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Newsletter that = (Newsletter) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (senderEmail != null ? !senderEmail.equals(that.senderEmail) : that.senderEmail != null) return false;
        if (contentPin != null ? !contentPin.equals(that.contentPin) : that.contentPin != null) return false;
        return postDateUTC != null ? postDateUTC.equals(that.postDateUTC) : that.postDateUTC == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (senderEmail != null ? senderEmail.hashCode() : 0);
        result = 31 * result + (contentPin != null ? contentPin.hashCode() : 0);
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
