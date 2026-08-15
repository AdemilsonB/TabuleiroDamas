package com.tabuleirodamas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI documentacaoDaApi() {
        return new OpenAPI().info(new Info()
                .title("API de Damas Brasileiras")
                .version("1.0.0")
                .description("Backend do jogo de damas: captura obrigatória, lei da maioria "
                        + "e dama voadora. Contrato pronto para geração de client TypeScript."));
    }
}
