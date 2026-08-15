package com.tabuleirodamas.domain;

/**
 * Peca do jogo. Nao guarda a propria posicao: o tabuleiro e a unica fonte de
 * verdade sobre onde ela esta, o que elimina o risco de dessincronizacao.
 */
public record Peca(Cor cor, TipoPeca tipo) {

    public static Peca pedra(Cor cor) {
        return new Peca(cor, TipoPeca.PEDRA);
    }

    public static Peca dama(Cor cor) {
        return new Peca(cor, TipoPeca.DAMA);
    }

    public boolean ehDama() {
        return tipo == TipoPeca.DAMA;
    }

    public Peca promovida() {
        return ehDama() ? this : dama(cor);
    }
}
