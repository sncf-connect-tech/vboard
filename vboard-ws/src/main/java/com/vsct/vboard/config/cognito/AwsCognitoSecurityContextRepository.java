package com.vsct.vboard.config.cognito;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AwsCognitoSecurityContextRepository implements SecurityContextRepository {

    private AwsCognitoConfig awsCognitoConfig;

    public AwsCognitoSecurityContextRepository(AwsCognitoConfig awsCognitoConfig) {
        this.awsCognitoConfig = awsCognitoConfig;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder httpRequestResponseHolder) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        String token = httpRequestResponseHolder.getRequest().getHeader(awsCognitoConfig.getJwtHttpHeaderName());
        if (token != null) {
            context.setAuthentication(new JsonWebTokenAuthentication(token, awsCognitoConfig.getRolesFieldName()));
        }
        return context;
    }

    @Override
    public void saveContext(SecurityContext securityContext, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

    }

    @Override
    public boolean containsContext(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(awsCognitoConfig.getJwtHttpHeaderName()) != null;
    }
}
