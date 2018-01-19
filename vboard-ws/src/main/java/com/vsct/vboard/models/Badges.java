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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "badges")
public class Badges{

    @Id
    @NotNull
    private final String email;
    private String team;
    private int pinsPostedLevel;
    private int likesReceivedLevel;
    private int likesReceivedForOnePinLevel;
    private int likesPostedLevel;
    private int commentReceivedLevel;
    private int commentsReceivedForOnePinLevel;
    private int commentsPostedLevel;
    private int savedPinsLevel;
    private int secretLevel;
    private int connexionNumberLevel;
    private int globalLevel;

    // Default constructor for Hibernate
    public Badges() { this("@", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }

    public Badges(String email, String team) { this(email, team, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }

    public Badges(String email, String team, int pinsPostedLevel, int likesReceivedLevel, int likesReceivedForOnePinLevel,
                  int likesPostedLevel, int commentReceivedLevel, int commentsReceivedForOnePinLevel, int commentsPostedLevel,
                  int savedPinsLevel, int secretLevel, int connexionNumberLevel, int globalLevel) {
        this.email = email;
        this.team = team;
        this.pinsPostedLevel = pinsPostedLevel;
        this.likesReceivedLevel = likesReceivedLevel;
        this.likesReceivedForOnePinLevel = likesReceivedForOnePinLevel;
        this.likesPostedLevel = likesPostedLevel;
        this.commentReceivedLevel = commentReceivedLevel;
        this.commentsReceivedForOnePinLevel = commentsReceivedForOnePinLevel;
        this.commentsPostedLevel = commentsPostedLevel;
        this.savedPinsLevel = savedPinsLevel;
        this.secretLevel = secretLevel;
        this.connexionNumberLevel = connexionNumberLevel;
        this.globalLevel = globalLevel;
    }


    public String getEmail() {
        return email;
    }

    public String getTeam() {
        return team;
    }

    public int getPinsPostedLevel() {
        return pinsPostedLevel;
    }

    public int getLikesReceivedLevel() {
        return likesReceivedLevel;
    }

    public int getLikesReceivedForOnePinLevel() {
        return likesReceivedForOnePinLevel;
    }

    public int getLikesPostedLevel() {
        return likesPostedLevel;
    }

    public int getCommentReceivedLevel() {
        return commentReceivedLevel;
    }

    public int getCommentsReceivedForOnePinLevel() {
        return commentsReceivedForOnePinLevel;
    }

    public int getCommentsPostedLevel() {
        return commentsPostedLevel;
    }

    public int getSavedPinsLevel() {
        return savedPinsLevel;
    }

    public int getSecretLevel() {
        return secretLevel;
    }

    public int getConnexionNumberLevel() {
        return connexionNumberLevel;
    }


    public int getGlobalLevel() {
        return globalLevel;
    }

    // Calculate the minimal level get on a badge (which is the global level)
    public void setGlobalLevel() { this.globalLevel =
            Math.min(pinsPostedLevel,
                Math.min(likesReceivedLevel,
                    Math.min(likesReceivedForOnePinLevel,
                            Math.min(likesPostedLevel,
                                    Math.min(commentReceivedLevel,
                                            Math.min(commentsReceivedForOnePinLevel,
                                                Math.min(commentsPostedLevel,
                                                        Math.min(savedPinsLevel, connexionNumberLevel))))))));
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setPinsPostedLevel(int pinsPostedLevel) {
        this.pinsPostedLevel = pinsPostedLevel;
    }

    public void setLikesReceivedLevel(int likesReceivedLevel) {
        this.likesReceivedLevel = likesReceivedLevel;
    }

    public void setLikesReceivedForOnePinLevel(int likesReceivedForOnePinLevel) {
        this.likesReceivedForOnePinLevel = likesReceivedForOnePinLevel;
    }

    public void setLikesPostedLevel(int likesPostedLevel) {
        this.likesPostedLevel = likesPostedLevel;
    }

    public void setCommentReceivedLevel(int commentReceivedLevel) {
        this.commentReceivedLevel = commentReceivedLevel;
    }

    public void setCommentsReceivedForOnePinLevel(int commentsReceivedForOnePinLevel) {
        this.commentsReceivedForOnePinLevel = commentsReceivedForOnePinLevel;
    }

    public void setCommentsPostedLevel(int commentsPostedLevel) {
        this.commentsPostedLevel = commentsPostedLevel;
    }

    public void setSavedPinsLevel(int savedPinsLevel) {
        this.savedPinsLevel = savedPinsLevel;
    }

    public void setSecretLevel(int secretLevel) {
        this.secretLevel = secretLevel;
    }

    public void setConnexionNumberLevel(int connexionNumberLevel) {
        this.connexionNumberLevel = connexionNumberLevel;
    }

    public void setGlobalLevel(int globalLevel) {
        this.globalLevel = globalLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Badges badges = (Badges) o;

        if (pinsPostedLevel != badges.pinsPostedLevel) return false;
        if (likesReceivedLevel != badges.likesReceivedLevel) return false;
        if (likesReceivedForOnePinLevel != badges.likesReceivedForOnePinLevel) return false;
        if (likesPostedLevel != badges.likesPostedLevel) return false;
        if (commentReceivedLevel != badges.commentReceivedLevel) return false;
        if (commentsReceivedForOnePinLevel != badges.commentsReceivedForOnePinLevel) return false;
        if (commentsPostedLevel != badges.commentsPostedLevel) return false;
        if (savedPinsLevel != badges.savedPinsLevel) return false;
        if (secretLevel != badges.secretLevel) return false;
        if (connexionNumberLevel != badges.connexionNumberLevel) return false;
        if (globalLevel != badges.globalLevel) return false;
        if (email != null ? !email.equals(badges.email) : badges.email != null) return false;
        return team != null ? team.equals(badges.team) : badges.team == null;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (team != null ? team.hashCode() : 0);
        result = 31 * result + pinsPostedLevel;
        result = 31 * result + likesReceivedLevel;
        result = 31 * result + likesReceivedForOnePinLevel;
        result = 31 * result + likesPostedLevel;
        result = 31 * result + commentReceivedLevel;
        result = 31 * result + commentsReceivedForOnePinLevel;
        result = 31 * result + commentsPostedLevel;
        result = 31 * result + savedPinsLevel;
        result = 31 * result + secretLevel;
        result = 31 * result + connexionNumberLevel;
        result = 31 * result + globalLevel;
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
