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

import java.util.HashSet;
import java.util.List;


public class LeaderBoard {

    private List<Profil> pinsPosted;
    private List<Profil> likesReceived;
    private List<Profil> likesReceivedForOnePin;
    private List<Profil> likesPosted;
    private List<Profil> commentReceived;
    private List<Profil> commentsReceivedForOnePin;
    private List<Profil> commentsPosted;
    private List<Profil> connexions;
    private List<Profil> savedPins;
    private List<Profil> pinRead;


    private LeaderBoard() { this(null, null, null, null, null, null, null, null, null, null); }

    public static LeaderBoard emptyOne () {
        return new LeaderBoard();
    }

    public LeaderBoard(List<Profil> pinsPosted, List<Profil> likesReceived, List<Profil> likesReceivedForOnePin,
                       List<Profil> likesPosted, List<Profil> commentReceived, List<Profil> commentsReceivedForOnePin, List<Profil> commentsPosted,
                       List<Profil> connexions, List<Profil> savedPins, List<Profil> pinRead) {
        this.pinsPosted = pinsPosted;
        this.likesReceived = likesReceived;
        this.likesReceivedForOnePin = likesReceivedForOnePin;
        this.likesPosted = likesPosted;
        this.commentReceived = commentReceived;
        this.commentsReceivedForOnePin = commentsReceivedForOnePin;
        this.commentsPosted = commentsPosted;
        this.connexions = connexions;
        this.savedPins = savedPins;
        this.pinRead = pinRead;
    }

    public List<Profil> getPinsPosted() {
        return pinsPosted;
    }

    public List<Profil> getLikesReceived() {
        return likesReceived;
    }

    public List<Profil> getLikesReceivedForOnePin() {
        return likesReceivedForOnePin;
    }

    public List<Profil> getLikesPosted() {
        return likesPosted;
    }

    public List<Profil> getCommentReceived() {
        return commentReceived;
    }

    public List<Profil> getCommentsReceivedForOnePin() {
        return commentsReceivedForOnePin;
    }

    public List<Profil> getCommentsPosted() {
        return commentsPosted;
    }

    public List<Profil> getConnexions() {
        return connexions;
    }

    public List<Profil> getSavedPins() {
        return savedPins;
    }

    public List<Profil> getPinRead() {
        return pinRead;
    }

    public void setPinsPosted(List<Profil> pinsPosted) {
        this.pinsPosted = pinsPosted;
    }

    public void setLikesReceived(List<Profil> likesReceived) {
        this.likesReceived = likesReceived;
    }

    public void setLikesReceivedForOnePin(List<Profil> likesReceivedForOnePin) {
        this.likesReceivedForOnePin = likesReceivedForOnePin;
    }

    public void setLikesPosted(List<Profil> likesPosted) {
        this.likesPosted = likesPosted;
    }

    public void setCommentReceived(List<Profil> commentReceived) {
        this.commentReceived = commentReceived;
    }

    public void setCommentsReceivedForOnePin(List<Profil> commentsReceivedForOnePin) {
        this.commentsReceivedForOnePin = commentsReceivedForOnePin;
    }

    public void setCommentsPosted(List<Profil> commentsPosted) {
        this.commentsPosted = commentsPosted;
    }

    public void setConnexions(List<Profil> connexions) {
        this.connexions = connexions;
    }

    public void setSavedPins(List<Profil> savedPins) {
        this.savedPins = savedPins;
    }

    public void setPinRead(List<Profil> pinRead) {
        this.pinRead = pinRead;
    }

    public HashSet<Profil> getAllLeaders() {
        HashSet<Profil> leaders = new HashSet<>();
        for (Profil profil: this.getPinsPosted()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getLikesReceived()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getLikesReceivedForOnePin()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getLikesPosted()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getCommentReceived()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getCommentsReceivedForOnePin()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getCommentsPosted()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        for (Profil profil: this.getConnexions()) {
            if (profil != null) {
                leaders.add(profil);
            }
        }
        return leaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LeaderBoard that = (LeaderBoard) o;

        if (pinsPosted != null ? !pinsPosted.equals(that.pinsPosted) : that.pinsPosted != null) return false;
        if (likesReceived != null ? !likesReceived.equals(that.likesReceived) : that.likesReceived != null)
            return false;
        if (likesReceivedForOnePin != null ? !likesReceivedForOnePin.equals(that.likesReceivedForOnePin) : that.likesReceivedForOnePin != null)
            return false;
        if (likesPosted != null ? !likesPosted.equals(that.likesPosted) : that.likesPosted != null) return false;
        if (commentReceived != null ? !commentReceived.equals(that.commentReceived) : that.commentReceived != null)
            return false;
        if (commentsReceivedForOnePin != null ? !commentsReceivedForOnePin.equals(that.commentsReceivedForOnePin) : that.commentsReceivedForOnePin != null)
            return false;
        if (commentsPosted != null ? !commentsPosted.equals(that.commentsPosted) : that.commentsPosted != null)
            return false;
        if (connexions != null ? !connexions.equals(that.connexions) : that.connexions != null) return false;
        if (savedPins != null ? !savedPins.equals(that.savedPins) : that.savedPins != null) return false;
        return pinRead != null ? pinRead.equals(that.pinRead) : that.pinRead == null;
    }

    @Override
    public int hashCode() {
        int result = pinsPosted != null ? pinsPosted.hashCode() : 0;
        result = 31 * result + (likesReceived != null ? likesReceived.hashCode() : 0);
        result = 31 * result + (likesReceivedForOnePin != null ? likesReceivedForOnePin.hashCode() : 0);
        result = 31 * result + (likesPosted != null ? likesPosted.hashCode() : 0);
        result = 31 * result + (commentReceived != null ? commentReceived.hashCode() : 0);
        result = 31 * result + (commentsReceivedForOnePin != null ? commentsReceivedForOnePin.hashCode() : 0);
        result = 31 * result + (commentsPosted != null ? commentsPosted.hashCode() : 0);
        result = 31 * result + (connexions != null ? connexions.hashCode() : 0);
        result = 31 * result + (savedPins != null ? savedPins.hashCode() : 0);
        result = 31 * result + (pinRead != null ? pinRead.hashCode() : 0);
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
