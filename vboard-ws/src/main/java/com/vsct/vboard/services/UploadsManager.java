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

package com.vsct.vboard.services;

import com.vsct.vboard.config.ProxyConfig;
import com.vsct.vboard.config.UploadsConfig;
import com.vsct.vboard.models.VBoardException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Proxy;
import java.net.URL;

@Service
public class UploadsManager {
    private final UploadsConfig uploadsConfig;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ProxyConfig proxyConfig;

    @Autowired
    UploadsManager(UploadsConfig uploadsConfig, ProxyConfig proxyConfig) throws IOException {
        this.uploadsConfig = uploadsConfig;
        if (!Files.exists(getPinsImagesDirectory())) {
            Files.createDirectory(getPinsImagesDirectory());
        }
        if (!Files.exists(getAvatarImagesDirectory())) {
            Files.createDirectory(getAvatarImagesDirectory());
        }
        this.proxyConfig = proxyConfig;
    }

    // img should be the base64 encoded image (exception: "default")
    public void saveAvatar(String img, String name) {
        if (!"default".equals(img)) { // which means the user wanted to delete it's avatar, thus the default one is set
            byte[] data = Base64.getDecoder().decode(img);
            try (OutputStream stream = new FileOutputStream(getAvatarImagesDirectory().resolve(name + ".png").toFile())) {
                stream.write(data);
                stream.close();
            } catch (Exception e) {
                this.logger.error(e.getMessage());
            }
        } else {
            try { // Save the avatar the this.openAMConfig.getHostName() + /images folder (NAS)
                InputStream is = this.getClass().getClassLoader().getResource("avatar.png").openStream();
                OutputStream os = new FileOutputStream(getAvatarImagesDirectory().resolve(name + ".png").toFile());
                IOUtils.copy(is, os);
                is.close();
                os.close();
                this.logger.debug("Avatar de base enregistré dans le NAS pour: {}", name);
            } catch (Exception e) {
                this.logger.error(e.getMessage());
            }
        }

    }

    // img should be the base64 encoded image (exception: url)
    public void savePinImage(String img, String name) {
        if (!img.startsWith("http")) { // If the image is an url, the image is downloaded and uploaded on the pinImg folder (NAS)
            byte[] data = Base64.getDecoder().decode(img);
            try (OutputStream stream = new FileOutputStream(getPinsImagesDirectory().resolve(name + ".png").toFile())) {
                stream.write(data);
                stream.close();
            } catch (Exception e) {
                this.logger.error(e.getMessage());
            }
        } else if (!img.endsWith("svg") && !img.endsWith("gif")) {
            URL url;
            try {
                if (proxyConfig.getProxy() != Proxy.NO_PROXY) {
                    url = new URL("http", proxyConfig.getHostname(), proxyConfig.getPort(), img);
                } else {
                    url = new URL(img);
                }
            } catch (MalformedURLException e) {
                throw new VBoardException("Could not retrieve pin image from the web", e);
            }
            try {
                InputStream is = url.openStream();
                OutputStream os = new FileOutputStream(getPinsImagesDirectory().resolve(name + ".png").toFile());
                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();
            } catch (IOException e) {
                throw new VBoardException("Could not write pin image to filesystem", e);
            }
        }
    }

    /**
     * Get the base64 encoded avatar of a profil (user or team)
     *
     * @return String base64 encoded image
     */
    public String getAvatar(String name) {
        try {
            BufferedImage img = ImageIO.read(getAvatarImagesDirectory().resolve(name + ".png").toFile());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            ImageIO.write(img, "png", bos);
            byte[] imageBytes = bos.toByteArray();

            bos.close();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new VBoardException("Avatar encoding error", e);
        }
    }

    /**
     * Get the base64 encoded image of a pin (entry parameter: id)
     *
     * @return String base64 encoded image
     */
    public String getImage(String name) {
        try {
            BufferedImage img = ImageIO.read(getPinsImagesDirectory().resolve(name + ".png").toFile());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            ImageIO.write(img, "png", bos);
            byte[] imageBytes = bos.toByteArray();

            bos.close();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new VBoardException("Image encoding error", e);
        }
    }

    final public Path getPinsImagesDirectory() {
        return this.uploadsConfig.getImagesStorageDirectory().resolve("pinImg");
    }

    final public Path getAvatarImagesDirectory() {
        return this.uploadsConfig.getImagesStorageDirectory().resolve("avatar");
    }

    final public Path getBlogImagesDirectory() {
        return this.uploadsConfig.getBlogImagesDirectory();
    }

}
