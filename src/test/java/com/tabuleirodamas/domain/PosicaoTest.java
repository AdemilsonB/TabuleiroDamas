package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PosicaoTest {

    @Test
    void converteIndicesParaNotacaoAlgebrica() {
        assertThat(new Posicao(0, 1).notacao()).isEqualTo("b8");
        assertThat(new Posicao(7, 0).notacao()).isEqualTo("a1");
        assertThat(new Posicao(4, 7).notacao()).isEqualTo("h4");
    }

    @Test
    void converteNotacaoAlgebricaParaIndices() {
        assertThat(Posicao.de("b8")).isEqualTo(new Posicao(0, 1));
        assertThat(Posicao.de("a1")).isEqualTo(new Posicao(7, 0));
        assertThat(Posicao.de("H4")).isEqualTo(new Posicao(4, 7));
    }

    @Test
    void recusaCoordenadaForaDoTabuleiro() {
        assertThatThrownBy(() -> new Posicao(8, 0))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.FORA_DO_TABULEIRO);

        assertThatThrownBy(() -> new Posicao(0, -1))
                .isInstanceOf(MovimentoIlegalException.class);
    }

    @Test
    void recusaNotacaoMalformada() {
        assertThatThrownBy(() -> Posicao.de("z9")).isInstanceOf(MovimentoIlegalException.class);
        assertThatThrownBy(() -> Posicao.de("b")).isInstanceOf(MovimentoIlegalException.class);
        assertThatThrownBy(() -> Posicao.de(null)).isInstanceOf(MovimentoIlegalException.class);
    }

    @Test
    void deslocarDevolveVazioQuandoSaiDoTabuleiro() {
        assertThat(new Posicao(0, 0).deslocar(-1, 0)).isEmpty();
        assertThat(new Posicao(7, 7).deslocar(1, 1)).isEmpty();
        assertThat(new Posicao(4, 4).deslocar(-1, 1)).contains(new Posicao(3, 5));
    }

    @Test
    void existeNaoLancaParaCoordenadaInvalida() {
        assertThat(Posicao.existe(8, 0)).isFalse();
        assertThat(Posicao.existe(-1, 3)).isFalse();
        assertThat(Posicao.existe(3, 3)).isTrue();
    }

    @Test
    void apenasCasasEscurasSaoJogaveis() {
        assertThat(new Posicao(0, 1).jogavel()).isTrue();
        assertThat(new Posicao(0, 0).jogavel()).isFalse();
    }

    @Test
    void corConheceSentidoDeAvancoEPromocao() {
        assertThat(Cor.BRANCA.avanco()).isEqualTo(-1);
        assertThat(Cor.PRETA.avanco()).isEqualTo(1);
        assertThat(Cor.BRANCA.linhaDePromocao()).isZero();
        assertThat(Cor.PRETA.linhaDePromocao()).isEqualTo(7);
        assertThat(Cor.BRANCA.oposta()).isEqualTo(Cor.PRETA);
    }

    @Test
    void ehIdentificadaPorValor() {
        assertThat(new Posicao(2, 3)).isEqualTo(new Posicao(2, 3));
        assertThat(Optional.of(new Posicao(2, 3))).contains(Posicao.de("d6"));
    }
}
