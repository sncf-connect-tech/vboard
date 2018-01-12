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
import java.util.Comparator;

@Entity
@Table(name = "Stats")
public class Stats  implements Comparable<Stats>{

    @Id
    @NotNull
    private final String email;
    private String team;
    private int pinsPosted;
    private int likesReceived;
    private int likesReceivedForOnePin;
    private int likesPosted;
    private int commentReceived;
    private int commentsReceivedForOnePin;
    private int commentsPosted;
    private int savedPins;
    private int secret;
    private int connexionNumber;
    @NotBlank
    private String lastConnexion;

    // Default constructor for Hibernate
    public Stats() { this("@", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, new DateTime(DateTimeZone.UTC).toString()); }

    public Stats(String email, String team) { this(email, team, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, new DateTime(DateTimeZone.UTC).toString()); }

    public Stats(Stats stats) {
        this(stats.email, stats.team, stats.pinsPosted, stats.likesReceived, stats.likesReceivedForOnePin,
                stats.likesPosted, stats.commentReceived, stats.commentsReceivedForOnePin, stats.commentsPosted,
                stats.getSavedPins(), stats.secret, stats.connexionNumber, new DateTime(DateTimeZone.UTC).toString());
    }

    public Stats(String email, String team, int pinsPosted, int likesReceived, int likesReceivedForOnePin,
                 int likesPosted, int commentReceived, int commentsReceivedForOnePin, int commentsPosted,
                 int savedPins, int secret, int connexionNumber, String lastConnexion) {
        this.email = email;
        this.team = team;
        this.pinsPosted = pinsPosted;
        this.likesReceived = likesReceived;
        this.likesReceivedForOnePin = likesReceivedForOnePin;
        this.likesPosted = likesPosted;
        this.commentReceived = commentReceived;
        this.commentsReceivedForOnePin = commentsReceivedForOnePin;
        this.commentsPosted = commentsPosted;
        this.savedPins = savedPins;
        this.secret = secret;
        this.connexionNumber = connexionNumber;
        this.lastConnexion = lastConnexion;
    }

    public String getEmail() {
        return email;
    }

    public String getTeam() {
        return team;
    }

    public int getPinsPosted() {
        return pinsPosted;
    }

    public int getLikesReceived() {
        return likesReceived;
    }

    public int getLikesReceivedForOnePin() {
        return likesReceivedForOnePin;
    }

    public int getLikesPosted() {
        return likesPosted;
    }

    public int getCommentReceived() {
        return commentReceived;
    }

    public int getCommentsReceivedForOnePin() {
        return commentsReceivedForOnePin;
    }

    public int getCommentsPosted() {
        return commentsPosted;
    }

    public int getSavedPins() {
        return savedPins;
    }

    public int getSecret() {
        return secret;
    }

    public int getConnexionNumber() {
        return connexionNumber;
    }

    public String getLastConnexion() {
        return lastConnexion;
    }

    public void setPinsPosted(int pinsPosted) {
        this.pinsPosted = pinsPosted;
    }

    public void setLikesReceived(int likesReceived) {
        this.likesReceived = likesReceived;
    }

    public void setLikesReceivedForOnePin(int likesReceivedForOnePin) {
        this.likesReceivedForOnePin = likesReceivedForOnePin;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setLikesPosted(int likesPosted) {
        this.likesPosted = likesPosted;
    }

    public void setCommentReceived(int commentReceived) {
        this.commentReceived = commentReceived;
    }

    public void setCommentsReceivedForOnePin(int commentsReceivedForOnePin) {
        this.commentsReceivedForOnePin = commentsReceivedForOnePin;
    }

    public void setCommentsPosted(int commentsPosted) {
        this.commentsPosted = commentsPosted;
    }

    public void setSavedPins(int savedPins) {
        this.savedPins = savedPins;
    }

    public void setSecret(int secret) {
        this.secret = secret;
    }

    public void incrementConnexionNumber() { this.connexionNumber++; }

    public void setConnexionNumber(int connexionNumber) { this.connexionNumber = connexionNumber; }

    public void setLastConnexion(String lastConnexion) { this.lastConnexion = lastConnexion; }

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

    // Compare the values to sort stats (and thus stats owner for leaderboards)
    @Override
    public int compareTo(Stats o) {
        return 0;
    }

    public static class Comparators {

        public static final Comparator<Stats> pinsPosted = (b1, b2) -> b2.pinsPosted - b1.pinsPosted;
        public static final Comparator<Stats> likesReceived = (b1, b2) -> b2.likesReceived - b1.likesReceived;
        public static final Comparator<Stats> likesReceivedForOnePin = (b1, b2) -> b2.likesReceivedForOnePin - b1.likesReceivedForOnePin;
        public static final Comparator<Stats> likesPosted = (b1, b2) -> b2.likesPosted - b1.likesPosted;
        public static final Comparator<Stats> commentReceived = (b1, b2) -> b2.commentReceived - b1.commentReceived;
        public static final Comparator<Stats> commentsReceivedForOnePin = (b1, b2) -> b2.commentsReceivedForOnePin - b1.commentsReceivedForOnePin;
        public static final Comparator<Stats> commentsPosted = (b1, b2) -> b2.commentsPosted - b1.commentsPosted;
        public static final Comparator<Stats> connexionNumber = (b1, b2) -> b2.connexionNumber - b1.connexionNumber;

    }

}
