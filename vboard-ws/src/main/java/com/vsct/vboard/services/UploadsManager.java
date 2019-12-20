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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class UploadsManager {
    private final UploadsConfig uploadsConfig;
    private final ProxyConfig proxyConfig;

    @Autowired
    public UploadsManager(UploadsConfig uploadsConfig, ProxyConfig proxyConfig) {
        this.uploadsConfig = uploadsConfig;
        this.proxyConfig = proxyConfig;
    }

    // img should be the base64 encoded image (exception: "default")
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION") // handled by try() syntax
    public void saveAvatar(String img, String name) {
        if ("default".equals(img)) { // which means the user wanted to delete it's avatar, thus the default one is set
            try (InputStream is = this.getClass().getClassLoader().getResource("avatar.png").openStream()) { // Save the avatar the this.openAMConfig.getHostName() + /images folder (NAS)
                try (OutputStream os = new FileOutputStream(getAvatarImagesDirectory().resolve(name + ".png").toFile())) {
                    IOUtils.copy(is, os);
                }
            } catch (IOException e) {
                throw new VBoardException("Could not write default avatar image to filesystem", e);
            }
        } else {
            byte[] data = Base64.getDecoder().decode(img);
            try (OutputStream stream = new FileOutputStream(getAvatarImagesDirectory().resolve(name + ".png").toFile())) {
                stream.write(data);
            } catch (IOException e) {
                throw new VBoardException("Could not write avatar image to filesystem", e);
            }
        }
    }

    // img should be the base64 encoded image (exception: url)
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    public void savePinImage(String img, String name) {
        if (img.startsWith("http")) { // If the image is an url, the image is downloaded and uploaded on the pinImg folder (NAS)
            if (img.endsWith("svg") || img.endsWith("gif")) {
                throw new VBoardException("Neither .gif nor .svg are currently supported"); // cf. https://github.com/voyages-sncf-technologies/vboard/issues/82
            }
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
            try (InputStream is = url.openStream()) {
                try (OutputStream os = new FileOutputStream(getPinsImagesDirectory().resolve(name + ".png").toFile())) {
                    byte[] b = new byte[2048];
                    int length;
                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }
                }
            } catch (IOException e) {
                throw new VBoardException("Could not write pin image to filesystem", e);
            }
        } else { // Case of a base 64 image:
            byte[] data = Base64.getDecoder().decode(img);
            try (OutputStream stream = new FileOutputStream(getPinsImagesDirectory().resolve(name + ".png").toFile())) {
                stream.write(data);
            } catch (IOException e) {
                throw new VBoardException("Could not decode base64 image", e);
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

    final public boolean isMultiplePinsPerUrlAllowed() {
        return this.uploadsConfig.isMultiplePinsPerUrlAllowed();
    }
}
