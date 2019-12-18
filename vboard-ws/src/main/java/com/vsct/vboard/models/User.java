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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsct.vboard.utils.SerializationError;
import com.vsct.vboard.utils.StaticContextAccessor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.builder.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.countMatches;

@Entity
@Table(name = "users")
@SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
public class User implements Diffable<User>, Profil, Serializable {

    @Id
    @NotNull
    private String email;
    private String firstName;
    private String lastName;
    @Column(name = "avatar")
    private boolean hasCustomAvatar;
    private String team;
    private String info;
    @Column(columnDefinition = "TEXT") // To use sql TEXT rather than VARCHAR (limited to 255 characters)
    private String favoriteLabels;
    private String lastConnection;
    private boolean receiveNlEmails; // The user authorize or not to receive newsletter Emails
    private boolean receivePopularPinsEmails; // The user authorize or not to receive popular pins Emails
    private boolean receiveLeaderboardEmails; // The user authorize or not to receive leaderboard Emails
    private boolean receiveRecapEmails; // The user authorize or not to receive favorite labeled pins Emails
    @Column(name = "role")
    private String roles;
    private String newsletterLabel; // Only used for newsletter admin
    @JsonInclude()
    @Transient
    private boolean isAdmin;

    // For JPA to initialize entities
    private User() {
        this("", "", "", false, "", "");
    }

    public User(String email, String firstName, String lastName) {
        this(email, firstName, lastName, false, "", "");
    }

    public User(String email,
                String firstName,
                String lastName,
                boolean hasCustomAvatar,
                String team,
                String info) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.hasCustomAvatar = hasCustomAvatar;
        this.team = team;
        this.info = info;
        this.lastConnection = new DateTime(DateTimeZone.UTC).toString();
        this.receiveNlEmails = true;
        this.receivePopularPinsEmails = true;
        this.receiveLeaderboardEmails = true;
        this.receiveRecapEmails = true;
        this.roles = "";
        this.favoriteLabels = "";
        this.newsletterLabel = "#newsletter";
        this.isAdmin = false;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public boolean hasCustomAvatar() {
        return hasCustomAvatar;
    }

    public void setHasCustomAvatar(final boolean hasCustomAvatar) {
        this.hasCustomAvatar = hasCustomAvatar;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(final String team) {
        this.team = team;
    }

    public boolean isReceiveNlEmails() {
        return receiveNlEmails;
    }

    public void setReceiveNlEmails(boolean receiveNlEmails) {
        this.receiveNlEmails = receiveNlEmails;
    }

    public boolean isReceivePopularPinsEmails() {
        return receivePopularPinsEmails;
    }

    public void setReceivePopularPinsEmails(boolean receivePopularPinsEmails) {
        this.receivePopularPinsEmails = receivePopularPinsEmails;
    }

    public boolean isReceiveLeaderboardEmails() {
        return receiveLeaderboardEmails;
    }

    public void setReceiveLeaderboardEmails(boolean receiveLeaderboardEmails) {
        this.receiveLeaderboardEmails = receiveLeaderboardEmails;
    }

    public boolean isReceiveRecapEmails() {
        return receiveRecapEmails;
    }

    public void setReceiveRecapEmails(boolean receiveRecapEmails) {
        this.receiveRecapEmails = receiveRecapEmails;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(final String info) {
        this.info = info;
    }

    public String getFavoriteLabels() {
        return favoriteLabels;
    }

    public void setFavoriteLabels(final String favoriteLabels) {
        this.favoriteLabels = favoriteLabels;
    }

    public String getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(String lastConnection) {
        this.lastConnection = lastConnection;
    }

    @Override
    public String getNiceName() {
        return this.getFirstName() + " " + this.getLastName();
    }

    public String getRoles() {
        return roles;
    }

    private List<String> getRolesAsList() {
        return Arrays.asList(roles.split(","));
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        final List<String> roles = getRolesAsList();
        roles.add(role.toString());
        this.roles = String.join(",", roles);
    }

    public void removeRole(Role role) {
        final List<String> roles = getRolesAsList();
        roles.remove(role.toString());
        this.roles = String.join(",", roles);
    }

    public boolean hasRole(Role role) {
        return getRolesAsList().contains(role.toString());
    }

    @Override
    public String getId() {
        return this.getEmail();
    }

    public String getNewsletterLabel() {
        return newsletterLabel;
    }

    public void setNewsletterLabel(String newsletterLabel) {
        this.newsletterLabel = newsletterLabel;
    }

    public String getUserString() {
        return getFirstName() + "," + getLastName() + "," + getEmail();
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    // Get an optional email from a given string (with the format firstName,LastName,email)
    static public Optional<String> getEmailFromString(String user) {
        return Optional.ofNullable(countMatches(user, ",") == 2 ? user.split(",")[2] : null);
    }

    @Override
    // WARNING: remember to update .hashCode() & .diff(user) accordingly
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return new EqualsBuilder()
                .append(hasCustomAvatar, user.hasCustomAvatar)
                .append(receiveNlEmails, user.receiveNlEmails)
                .append(receivePopularPinsEmails, user.receivePopularPinsEmails)
                .append(receiveLeaderboardEmails, user.receiveLeaderboardEmails)
                .append(receiveRecapEmails, user.receiveRecapEmails)
                .append(isAdmin, user.isAdmin)
                .append(email, user.email)
                .append(firstName, user.firstName)
                .append(lastName, user.lastName)
                .append(team, user.team)
                .append(info, user.info)
                .append(favoriteLabels, user.favoriteLabels)
                .append(lastConnection, user.lastConnection)
                .append(roles, user.roles)
                .append(newsletterLabel, user.newsletterLabel)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(email)
                .append(firstName)
                .append(lastName)
                .append(hasCustomAvatar)
                .append(team)
                .append(info)
                .append(favoriteLabels)
                .append(lastConnection)
                .append(receiveNlEmails)
                .append(receivePopularPinsEmails)
                .append(receiveLeaderboardEmails)
                .append(receiveRecapEmails)
                .append(roles)
                .append(newsletterLabel)
                .append(isAdmin)
                .toHashCode();
    }

    public DiffResult diff(User user) {
     // No need for null check, as NullPointerException correct if obj is null
     return new DiffBuilder(this, user, ToStringStyle.SHORT_PREFIX_STYLE)
             .append("hasCustomAvatar", hasCustomAvatar, user.hasCustomAvatar)
             .append("receiveNlEmails", receiveNlEmails, user.receiveNlEmails)
             .append("receivePopularPinsEmails", receivePopularPinsEmails, user.receivePopularPinsEmails)
             .append("receiveLeaderboardEmails", receiveLeaderboardEmails, user.receiveLeaderboardEmails)
             .append("receiveRecapEmails", receiveRecapEmails, user.receiveRecapEmails)
             .append("isAdmin", isAdmin, user.isAdmin)
             .append("email", email, user.email)
             .append("firstName", firstName, user.firstName)
             .append("lastName", lastName, user.lastName)
             .append("team", team, user.team)
             .append("info", info, user.info)
             .append("favoriteLabels", favoriteLabels, user.favoriteLabels)
             .append("lastConnection", lastConnection, user.lastConnection)
             .append("roles", roles, user.roles)
             .append("newsletterLabel", newsletterLabel, user.newsletterLabel)
       .build();
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
