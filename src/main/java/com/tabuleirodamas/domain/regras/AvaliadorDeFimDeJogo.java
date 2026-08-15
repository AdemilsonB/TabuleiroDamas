package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Tabuleiro;

/**
 * Decide se a partida terminou. Substitui a contagem de pontos do prototipo, que
 * media o jogador errado e nao enxergava afogamento nem empate.
 */
public final class AvaliadorDeFimDeJogo {

    /** 20 lances de cada lado sem captura e sem movimento de pedra. */
    public static final int LIMITE_LANCES_SEM_PROGRESSO = 40;

    private AvaliadorDeFimDeJogo() {
    }

    public static EstadoPartida avaliar(Tabuleiro tabuleiro, Cor proximoAJogar,
                                        int lancesSemProgresso) {
        boolean semPecas = tabuleiro.contar(proximoAJogar) == 0;
        boolean afogado = GeradorDeMovimentos.legais(tabuleiro, proximoAJogar).isEmpty();

        if (semPecas || afogado) {
            return EstadoPartida.vitoriaDe(proximoAJogar.oposta());
        }
        if (lancesSemProgresso >= LIMITE_LANCES_SEM_PROGRESSO) {
            return EstadoPartida.EMPATE;
        }
        return EstadoPartida.EM_ANDAMENTO;
    }
}
