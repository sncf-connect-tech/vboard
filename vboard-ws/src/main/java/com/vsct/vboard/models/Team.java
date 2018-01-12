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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.elasticsearch.common.lang3.builder.EqualsBuilder;
import org.elasticsearch.common.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Entity
@Table(name = "teams")
@SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
public class Team implements Profil {

    @Id
    @NotNull
    private final String name;
    private String email;
    @Column(name = "avatar")
    private boolean hasCustomAvatar;
    @Column(columnDefinition = "TEXT") // To use sql TEXT rather than VARCHAR (limited to 255 characters)
    private String members;
    private String info;
    private double latitude;
    private double longitude;
    private String localisation;
    private String project;

    public Team() { this("", "", false, new ArrayList<>(), "", 0, 0, "", ""); }

    public Team(String name, String email, boolean hasCustomAvatar, List<String> members, String info, double latitude, double longitude, String localisation, String project) {
        this.name = name;
        this.email = email;
        this.hasCustomAvatar = hasCustomAvatar;
        this.members = String.join(",", members);
        this.info = info;
        this.latitude = latitude;
        this.longitude = longitude;
        this.localisation = localisation;
        this.project = project;
    }

    public Team(String name) {
        this(name, "", false, new ArrayList<>(), "", 0, 0, "", "");
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean hasCustomAvatar() {
        return hasCustomAvatar;
    }

    public void setHasCustomAvatar(boolean hasCustomAvatar) {
        this.hasCustomAvatar = hasCustomAvatar;
    }

    public List<String> getMembers() {
        return Arrays.asList(this.members.split(","));
    }

    public void setMembers(List<String> members) {
        this.members = String.join(",", members);
    }

    public void addMember(String member) {
        // Prevent any "," to be misplaced (and version compatibility)
        member = member.replace(',', ';');
        if (isBlank(this.members)) {
            this.members = member;
        } else {
            HashSet<String> uniqueMembers = new HashSet<>(Arrays.asList(this.members.split(",")));
            uniqueMembers.add(member);
            this.members = String.join(",", uniqueMembers);
        }
    }

    public void removeMember(String member) {
        // Prevent any "," to be misplaced (and version compatibility)
        member = member.replace(',', ';');
        if (!isBlank(this.members)) {
            HashSet<String> uniqueMembers = new HashSet<>(Arrays.asList(this.members.split(",")));
            uniqueMembers.remove(member);
            this.members = String.join(",", uniqueMembers);
        }
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getNiceName() {
        return this.getName();
    }

    public String getId() {
        return this.getName();
    }

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

}
