package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabuleiroTest {

    @Test
    void posicaoInicialTemDozePecasDeCadaCor() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(12);
    }

    @Test
    void posicaoInicialUsaSomenteCasasEscuras() {
        assertThat(Tabuleiro.inicial().casas().keySet()).allMatch(Posicao::jogavel);
    }

    @Test
    void posicaoInicialColocaPretasNoTopoEBrancasNaBase() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.pecaEm(Posicao.de("b8"))).contains(Peca.pedra(Cor.PRETA));
        assertThat(tabuleiro.pecaEm(Posicao.de("a1"))).contains(Peca.pedra(Cor.BRANCA));
        assertThat(tabuleiro.posicoesDe(Cor.PRETA)).allMatch(p -> p.linha() <= 2);
        assertThat(tabuleiro.posicoesDe(Cor.BRANCA)).allMatch(p -> p.linha() >= 5);
    }

    @Test
    void posicaoInicialDeixaAsDuasLinhasCentraisVazias() {
        assertThat(Tabuleiro.inicial().casas().keySet())
                .noneMatch(p -> p.linha() == 3 || p.linha() == 4);
    }

    @Test
    void todasAsPecasIniciaisSaoPedras() {
        assertThat(Tabuleiro.inicial().casas().values()).noneMatch(Peca::ehDama);
    }

    @Test
    void comPecaDevolveNovoTabuleiroSemAlterarOOriginal() {
        Tabuleiro original = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro novo = original.comPeca(Posicao.de("d4"), Peca.dama(Cor.PRETA));

        assertThat(original.contar(Cor.PRETA)).isZero();
        assertThat(novo.contar(Cor.PRETA)).isEqualTo(1);
        assertThat(novo.pecaEm(Posicao.de("c3"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void semPecaDevolveNovoTabuleiroSemAlterarOOriginal() {
        Tabuleiro original = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro novo = original.semPeca(Posicao.de("c3"));

        assertThat(original.ocupada(Posicao.de("c3"))).isTrue();
        assertThat(novo.vazia(Posicao.de("c3"))).isTrue();
    }

    @Test
    void mapaDeCasasNaoPodeSerModificadoPeloChamador() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThatThrownBy(() -> tabuleiro.casas().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pecaPromovidaViraDamaMantendoACor() {
        assertThat(Peca.pedra(Cor.BRANCA).promovida()).isEqualTo(Peca.dama(Cor.BRANCA));
        assertThat(Peca.dama(Cor.PRETA).promovida()).isEqualTo(Peca.dama(Cor.PRETA));
    }
}
