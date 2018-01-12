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

package com.vsct.vboard.config;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.Min;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticSearchClientConfig {
    @NotBlank
    private String serverUri;
    @NotBlank
    private String pinsIndex;
    @Min(value = 1)
    private int querySize;

    public ElasticSearchClientConfig() {
    }

    public void setServerUri(String serverUri) {
        this.serverUri = serverUri;
    }

    public void setPinsIndex(String pinsIndex) {
        this.pinsIndex = pinsIndex;
    }

    public void setQuerySize(int querySize) {
        this.querySize = querySize;
    }

    public String getServerUri() {
        return serverUri;
    }

    public String getPinsIndex() {
        return pinsIndex;
    }

    public int getQuerySize() {
        return querySize;
    }
}