package com.vsct.vboard.config.cognito;

import org.springframework.security.core.GrantedAuthority;

public class GroupAuthority implements GrantedAuthority {

    private final String authority;

    public GroupAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }
}
