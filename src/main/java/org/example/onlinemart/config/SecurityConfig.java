package org.example.onlinemart.config;

import org.example.onlinemart.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@SuppressWarnings("deprecation")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // We don't use the default Spring Auth Manager with user details,
        // because we handle authentication ourselves via JWT.
        // This can remain empty or you can set a custom provider if you prefer.
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Optionally ignore static resources
        web.ignoring().antMatchers("/css/**", "/js/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // for REST
                .authorizeRequests()
                // registration, login endpoints
                .antMatchers("/api/auth/**").permitAll()
                // admin endpoints
                .antMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                // user endpoints
                .antMatchers("/api/user/**").hasAuthority("ROLE_USER")
                // anything else
                .anyRequest().authenticated()
                .and()
                // add our JWT filter
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
    }
}