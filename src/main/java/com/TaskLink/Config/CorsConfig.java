package com.TaskLink.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;


import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public org.springframework.web.cors.UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        
        cors.addAllowedOriginPattern("*");   // Allow all domains
        cors.addAllowedMethod("*");          // Allow all methods
        cors.addAllowedHeader("*");          // Allow all headers
        
        cors.setAllowCredentials(true);      // Allow cookies/tokens

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
