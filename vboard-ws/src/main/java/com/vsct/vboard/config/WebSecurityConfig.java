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

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.authentication.KeycloakLogoutHandler;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/*
 * This class is inspired by KeycloakWebSecurityConfigurerAdapter,
 * but only creates Keycloack Beans if it is configured through environment variables
 */
@KeycloakConfiguration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements WebSecurityConfigurer<WebSecurity> {

    private static final SessionAuthenticationStrategy SESSION_AUTH_STRATEGY = new NullAuthenticatedSessionStrategy();
    private ApplicationContext applicationContext;

    @Autowired
    public WebSecurityConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        if (KeycloakEnabledInEnv.evaluate()) {
            auth.authenticationProvider(new KeycloakAuthenticationProvider());
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionAuthenticationStrategy(SESSION_AUTH_STRATEGY)
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
        http.csrf().disable();
        http.authorizeRequests()
            .requestMatchers(new AntPathRequestMatcher("/**", "GET")).permitAll()
            .antMatchers("/pins/vblog").permitAll();

        if (KeycloakEnabledInEnv.evaluate()) {
            configureKeycloakSecurity(http);
            // Keycloak is configured => we require auth
            http.authorizeRequests()
                    .antMatchers("/**").authenticated();
        }
    }

    protected void configureKeycloakSecurity(HttpSecurity http) throws Exception {
        // Not @Autowiring those beans as they may not be available because of the @Conditional
        final AdapterDeploymentContext adc = applicationContext.getBean(AdapterDeploymentContext.class);
        final KeycloakPreAuthActionsFilter keycloakPreAuthActionsFilter = applicationContext.getBean(KeycloakPreAuthActionsFilter.class);
        http.addFilterBefore(keycloakPreAuthActionsFilter, LogoutFilter.class)
            .addFilterBefore(keycloakAuthenticationProcessingFilter(), BasicAuthenticationFilter.class)
            .exceptionHandling().authenticationEntryPoint(new KeycloakAuthenticationEntryPoint(adc));
        http.logout()
            .addLogoutHandler(new KeycloakLogoutHandler(adc))
            .logoutUrl("/sso/logout").permitAll()
            .logoutSuccessUrl("/");
    }

    // Must be injected so that GenericFilterBean.initFilterBean will be called via InitializingBean.afterPropertiesSet
    @Bean
    @Conditional(KeycloakEnabledInEnv.class)
    @Autowired
    public static KeycloakPreAuthActionsFilter keycloakPreAuthActionsFilter(HttpSessionManager sessionManager) {
        return new KeycloakPreAuthActionsFilter(sessionManager);
    }

    // Also injected in `KeycloakPreAuthActionsFilter` as the `deploymentContext` property through the `initFilterBean` method.
    // !BEWARE! if this `deploymentContext` property ends up null, it will lead to a NullPointerException in `org.keycloak.adapters.PreAuthActionsHandler.preflightCors:107`
    @Bean
    @Conditional(KeycloakEnabledInEnv.class)
    public static AdapterDeploymentContext adapterDeploymentContext() throws Exception {
        AdapterDeploymentContextFactoryBean factoryBean = new AdapterDeploymentContextFactoryBean(new KeycloakSpringBootConfigResolver());
        factoryBean.afterPropertiesSet(); // creates the AdapterDeploymentContext
        return factoryBean.getObject();
    }

    private KeycloakAuthenticationProcessingFilter keycloakAuthenticationProcessingFilter() throws Exception {
        KeycloakAuthenticationProcessingFilter filter = new KeycloakAuthenticationProcessingFilter(authenticationManagerBean());
        filter.setSessionAuthenticationStrategy(SESSION_AUTH_STRATEGY);
        filter.setApplicationContext(applicationContext); // required for below function
        filter.afterPropertiesSet(); // sets adapterDeploymentContext by retrieving Bean
        return filter;
    }
}
