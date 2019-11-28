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

package com.vsct.vboard.config.cognito;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonWebTokenAuthentication implements Authentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonWebTokenAuthentication.class);
    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();
    private boolean isAuthenticated = false;
    private final String token;
    private final String keyId;
    private final String email;
    private final String username;
    private final List<String> roleAuthorities;

    JsonWebTokenAuthentication(String token, String rolesFieldName) {
        LOGGER.debug("Bare JWT token: {}", token);
        this.token = token;
        try {
            String[] parts = token.split("\\.");
            String b64DecodedPayload = StringUtils.newStringUtf8(Base64.decodeBase64(parts[0]));
            LOGGER.debug("Base64-decoded JWT token header: {}", b64DecodedPayload);
            Map<String, Object> header = (Map<String, Object>) OBJECT_MAPPER.readValue(b64DecodedPayload, Map.class);
            b64DecodedPayload = StringUtils.newStringUtf8(Base64.decodeBase64(parts[1]));
            LOGGER.debug("Base64-decoded JWT token payload: {}", b64DecodedPayload);
            Map<String, Object> payload = (Map<String, Object>) OBJECT_MAPPER.readValue(b64DecodedPayload, Map.class);
            keyId = (String) header.get("kid");
            email = (String) payload.get("email");
            username = (String) payload.get("username");
            String roles = (String) payload.get(rolesFieldName);
            if (roles == null) {
                throw new IllegalArgumentException("Field \"" + rolesFieldName + "\" not found in JWT payload");
            }
            roleAuthorities = Arrays.asList(roles.substring(2, roles.length() - 2).split(", "));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleAuthorities.stream().map(GroupAuthority::new)
                                       .collect(Collectors.toList());
    }

    @Override
    public Object getCredentials() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getDetails() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    String getKeyId() {
        return keyId;
    }

    String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    // Taken from: https://github.com/auth0/java-jwt/blob/3.8.3/lib/src/main/java/com/auth0/jwt/impl/JWTParser.java#L63
    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }
}
