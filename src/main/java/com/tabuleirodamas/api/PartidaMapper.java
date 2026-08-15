package com.tabuleirodamas.api;

import com.tabuleirodamas.api.dto.CasaDTO;
import com.tabuleirodamas.api.dto.MovimentoDTO;
import com.tabuleirodamas.api.dto.PartidaDTO;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Peca;
import com.tabuleirodamas.domain.Posicao;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PartidaMapper {

    private static final Comparator<Map.Entry<Posicao, Peca>> POR_LINHA_E_COLUNA =
            Comparator.<Map.Entry<Posicao, Peca>>comparingInt(entrada -> entrada.getKey().linha())
                    .thenComparingInt(entrada -> entrada.getKey().coluna());

    private PartidaMapper() {
    }

    public static PartidaDTO dePartida(Partida partida) {
        List<CasaDTO> casas = partida.tabuleiro().casas().entrySet().stream()
                .sorted(POR_LINHA_E_COLUNA)
                .map(PartidaMapper::deCasa)
                .toList();

        return new PartidaDTO(
                partida.id().toString(),
                partida.estado(),
                partida.vezDe(),
                partida.estado().vencedor().orElse(null),
                partida.capturaObrigatoria(),
                partida.placar(),
                partida.lancesSemProgresso(),
                casas,
                partida.historico().stream().map(PartidaMapper::deMovimento).toList());
    }

    private static CasaDTO deCasa(Map.Entry<Posicao, Peca> entrada) {
        Posicao posicao = entrada.getKey();
        Peca peca = entrada.getValue();
        return new CasaDTO(posicao.notacao(), posicao.linha(), posicao.coluna(),
                peca.cor(), peca.tipo());
    }

    public static MovimentoDTO deMovimento(Movimento movimento) {
        return new MovimentoDTO(
                movimento.caminhoEmNotacao(),
                movimento.capturas().stream().map(Posicao::notacao).sorted().toList(),
                movimento.promove());
    }
}
