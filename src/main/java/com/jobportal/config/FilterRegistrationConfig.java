package com.jobportal.config;

import com.jobportal.ratelimit.RateLimitFilter;
import com.jobportal.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Both JwtAuthenticationFilter and RateLimitFilter are annotated
 * @Component so Spring can inject their dependencies (JwtService, Redis,
 * etc.), and they're ALSO wired explicitly into Spring Security's filter
 * chain in SecurityConfig via addFilterBefore(). That combination is a
 * classic gotcha: Spring Boot separately auto-registers every Filter bean
 * as a plain servlet-container filter too, so without this class each of
 * these filters would silently run TWICE per request -- once in Security's
 * chain (correctly ordered) and once again via Boot's default filter
 * registration (unordered, outside Security's control). For
 * JwtAuthenticationFilter this mostly wastes a little CPU re-validating an
 * already-validated token; for RateLimitFilter it would double-count every
 * request against the rate limit, incorrectly halving the effective limit.
 * Setting setEnabled(false) here disables the redundant auto-registration
 * so each filter runs exactly once, in the position SecurityConfig defines.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}