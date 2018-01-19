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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.utils.SerializationError;
import com.vsct.vboard.utils.StaticContextAccessor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Entity
@Table(name = "pins")
@SuppressFBWarnings({"ISB_TOSTRING_APPENDING", "STT_STRING_PARSING_A_FIELD"})
public class Pin {

    @Id
    @NotNull
    private final String pinId;
    private String pinTitle;
    private String hrefUrl;
    @NotBlank
    private String indexableTextContent;
    private String labels;
    @NotBlank
    @Column(name = "post_date_utc")
    private final String postDateUTC;
    private int likes;
    private int commentsNumber;
    @Column(columnDefinition = "TEXT") // To use sql TEXT rather than VARCHAR (limited to 255 characters)
    private String imgType;
    private String author;


    // Default constructor for Hibernate
    public Pin() {
        this("", "0", "", 0, "", "", "", "", "", 0);
    }

    public Pin(String pinId,
               String pinTitle,
               String hrefUrl,
               int likes,
               String imgType,
               String labels,
               String indexableTextContent,
               String author,
               DateTime postDateUTC) {
        // From joda docs: Output the date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) where 'ZZ' outputs the offset with a colon
        this(pinId, pinTitle, hrefUrl, likes, imgType, labels, indexableTextContent, author, postDateUTC.toString(), 0);
    }

    public Pin(String pinTitle,
               String hrefUrl,
               int likes,
               String imgType,
               String labels,
               String indexableTextContent,
               String author,
               DateTime postDateUTC) {
        // From joda docs: Output the date time in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ) where 'ZZ' outputs the offset with a colon
        this("vboard-" + UUID.randomUUID().toString(), pinTitle, hrefUrl, likes, imgType, labels, indexableTextContent, author, postDateUTC.toString(), 0);
    }

    public Pin(String pinId,
               String pinTitle,
               String hrefUrl,
               int likes,
               String imgType,
               String labels,
               String indexableTextContent,
               String author,
               String postDateUTC,
               int commentsNumber) {
        this.pinId = pinId;
        this.pinTitle = pinTitle;
        this.hrefUrl = hrefUrl;
        this.likes = likes;
        this.imgType = imgType;
        this.labels = labels;
        this.indexableTextContent = indexableTextContent;
        this.author = author;
        this.postDateUTC = postDateUTC;
        this.commentsNumber = commentsNumber;
    }

    public String getPinId() {
        return this.pinId;
    }

    public String getPinTitle() {
        return this.pinTitle;
    }

    public String getHrefUrl() {
        return this.hrefUrl;
    }

    public int getLikes() {
        return this.likes;
    }

    public String getImgType() {
        return this.imgType;
    }

    public String getLabels() {
        return this.labels;
    }

    @JsonIgnore
    public List<String> getLabelsAsList() {
        if (isBlank(labels)) {
            return Collections.emptyList();
        }
        return Arrays.asList(labels.split(","));
    }

    @JsonIgnore
    public List<String> getSortedLabels() {
        final List<String> labelsList = this.getLabelsAsList();
        labelsList.sort(String::compareTo);
        return labelsList;
    }

    public String getIndexableTextContent() {
        return this.indexableTextContent;
    }

    public String getPostDateUTC() {
        return this.postDateUTC;
    }

    public DateTime getPostDate() {
        return new DateTime(postDateUTC);
    }

    public String getAuthor() {
        return this.author;
    }

    public int getCommentsNumber() {
        return this.commentsNumber;
    }


    public void setPinTitle(String pinTitle) {
        this.pinTitle = pinTitle;
    }

    public void setHrefUrl(String hrefUrl) {
        this.hrefUrl = hrefUrl;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void addLike() {
        this.likes++;
    }

    public void removeLike() {
        this.likes--;
        if (this.likes < 0) {
            this.likes = 0;
        }
    }

    public void setImgType(String imgType) {
        this.imgType = imgType;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void increaseCommentsNumber() {
        this.commentsNumber++;
    }

    public void decreaseCommentsNumber() {
        this.commentsNumber--;
    }

    public void setIndexableTextContent(String indexableTextContent) {
        this.indexableTextContent = indexableTextContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pin pin = (Pin) o;

        if (likes != pin.likes) return false;
        if (commentsNumber != pin.commentsNumber) return false;
        if (pinId != null ? !pinId.equals(pin.pinId) : pin.pinId != null) return false;
        if (pinTitle != null ? !pinTitle.equals(pin.pinTitle) : pin.pinTitle != null) return false;
        if (hrefUrl != null ? !hrefUrl.equals(pin.hrefUrl) : pin.hrefUrl != null) return false;
        if (indexableTextContent != null ? !indexableTextContent.equals(pin.indexableTextContent) : pin.indexableTextContent != null)
            return false;
        if (labels != null ? !labels.equals(pin.labels) : pin.labels != null) return false;
        if (postDateUTC != null ? !postDateUTC.equals(pin.postDateUTC) : pin.postDateUTC != null) return false;
        if (imgType != null ? !imgType.equals(pin.imgType) : pin.imgType != null) return false;
        return author != null ? author.equals(pin.author) : pin.author == null;
    }

    @Override
    public int hashCode() {
        int result = pinId != null ? pinId.hashCode() : 0;
        result = 31 * result + (pinTitle != null ? pinTitle.hashCode() : 0);
        result = 31 * result + (hrefUrl != null ? hrefUrl.hashCode() : 0);
        result = 31 * result + (indexableTextContent != null ? indexableTextContent.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (postDateUTC != null ? postDateUTC.hashCode() : 0);
        result = 31 * result + likes;
        result = 31 * result + commentsNumber;
        result = 31 * result + (imgType != null ? imgType.hashCode() : 0);
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