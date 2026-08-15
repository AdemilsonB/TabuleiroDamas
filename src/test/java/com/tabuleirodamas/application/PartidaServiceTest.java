package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.MotivoIlegalidade;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.MovimentoIlegalException;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaServiceTest {

    private PartidaService servico;

    @BeforeEach
    void preparar() {
        servico = new PartidaService(new PartidaRepositoryEmMemoria());
    }

    @Test
    void criaPartidaERecuperaPeloId() {
        Partida criada = servico.criar();

        assertThat(servico.buscar(criada.id()).id()).isEqualTo(criada.id());
        assertThat(criada.vezDe()).isEqualTo(Cor.BRANCA);
    }

    @Test
    void buscarPartidaInexistenteFalha() {
        UUID inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> servico.buscar(inexistente))
                .isInstanceOf(PartidaNaoEncontradaException.class);
    }

    @Test
    void jogarPersisteONovoEstado() {
        Partida criada = servico.criar();

        servico.jogar(criada.id(), List.of(Posicao.de("c3"), Posicao.de("d4")));

        Partida recuperada = servico.buscar(criada.id());
        assertThat(recuperada.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(recuperada.historico()).hasSize(1);
    }

    @Test
    void jogadaIlegalNaoAlteraOEstadoGuardado() {
        Partida criada = servico.criar();

        assertThatThrownBy(() -> servico.jogar(criada.id(),
                List.of(Posicao.de("b6"), Posicao.de("c5"))))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PECA_DO_ADVERSARIO);

        assertThat(servico.buscar(criada.id()).vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(servico.buscar(criada.id()).historico()).isEmpty();
    }

    @Test
    void listaMovimentosLegaisDaVezEDeUmaOrigem() {
        Partida criada = servico.criar();

        assertThat(servico.movimentosLegais(criada.id())).hasSize(7);
        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("a3")))
                .extracting(Movimento::destino).containsExactly(Posicao.de("b4"));
    }

    @Test
    void consultarMovimentosDeCasaVaziaDevolveListaVaziaSemFalhar() {
        Partida criada = servico.criar();

        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("d4"))).isEmpty();
        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("b6"))).isEmpty();
    }

    @Test
    void removePartida() {
        Partida criada = servico.criar();

        servico.remover(criada.id());

        assertThatThrownBy(() -> servico.buscar(criada.id()))
                .isInstanceOf(PartidaNaoEncontradaException.class);
    }
}
