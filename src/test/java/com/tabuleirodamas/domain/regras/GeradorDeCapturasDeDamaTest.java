package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeCapturasDeDamaTest {

    @Test
    void damaCapturaADistanciaEPousaEmQualquerCasaLivreDepois() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino)
                .containsExactlyInAnyOrder(
                        Posicao.de("e5"), Posicao.de("f6"), Posicao.de("g7"), Posicao.de("h8"));
    }

    @Test
    void damaEBarradaPorDuasPecasConsecutivas() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void damaNaoPulaPecaDaPropriaCor() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void damaEncadeiaCapturasMudandoDeDirecao() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("c3", Cor.PRETA)
                .pedra("c5", Cor.PRETA)
                .construir();

        List<Movimento> capturas = GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(capturas).extracting(Movimento::caminhoEmNotacao)
                .contains(List.of("a1", "d4", "b6"), List.of("a1", "d4", "a7"));
        assertThat(capturas).anyMatch(movimento -> movimento.quantidadeCapturada() == 2);
    }

    @Test
    void damaNuncaCapturaAMesmaPecaDuasVezes() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .allMatch(movimento -> movimento.quantidadeCapturada() == 1);
    }

    @Test
    void damaNaoCapturaQuandoNaoHaCasaLivreDepoisDaPeca() {
        // A preta esta na quina h8: o pouso cairia fora do tabuleiro.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("g7", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void damaCapturaNaBordaQuandoAQuinaEstaLivre() {
        // De f6 a dama toma g7 e pousa em h8, que existe e esta vazia.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("f6", Cor.BRANCA)
                .pedra("g7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.destino()).isEqualTo(Posicao.de("h8"));
                    assertThat(movimento.capturas()).containsExactly(Posicao.de("g7"));
                });
    }

    @Test
    void damaNaoPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .noneMatch(Movimento::promove);
    }

    @Test
    void gerarCapturasDeDamaNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("c3", Cor.PRETA)
                .pedra("c5", Cor.PRETA)
                .construir();

        GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(2);
        assertThat(tabuleiro.pecaEm(Posicao.de("a1"))).isPresent();
    }
}
