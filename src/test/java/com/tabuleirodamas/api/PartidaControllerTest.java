package com.tabuleirodamas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PartidaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String criarPartida() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/partidas"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String corpoDeJogada(String... casas) throws Exception {
        return objectMapper.writeValueAsString(Map.of("caminho", casas));
    }

    @Test
    void criaPartidaComTabuleiroCompletoEVezDasBrancas() throws Exception {
        mockMvc.perform(post("/api/partidas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vezDe").value("BRANCA"))
                .andExpect(jsonPath("$.estado").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.casas.length()").value(24))
                .andExpect(jsonPath("$.capturaObrigatoria").value(false));
    }

    @Test
    void consultaPartidaPeloId() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void listaTodosOsLancesLegaisDaVez() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void filtraLancesLegaisPelaCasaDeOrigem() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id).param("origem", "a3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].caminho[1]").value("b4"));
    }

    @Test
    void consultaDeOrigemVaziaDevolveListaVaziaComStatusOk() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id).param("origem", "d4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void executaJogadaValidaEPassaAVez() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3", "d4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vezDe").value("PRETA"))
                .andExpect(jsonPath("$.historico.length()").value(1));
    }

    @Test
    void jogadaIlegalRespondeQuatrocentosEVinteEDoisComMotivoTipado() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("b6", "c5")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.motivo").value("PECA_DO_ADVERSARIO"))
                .andExpect(jsonPath("$.title").value("Movimento ilegal"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void casaForaDoTabuleiroRespondeQuatrocentosEVinteEDois() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3", "z9")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.motivo").value("FORA_DO_TABULEIRO"));
    }

    @Test
    void caminhoCurtoDemaisRespondeQuatrocentos() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void partidaInexistenteRespondeQuatrocentosEQuatro() throws Exception {
        mockMvc.perform(get("/api/partidas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Partida não encontrada"));
    }

    @Test
    void removePartida() throws Exception {
        String id = criarPartida();

        mockMvc.perform(delete("/api/partidas/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/partidas/{id}", id)).andExpect(status().isNotFound());
    }
}
