package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeCapturasDePedraTest {

    @Test
    void capturaPecaAdjacenteComCasaLivreAtras() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.destino()).isEqualTo(Posicao.de("e5"));
                    assertThat(movimento.capturas()).containsExactly(Posicao.de("d4"));
                });
    }

    @Test
    void pedraCapturaParaTras() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d2", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino).containsExactly(Posicao.de("e1"));
    }

    @Test
    void naoCapturaQuandoACasaAtrasEstaOcupada() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void naoCapturaPecaDaPropriaCor() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void naoCapturaQuandoOPousoCairiaForaDoTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("g7", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void encadeiaTresCapturasEmUmUnicoLance() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("a1", "c3", "e5", "g7");
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(3);
                });
    }

    @Test
    void registraApenasSequenciasMaximaisNaoOsPrefixos() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .construir();

        List<Movimento> capturas = GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(capturas).hasSize(1);
        assertThat(capturas.get(0).quantidadeCapturada()).isEqualTo(2);
    }

    @Test
    void pecaJaCapturadaBloqueiaENaoPodeSerTomadaDeNovo() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("f4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("c3", "e5", "g3");
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(2);
                });
    }

    @Test
    void ofereceAmbosOsRamosQuandoHaEscolha() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("b6"), Posicao.de("f6"));
    }

    @Test
    void pedraQueTerminaNaLinhaDePromocaoPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.destino()).isEqualTo(Posicao.de("d8"));
                    assertThat(movimento.promove()).isTrue();
                });
    }

    @Test
    void pedraQueAtravessaALinhaDePromocaoEmTransitoNaoPromove() {
        // b6 captura c7 caindo em d8 (linha de promocao), mas ainda pode capturar
        // e7 e cair em f6. Pela regra oficial ela continua pedra.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .pedra("e7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("b6", "d8", "f6");
                    assertThat(movimento.promove()).isFalse();
                });
    }

    @Test
    void gerarCapturasNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .construir();

        GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(2);
        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(1);
    }
}
