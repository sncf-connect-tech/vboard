package com.vsct.vboard.config.cognito;

import com.auth0.jwt.interfaces.ECDSAKeyProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.HashMap;
import java.util.Map;

import static com.vsct.vboard.config.cognito.PemUtils.getPublicKey;
import static com.vsct.vboard.config.cognito.PemUtils.parsePEM;

public class AwsCognitoECDSAKeyProvider implements ECDSAKeyProvider {

    private static Logger LOGGER = LoggerFactory.getLogger(AwsCognitoECDSAKeyProvider.class);
    private String region;
    private String kid;
    private Map<String, ECPublicKey> cache = new HashMap<>();

    AwsCognitoECDSAKeyProvider(String region, String kid) {
        this.region = region;
        this.kid = kid;
    }

    @Override
    public ECPublicKey getPublicKeyById(String keyId) {
        ECPublicKey publicKey = cache.get(kid);
        if (publicKey != null) {
            LOGGER.debug("Cache hit for kid={}", kid);
            return publicKey;
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(String.format("https://public-keys.auth.elb.%s.amazonaws.com/%s", region, kid));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                InputStream content = response.getEntity().getContent();
                try (InputStreamReader textReader = new InputStreamReader(content)) {
                    publicKey = (ECPublicKey) getPublicKey(parsePEM(textReader), "EC");
                    cache.put(kid, publicKey);
                    LOGGER.debug("New cache entry set for kid={} from https://public-keys.auth.elb.{}.amazonaws.com", kid, region);
                    return publicKey;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ECPrivateKey getPrivateKey() {
        throw new NotImplementedException();
    }

    @Override
    public String getPrivateKeyId() {
        throw new NotImplementedException();
    }
}
