package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentosLegaisTest {

    @Test
    void semCapturaDisponivelDevolveOsMovimentosSimples() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA))
                .hasSize(2)
                .noneMatch(Movimento::ehCaptura);
    }

    @Test
    void havendoCapturaOMovimentoSimplesDeixaDeSerLegal() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .pedra("a1", Cor.BRANCA)
                .construir();

        List<Movimento> legais = GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA);

        assertThat(legais).singleElement().satisfies(movimento -> {
            assertThat(movimento.ehCaptura()).isTrue();
            assertThat(movimento.destino()).isEqualTo(Posicao.de("f6"));
        });
        assertThat(legais).extracting(Movimento::origem).doesNotContain(Posicao.de("a1"));
    }

    @Test
    void leiDaMaioriaDescartaAsCapturasMenores() {
        // a1 encadeia tres capturas; h2 so captura uma. So a de tres e legal.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .pedra("h2", Cor.BRANCA)
                .pedra("g3", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(3);
                    assertThat(movimento.origem()).isEqualTo(Posicao.de("a1"));
                });
    }

    @Test
    void leiDaMaioriaMantemEmpatesDeQuantidade() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA))
                .hasSize(2)
                .allMatch(movimento -> movimento.quantidadeCapturada() == 1);
    }

    @Test
    void legaisDeFiltraPelaCasaDeOrigem() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a1", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("a1")))
                .extracting(Movimento::destino).containsExactly(Posicao.de("b2"));
    }

    @Test
    void legaisDeDevolveVazioParaCasaVaziaOuPecaAdversaria() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("h8"))).isEmpty();
        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("a7"))).isEmpty();
    }

    @Test
    void existeCapturaRespondeSemAlterarOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.existeCaptura(tabuleiro, Cor.BRANCA)).isTrue();
        assertThat(GeradorDeMovimentos.existeCaptura(tabuleiro, Cor.PRETA)).isTrue();
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(1);
    }

    @Test
    void posicaoInicialNaoTemCapturaDisponivel() {
        assertThat(GeradorDeMovimentos.existeCaptura(Tabuleiro.inicial(), Cor.BRANCA)).isFalse();
        assertThat(GeradorDeMovimentos.legais(Tabuleiro.inicial(), Cor.BRANCA)).hasSize(7);
    }
}
