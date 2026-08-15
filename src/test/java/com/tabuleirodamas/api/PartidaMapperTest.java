package com.tabuleirodamas.api;

import com.tabuleirodamas.api.dto.CasaDTO;
import com.tabuleirodamas.api.dto.JogadaRequest;
import com.tabuleirodamas.api.dto.PartidaDTO;
import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import com.tabuleirodamas.domain.TipoPeca;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartidaMapperTest {

    @Test
    void mapeiaPartidaNovaComVinteEQuatroCasasOcupadas() {
        PartidaDTO dto = PartidaMapper.dePartida(Partida.nova());

        assertThat(dto.casas()).hasSize(24);
        assertThat(dto.estado()).isEqualTo(EstadoPartida.EM_ANDAMENTO);
        assertThat(dto.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(dto.vencedor()).isNull();
        assertThat(dto.capturaObrigatoria()).isFalse();
        assertThat(dto.historico()).isEmpty();
        assertThat(dto.placar()).containsEntry(Cor.BRANCA, 0).containsEntry(Cor.PRETA, 0);
    }

    @Test
    void casaCarregaNotacaoEIndicesJuntos() {
        PartidaDTO dto = PartidaMapper.dePartida(Partida.nova());

        assertThat(dto.casas()).contains(new CasaDTO("b8", 0, 1, Cor.PRETA, TipoPeca.PEDRA));
        assertThat(dto.casas()).contains(new CasaDTO("a1", 7, 0, Cor.BRANCA, TipoPeca.PEDRA));
    }

    @Test
    void mapeiaSequenciaDeCapturaComCaminhoECapturasEmNotacao() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(List.of(
                Posicao.de("a1"), Posicao.de("c3"), Posicao.de("e5")));
        PartidaDTO dto = PartidaMapper.dePartida(depois);

        assertThat(dto.historico()).singleElement().satisfies(movimento -> {
            assertThat(movimento.caminho()).containsExactly("a1", "c3", "e5");
            assertThat(movimento.capturas()).containsExactlyInAnyOrder("b2", "d4");
        });
    }

    @Test
    void exponeVencedorQuandoAPartidaTermina() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir(), Cor.BRANCA);

        PartidaDTO dto = PartidaMapper.dePartida(
                partida.jogar(List.of(Posicao.de("c3"), Posicao.de("e5"))));

        assertThat(dto.estado()).isEqualTo(EstadoPartida.VITORIA_BRANCA);
        assertThat(dto.vencedor()).isEqualTo(Cor.BRANCA);
    }

    @Test
    void requisicaoDeJogadaConverteNotacaoEmPosicoes() {
        JogadaRequest requisicao = new JogadaRequest(List.of("b6", "d4", "f2"));

        assertThat(requisicao.paraPosicoes())
                .containsExactly(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2"));
    }
}
