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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for extracting the payload of a signed request sent by Facebook.
 *
 * @author Craig Walls
 */
public class SignedRequestDecoder {

    private final String secret;

    private final ObjectMapper objectMapper;

    /**
     * @param secret the application secret used in creating and verifying the signature of the signed request.
     */
    public SignedRequestDecoder(String secret) {
        this.secret = secret;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    /**
     * Decodes a signed request, returning the payload of the signed request as a Map
     *
     * @param signedRequest the value of the signed_request parameter sent by Facebook.
     * @return the payload of the signed request as a Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, ?> decodeSignedRequest(String signedRequest) {
        return decodeSignedRequest(signedRequest, Map.class);
    }

    /**
     * Decodes a signed request, returning the payload of the signed request as a specified type.
     *
     * @param signedRequest the value of the signed_request parameter sent by Facebook.
     * @param type          the type to bind the signed_request to.
     * @param <T>           the Java type to bind the signed_request to.
     * @return the payload of the signed request as an object
     */
    public <T> T decodeSignedRequest(String signedRequest, Class<T> type) {
        String[] split = signedRequest.split("\\.");
        String encodedSignature = split[0];
        String payload = split[1];
        String decoded = base64DecodeToString(payload);
        byte[] signature = base64DecodeToBytes(encodedSignature);
        try {
            T data = objectMapper.readValue(decoded, type);
            String algorithm = objectMapper.readTree(decoded).get("algorithm").textValue();
            if (!"HMAC-SHA256".equals(algorithm)) {
                throw new RuntimeException("Unknown encryption algorithm: " + algorithm);
            }
            byte[] expectedSignature = encrypt(payload, secret);
            if (!Arrays.equals(expectedSignature, signature)) {
                throw new RuntimeException("Invalid signature.");
            }
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Error parsing payload.", e);
        }
    }

    private String padForBase64(String base64) {
        return base64 + PADDING.substring(0, (4 - base64.length() % 4) % 4);
    }

    private byte[] base64DecodeToBytes(String in) {
        return Base64.getDecoder().decode(padForBase64(in.replace('_', '/').replace('-', '+')));
    }

    private String base64DecodeToString(String in) {
        return new String(base64DecodeToBytes(in), UTF_8);
    }

    private byte[] encrypt(String base, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(UTF_8), HMAC_SHA256_MAC_NAME);
            Mac mac = Mac.getInstance(HMAC_SHA256_MAC_NAME);
            mac.init(secretKeySpec);
            return mac.doFinal(base.getBytes(UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String PADDING = "===";

    private static final String HMAC_SHA256_MAC_NAME = "HMACSHA256";

}