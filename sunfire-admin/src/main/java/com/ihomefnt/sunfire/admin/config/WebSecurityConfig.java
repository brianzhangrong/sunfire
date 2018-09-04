package com.ihomefnt.sunfire.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * Override this method to configure {@link WebSecurity}. For example, if you wish to ignore
     * certain requests.
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**");
    }

    /**
     * Override this method to configure the {@link HttpSecurity}. Typically subclasses should not
     * invoke this method by calling super as it may override their configuration. The default
     * configuration is:
     *
     * <pre>
     * http.authorizeRequests().anyRequest().authenticated().and().formLogin().and().httpBasic();
     * </pre>
     *
     * @param http the {@link HttpSecurity} to modify
     * @throws Exception if an error occurs
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        //http.authorizeRequests().and().csrf().ignoringAntMatchers("/**");
        http.csrf().disable();
    }
}
