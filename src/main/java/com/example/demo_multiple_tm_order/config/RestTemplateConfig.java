package com.example.demo_multiple_tm_order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 *
 * Provides a RestTemplate bean for making HTTP requests to other microservices.
 * Used by BffService to coordinate distributed transactions across services.
 *
 * Note: In production environments, consider:
 * - Adding connection pooling configuration
 * - Setting appropriate timeouts
 * - Adding retry logic
 * - Implementing circuit breaker patterns (e.g., with Resilience4j)
 * - Using service discovery (e.g., with Spring Cloud)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
