package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Posicao;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Uma jogada e sempre o caminho completo: ["b6","d4","f2"]. Guardar so origem e
 * destino seria ambiguo, porque duas sequencias distintas podem terminar na mesma casa.
 */
public record JogadaRequest(
        @NotEmpty(message = "Informe o caminho da jogada.")
        @Size(min = 2, message = "O caminho precisa de ao menos duas casas.")
        List<String> caminho) {

    public List<Posicao> paraPosicoes() {
        return caminho.stream().map(Posicao::de).toList();
    }
}
