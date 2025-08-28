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

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AwsCognitoSecurityContextRepository implements SecurityContextRepository {

    private final AwsCognitoConfig awsCognitoConfig;

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
