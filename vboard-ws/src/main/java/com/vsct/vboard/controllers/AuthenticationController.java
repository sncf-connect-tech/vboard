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

package com.vsct.vboard.controllers;

import com.vsct.vboard.DAO.UserDAO;
import com.vsct.vboard.config.AdministratorsConfig;
import com.vsct.vboard.config.WebSecurityConfig;
import com.vsct.vboard.config.cognito.JsonWebTokenAuthentication;
import com.vsct.vboard.models.Role;
import com.vsct.vboard.models.User;
import com.vsct.vboard.models.VBoardException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.IDToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;


@RestController
@RequestMapping(value = "/authentication")
public class AuthenticationController {

    private static final String SESSION_USER_ATTRIBUTE_NAME = "User";
    private static final User ANONYMOUS_USER = new User("@nonymous", "Anonymous", "User");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
    private final UserDAO userDAO;
    private final AdministratorsConfig administratorsConfig;
    private final WebSecurityConfig webSecurityConfig;
    private final HttpSession session;

    @Autowired
    public AuthenticationController(UserDAO userDAO, AdministratorsConfig administratorsConfig, WebSecurityConfig webSecurityConfig, HttpSession session) {
        this.userDAO = userDAO;
        this.administratorsConfig = administratorsConfig;
        this.webSecurityConfig = webSecurityConfig;
        this.session = session;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    @ResponseBody
    @Valid
    public User login() {
        return initializeUser();
    }

    private User initializeUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) {
            try {
                this.session.setAttribute(SESSION_USER_ATTRIBUTE_NAME, ANONYMOUS_USER);
            } catch (IllegalStateException e) {
                LOGGER.error("Could not set attribute User in current session", e);
            }
            return ANONYMOUS_USER;
        }
        String userEmail = getUserEmailFromAuth(auth);
        if (userEmail.isEmpty()) {
            throw new VBoardException("An non-empty email is required to perform authentication");
        }
        User user = this.userDAO.findByEmail(userEmail);
        if (user == null) {
            user = createUserFromAuth(auth);
        }
        // We always update this in case the config value has changed:
        user.setIsAdmin(this.administratorsConfig.getEmails().contains(userEmail));
        this.userDAO.save(user);
        if (LOGGER.isWarnEnabled()) {  // Lazy: do not execute code to build log message if not needed
            User sessionUser = (User) session.getAttribute(SESSION_USER_ATTRIBUTE_NAME);
            if (sessionUser != null && !user.equals(sessionUser)) {
                LOGGER.warn("Differing users between DB & session: {}", user.diff(sessionUser));
            }
        }
        this.session.setAttribute(SESSION_USER_ATTRIBUTE_NAME, user);
        return user;
    }

    private static String getUserEmailFromAuth(Authentication auth) {
        if (auth instanceof JsonWebTokenAuthentication) {
            return ((JsonWebTokenAuthentication)auth).getEmail();
        }
        final KeycloakPrincipal userDetails = (KeycloakPrincipal) auth.getPrincipal();
        final IDToken idToken = userDetails.getKeycloakSecurityContext().getToken();
        return idToken.getEmail();
    }

    @NotNull
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    private static User createUserFromAuth(Authentication auth) {
        if (auth instanceof JsonWebTokenAuthentication) {
            JsonWebTokenAuthentication jwtAuth = ((JsonWebTokenAuthentication)auth);
            String username = jwtAuth.getName();
            String[] parts = StringUtils.split(username, "\\");
            if (parts != null) {
                username = parts[1];
            }
            parts = StringUtils.split(username, "_");
            if (parts == null) {
                throw new IllegalArgumentException("The username in the JWT token provided does not contain a '_'");
            }
            String firstName = StringUtils.capitalize(parts[0]);
            String lastName = StringUtils.capitalize(parts[1]);
            LOGGER.info("createUserFromAuth/JWT: email={} firstName={} lastName={}", jwtAuth.getEmail(), firstName, lastName);
            return new User(jwtAuth.getEmail(), firstName, lastName);
        }
        final KeycloakPrincipal userDetails = (KeycloakPrincipal) auth.getPrincipal();
        final IDToken idToken = userDetails.getKeycloakSecurityContext().getToken();
        LOGGER.info("createUserFromAuth/Keycloak: email={} firstName={} lastName={}", idToken.getEmail(), idToken.getGivenName(), idToken.getFamilyName());
        return new User(idToken.getEmail(), idToken.getGivenName(), idToken.getFamilyName());
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public void logout() {
        session.removeAttribute(SESSION_USER_ATTRIBUTE_NAME);
    }

    @NotNull
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public User getSessionUser() {
        User user = (User) session.getAttribute(SESSION_USER_ATTRIBUTE_NAME);
        if (user != null) {
            LOGGER.info("getSessionUser: niceName={} isAdmin={}", user.getNiceName(), user.isAdmin());
            return user;
        }
        if (LOGGER.isDebugEnabled()) {  // Lazy: do not generate method argument if not needed
            LOGGER.debug("No user found in session, re-initializing one. Called from method: {}", Thread.currentThread().getStackTrace()[2].getMethodName());
        }
        return initializeUser();
    }

    void ensureCurrentUserIsAdmin() {
        if (webSecurityConfig.isAuthEnabled() && !this.getSessionUser().isAdmin()) {
            throw new VBoardException("Unauthorized Access - Current user is not an admin");
        }
    }

    void ensureUserHasNewsletterRole() {
        if (webSecurityConfig.isAuthEnabled() && !this.getSessionUser().hasRole(Role.Newsletter)) {
            throw new VBoardException("Unauthorized Access - Current user does not have the 'Newsletter' role");
        }
    }

    void ensureEmailMatchesSessionUser(String email) {
        final String sessionUserEmail = this.getSessionUser().getEmail();
        if (!sessionUserEmail.equals(email)) {
            throw new VBoardException("Unauthorized Access - The user given by the frontend is not the one that is given by the backend Front-end user=" + email + ", backend-user=" + sessionUserEmail);
        }
    }

    // Check whether the user (Format string) is the same as the session user String
    public void ensureNewEntityAuthorMatchesSessionUser(String pinAuthor) {
        final String userString = this.getSessionUser().getUserString();
        if (!userString.equals(pinAuthor)) {
            throw new VBoardException("Unauthorized Access - The pin author (" + pinAuthor + ") does not match the authenticated user (" + userString + ")");
        }
    }

    // Check whether the user has the authorization to do that action (the author or an admins)
    void ensureUserHasRightsToAlterPin(String pinAuthor) {
        final User sessionUser = this.getSessionUser();
        final String userString = sessionUser.getUserString();
        if (webSecurityConfig.isAuthEnabled() && !(userString.equals(pinAuthor) || sessionUser.isAdmin() || sessionUser.hasRole(Role.Moderateur))) {
            throw new VBoardException("Unauthorized Access - User cannot update nor delete pins: " + userString
                                      + " isAdmin=" + sessionUser.isAdmin());
        }
    }

    // Check whether the user has the authorization to do that action (the author or an admins)
    void ensureUserHasRightsToAlterComment(String commentAuthor) {
        final User sessionUser = this.getSessionUser();
        final String userString = sessionUser.getUserString();
        if (webSecurityConfig.isAuthEnabled() && !(userString.equals(commentAuthor) || sessionUser.isAdmin() || sessionUser.getEmail().equals(commentAuthor) || sessionUser.hasRole(Role.Moderateur))) {
            throw new VBoardException("Unauthorized Access - The user does not have the authorization to do that action(" + userString + ")");
        }
    }

}
