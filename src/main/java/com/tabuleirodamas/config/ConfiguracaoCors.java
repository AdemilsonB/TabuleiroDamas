package com.tabuleirodamas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Libera o front Angular em desenvolvimento. Ajuste damas.cors.origens em producao. */
@Configuration
public class ConfiguracaoCors implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public ConfiguracaoCors(
            @Value("${damas.cors.origens:http://localhost:4200}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas.clone();
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
