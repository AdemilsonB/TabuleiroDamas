package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeMovimentosSimplesTest {

    @Test
    void pedraBrancaAvancaUmaDiagonalParaCima() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("c5"), Posicao.de("e5"));
    }

    @Test
    void pedraPretaAvancaUmaDiagonalParaBaixo() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.PRETA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.PRETA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("c3"), Posicao.de("e3"));
    }

    @Test
    void pedraNaoAndaParaTrasSemCaptura() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .doesNotContain(Posicao.de("c3"), Posicao.de("e3"));
    }

    @Test
    void pedraNaoAndaParaCasaOcupada() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::origem).doesNotContain(Posicao.de("d4"));
    }

    @Test
    void pedraPretaPromoveAoAlcancarALinhaUm() {
        Tabuleiro naLinhaDois = TabuleiroBuilder.vazio().pedra("b2", Cor.PRETA).construir();
        Tabuleiro longe = TabuleiroBuilder.vazio().pedra("b8", Cor.PRETA).construir();

        assertThat(GeradorDeMovimentos.simples(naLinhaDois, Cor.PRETA))
                .isNotEmpty()
                .allMatch(Movimento::promove);
        assertThat(GeradorDeMovimentos.simples(longe, Cor.PRETA)).noneMatch(Movimento::promove);
    }

    @Test
    void pedraBrancaPromoveAoAlcancarALinhaOito() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("a7", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .allMatch(Movimento::promove);
    }

    @Test
    void damaDeslizaPelaDiagonalInteira() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a1", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(
                        Posicao.de("b2"), Posicao.de("c3"), Posicao.de("d4"),
                        Posicao.de("e5"), Posicao.de("f6"), Posicao.de("g7"), Posicao.de("h8"));
    }

    @Test
    void damaParaAntesDeQualquerPeca() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("b2"), Posicao.de("c3"));
    }

    @Test
    void damaNuncaPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a7", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA)).noneMatch(Movimento::promove);
    }

    @Test
    void geraSomenteMovimentosDaCorPedida() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a7", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::origem).containsOnly(Posicao.de("d4"));
    }

    @Test
    void gerarNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(12);
    }

    @Test
    void posicaoInicialTemSeteAberturasParaCadaLado() {
        assertThat(GeradorDeMovimentos.simples(Tabuleiro.inicial(), Cor.BRANCA)).hasSize(7);
        assertThat(GeradorDeMovimentos.simples(Tabuleiro.inicial(), Cor.PRETA)).hasSize(7);
    }
}
