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

package com.vsct.vboard.config.cognito;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class AwsCognitoAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoAuthenticationProvider.class);

    private final AwsCognitoConfig awsCognitoConfig;

    public AwsCognitoAuthenticationProvider(AwsCognitoConfig awsCognitoConfig) {
        this.awsCognitoConfig = awsCognitoConfig;
    }

    @Override
    @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        JsonWebTokenAuthentication jwtAuth = (JsonWebTokenAuthentication) authentication;
        try {
            Algorithm algorithm = Algorithm.ECDSA256(new AwsCognitoECDSAKeyProvider(awsCognitoConfig.getRegion(), jwtAuth.getKeyId()));
            JWT.require(algorithm).build().verify(jwtAuth.getToken());
            jwtAuth.setAuthenticated(true);
            logger.debug("Authenticated with JWT with scopes: {}", authentication.getAuthorities());
            return authentication;
        } catch (JWTVerificationException e) {
            logger.error("JWT ECDSA256 verify error for user: {}", jwtAuth.getName(), e);
            throw new BadCredentialsException("Not a valid token", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(JsonWebTokenAuthentication.class);
    }
}
