package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.TipoPeca;

/** Casa ocupada. Traz a notacao e os indices para o cliente escolher o que usar. */
public record CasaDTO(String notacao, int linha, int coluna, Cor cor, TipoPeca tipo) {
}
