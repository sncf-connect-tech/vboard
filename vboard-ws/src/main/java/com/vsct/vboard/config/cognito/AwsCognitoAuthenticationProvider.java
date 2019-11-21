package com.vsct.vboard.config.cognito;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class AwsCognitoAuthenticationProvider implements AuthenticationProvider {

    private static Logger logger = LoggerFactory.getLogger(AwsCognitoAuthenticationProvider.class);

    private AwsCognitoConfig awsCognitoConfig;

    public AwsCognitoAuthenticationProvider(AwsCognitoConfig awsCognitoConfig) {
        this.awsCognitoConfig = awsCognitoConfig;
    }

    @Override
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
