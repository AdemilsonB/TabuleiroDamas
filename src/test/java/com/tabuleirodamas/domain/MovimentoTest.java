package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimentoTest {

    @Test
    void movimentoSimplesTemCaminhoDeDoisPontosESemCapturas() {
        Movimento movimento = Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false);

        assertThat(movimento.origem()).isEqualTo(Posicao.de("c3"));
        assertThat(movimento.destino()).isEqualTo(Posicao.de("d4"));
        assertThat(movimento.ehCaptura()).isFalse();
        assertThat(movimento.quantidadeCapturada()).isZero();
    }

    @Test
    void sequenciaDeCapturaConheceOrigemDestinoEQuantidade() {
        Movimento movimento = new Movimento(
                List.of(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2")),
                Set.of(Posicao.de("c5"), Posicao.de("e3")),
                false);

        assertThat(movimento.origem()).isEqualTo(Posicao.de("b6"));
        assertThat(movimento.destino()).isEqualTo(Posicao.de("f2"));
        assertThat(movimento.ehCaptura()).isTrue();
        assertThat(movimento.quantidadeCapturada()).isEqualTo(2);
        assertThat(movimento.caminhoEmNotacao()).containsExactly("b6", "d4", "f2");
    }

    @Test
    void recusaCaminhoComMenosDeDuasCasas() {
        assertThatThrownBy(() -> new Movimento(List.of(Posicao.de("c3")), Set.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caminhoECapturasSaoImutaveis() {
        Movimento movimento = Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false);

        assertThatThrownBy(() -> movimento.caminho().add(Posicao.de("e5")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aplicarMoveAPecaDaOrigemParaODestino() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro depois = tabuleiro.aplicar(
                Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false));

        assertThat(depois.vazia(Posicao.de("c3"))).isTrue();
        assertThat(depois.pecaEm(Posicao.de("d4"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void aplicarRemoveTodasAsPecasCapturadas() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e3", Cor.PRETA)
                .construir();

        Tabuleiro depois = tabuleiro.aplicar(new Movimento(
                List.of(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2")),
                Set.of(Posicao.de("c5"), Posicao.de("e3")),
                false));

        assertThat(depois.contar(Cor.PRETA)).isZero();
        assertThat(depois.pecaEm(Posicao.de("f2"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void aplicarPromoveQuandoOMovimentoIndica() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("b2", Cor.BRANCA).construir();

        Tabuleiro depois = tabuleiro.aplicar(
                Movimento.simples(Posicao.de("b2"), Posicao.de("a1"), true));

        assertThat(depois.pecaEm(Posicao.de("a1"))).contains(Peca.dama(Cor.BRANCA));
    }

    @Test
    void aplicarNaoAlteraOTabuleiroOriginal() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        tabuleiro.aplicar(new Movimento(
                List.of(Posicao.de("c3"), Posicao.de("e5")),
                Set.of(Posicao.de("d4")),
                false));

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(1);
        assertThat(tabuleiro.pecaEm(Posicao.de("c3"))).contains(Peca.pedra(Cor.BRANCA));
    }
}
