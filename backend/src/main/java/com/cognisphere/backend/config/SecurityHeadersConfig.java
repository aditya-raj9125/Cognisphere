package com.cognisphere.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.io.IOException;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                httpResponse.setHeader("X-Frame-Options", "DENY");
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
                chain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
