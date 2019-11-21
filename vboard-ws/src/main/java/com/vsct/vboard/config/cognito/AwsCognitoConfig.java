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
