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

package com.vsct.vboard.config.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Created by lucas_cimon on 16/09/2017.
 */
public class KeycloakEnabledInEnv implements Condition {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeycloakEnabledInEnv.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return evaluate();
    }

    static public boolean evaluate() {
        final Map<String, String> env = System.getenv();
        boolean enabled = "true".equals(env.getOrDefault("KCK_ENABLED", "false"));
        if (enabled && (!env.containsKey("KCK_PUBLIC_HOST") || env.containsKey("KCK_REALM_KEY"))) {
            LOGGER.warn("$KCK_ENABLED is set but not both $KCK_PUBLIC_HOST & $KCK_REALM_KEY");
        }
        return enabled;
    }
}
