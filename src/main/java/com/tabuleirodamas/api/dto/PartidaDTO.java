package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;

import java.util.List;
import java.util.Map;

public record PartidaDTO(
        String id,
        EstadoPartida estado,
        Cor vezDe,
        Cor vencedor,
        boolean capturaObrigatoria,
        Map<Cor, Integer> placar,
        int lancesSemProgresso,
        List<CasaDTO> casas,
        List<MovimentoDTO> historico) {
}
