package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaTest {

    private static List<Posicao> caminho(String... notacoes) {
        return Arrays.stream(notacoes).map(Posicao::de).toList();
    }

    @Test
    void partidaNovaComecaComAsBrancasEDozePecasDeCadaLado() {
        Partida partida = Partida.nova();

        assertThat(partida.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(partida.estado()).isEqualTo(EstadoPartida.EM_ANDAMENTO);
        assertThat(partida.tabuleiro().contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(partida.historico()).isEmpty();
        assertThat(partida.id()).isNotNull();
    }

    @Test
    void jogarDevolveNovaPartidaSemAlterarAAnterior() {
        Partida inicial = Partida.nova();

        Partida depois = inicial.jogar(caminho("c3", "d4"));

        assertThat(inicial.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(inicial.tabuleiro().pecaEm(Posicao.de("c3"))).isPresent();
        assertThat(depois.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(depois.tabuleiro().pecaEm(Posicao.de("d4"))).isPresent();
        assertThat(depois.historico()).hasSize(1);
    }

    @Test
    void recusaMoverPecaDoAdversario() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("b6", "c5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PECA_DO_ADVERSARIO);
    }

    @Test
    void recusaMoverDeCasaVazia() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("d4", "e5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.SEM_PECA_NA_ORIGEM);
    }

    @Test
    void recusaMovimentoSimplesHavendoCapturaDisponivel() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .pedra("a1", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("a1", "b2")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAPTURA_OBRIGATORIA);
    }

    @Test
    void recusaCapturaQueNaoTomaOMaximo() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .pedra("h2", Cor.BRANCA)
                .pedra("g3", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("h2", "f4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.NAO_CAPTURA_O_MAXIMO);
    }

    @Test
    void recusaCaminhoQueNaoEDiagonal() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("c3", "c5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAMINHO_NAO_DIAGONAL);
    }

    @Test
    void recusaDestinoOcupado() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .dama("d4", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("c3", "d4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.DESTINO_OCUPADO);
    }

    @Test
    void recusaDamaMovidaParaAPropriaCasaSemEstourarOTabuleiro() {
        // Regressao: no prototipo, origem igual ao destino fazia a varredura da dama
        // caminhar para fora do tabuleiro e lancar ArrayIndexOutOfBoundsException.
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .dama("d4", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("d4", "d4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAMINHO_NAO_DIAGONAL);
    }

    @Test
    void aSequenciaDeCapturaInteiraEUmUnicoLanceEOTurnoPassaUmaVezSo() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(caminho("a1", "c3", "e5"));

        assertThat(depois.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(depois.historico()).hasSize(1);
        assertThat(depois.tabuleiro().contar(Cor.PRETA)).isEqualTo(1);
        assertThat(depois.placar()).containsEntry(Cor.BRANCA, 11);
    }

    @Test
    void capturaObrigatoriaEExpostaParaOCliente() {
        Partida semCaptura = Partida.nova();
        Partida comCaptura = Partida.de(TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThat(semCaptura.capturaObrigatoria()).isFalse();
        assertThat(comCaptura.capturaObrigatoria()).isTrue();
    }

    @Test
    void movimentosLegaisDeNaoAlteraAPartida() {
        Partida partida = Partida.nova();

        partida.movimentosLegaisDe(Posicao.de("c3"));

        assertThat(partida.tabuleiro().contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(partida.tabuleiro().contar(Cor.PRETA)).isEqualTo(12);
        assertThat(partida.historico()).isEmpty();
    }

    @Test
    void contadorDeLancesSemProgressoZeraEmCapturaOuMovimentoDePedra() {
        // A dama preta fica em h6, fora da diagonal a1-h8, para que nenhum dos lances
        // abaixo abra uma captura e torne o movimento simples ilegal.
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("a3", Cor.BRANCA)
                .dama("h6", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida aposDama = partida.jogar(caminho("a1", "b2"));
        assertThat(aposDama.lancesSemProgresso()).isEqualTo(1);

        Partida aposDuasDamas = aposDama.jogar(caminho("h6", "g5"));
        assertThat(aposDuasDamas.lancesSemProgresso()).isEqualTo(2);

        Partida aposPedra = aposDuasDamas.jogar(caminho("a3", "b4"));
        assertThat(aposPedra.lancesSemProgresso()).isZero();
    }

    @Test
    void partidaEncerradaRecusaNovoLance() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida encerrada = partida.jogar(caminho("c3", "e5"));

        assertThat(encerrada.estado()).isEqualTo(EstadoPartida.VITORIA_BRANCA);
        assertThatThrownBy(() -> encerrada.jogar(caminho("e5", "d6")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PARTIDA_ENCERRADA);
    }

    @Test
    void promoveAPedraQueTerminaNaUltimaLinha() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a7", Cor.BRANCA)
                .pedra("h2", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(caminho("a7", "b8"));

        assertThat(depois.tabuleiro().pecaEm(Posicao.de("b8"))).contains(Peca.dama(Cor.BRANCA));
    }
}
