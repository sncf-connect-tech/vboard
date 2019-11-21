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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("aws-cognito")
public class AwsCognitoConfig {
    private boolean enabled;
    private String jwtHttpHeaderName;
    private String region;
    private String rolesFieldName;

    public boolean isEnabled() {
        return enabled;
    }

    public String getJwtHttpHeaderName() {
        return jwtHttpHeaderName;
    }

    public String getRegion() {
        return region;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setJwtHttpHeaderName(String jwtHttpHeaderName) {
        this.jwtHttpHeaderName = jwtHttpHeaderName;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRolesFieldName() {
        return rolesFieldName;
    }

    public void setRolesFieldName(String rolesFieldName) {
        this.rolesFieldName = rolesFieldName;
    }
}
