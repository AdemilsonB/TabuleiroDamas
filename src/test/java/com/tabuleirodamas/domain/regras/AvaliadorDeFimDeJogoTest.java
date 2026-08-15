package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliadorDeFimDeJogoTest {

    @Test
    void partidaSegueEmAndamentoQuandoHaLancesDisponiveis() {
        assertThat(AvaliadorDeFimDeJogo.avaliar(Tabuleiro.inicial(), Cor.BRANCA, 0))
                .isEqualTo(EstadoPartida.EM_ANDAMENTO);
    }

    @Test
    void vencePorAusenciaDePecasDoAdversario() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA, 0))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void vencePorAfogamentoQuandoOAdversarioNaoTemLanceLegal() {
        // A preta em a7 avanca para a linha 6: b6 esta ocupada por branca e a casa de
        // pouso da captura (c5) tambem, entao ela nao tem lance legal.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a7", Cor.PRETA)
                .pedra("b6", Cor.BRANCA)
                .pedra("c5", Cor.BRANCA)
                .dama("h8", Cor.BRANCA)
                .construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA, 0))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void empataAoAtingirOLimiteDeLancesSemProgresso() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .dama("h6", Cor.PRETA)
                .construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.BRANCA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO))
                .isEqualTo(EstadoPartida.EMPATE);
        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.BRANCA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO - 1))
                .isEqualTo(EstadoPartida.EM_ANDAMENTO);
    }

    @Test
    void vitoriaTemPrecedenciaSobreEmpate() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a1", Cor.BRANCA).construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void estadoConheceOVencedorESeEstaEncerrado() {
        assertThat(EstadoPartida.EM_ANDAMENTO.encerrada()).isFalse();
        assertThat(EstadoPartida.EM_ANDAMENTO.vencedor()).isEmpty();
        assertThat(EstadoPartida.VITORIA_BRANCA.encerrada()).isTrue();
        assertThat(EstadoPartida.VITORIA_BRANCA.vencedor()).contains(Cor.BRANCA);
        assertThat(EstadoPartida.VITORIA_PRETA.vencedor()).contains(Cor.PRETA);
        assertThat(EstadoPartida.EMPATE.encerrada()).isTrue();
        assertThat(EstadoPartida.EMPATE.vencedor()).isEqualTo(Optional.<Cor>empty());
    }
}
