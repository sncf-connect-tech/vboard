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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "uploads")
public class UploadsConfig {
    private Path imagesStorageDirectory;
    private Path blogImagesDirectory;

    public UploadsConfig() {
    }

    public void setImagesStorageDirectory(Path imagesStorageDirectory) throws IOException {
        if (!Files.exists(imagesStorageDirectory)) {
            Files.createDirectory(imagesStorageDirectory);
        }
        this.imagesStorageDirectory = imagesStorageDirectory;
    }

    public Path getImagesStorageDirectory() {
        return imagesStorageDirectory;
    }

    public void setBlogImagesDirectory(Path blogImagesDirectory) throws IOException {
        if (!Files.exists(blogImagesDirectory)) {
            Files.createDirectory(blogImagesDirectory);
        }
        this.blogImagesDirectory = blogImagesDirectory;
    }

    public Path getBlogImagesDirectory() {
        return blogImagesDirectory;
    }

}