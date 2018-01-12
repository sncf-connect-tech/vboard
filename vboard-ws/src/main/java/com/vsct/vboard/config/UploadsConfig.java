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

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "uploads")
public class UploadsConfig {
    @NotBlank
    private String providerPath;
    @NotBlank
    private String wordpressImagePath;

    public UploadsConfig() {
    }

    public void setProviderPath(String providerPath) {
        this.providerPath = providerPath;
    }

    public String getProviderPath() {
        return providerPath;
    }

    public void setWordpressImagePath(String wordpressImagePath) {
        this.wordpressImagePath = wordpressImagePath;
    }

    public String getWordpressImagePath() {
        return wordpressImagePath;
    }

}