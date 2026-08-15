package com.tabuleirodamas.domain;

public enum Cor {

    BRANCA(-1, 0),
    PRETA(1, Posicao.TAMANHO - 1);

    private final int avanco;
    private final int linhaDePromocao;

    Cor(int avanco, int linhaDePromocao) {
        this.avanco = avanco;
        this.linhaDePromocao = linhaDePromocao;
    }

    /** Sentido em que a pedra desta cor avanca: -1 sobe no tabuleiro, +1 desce. */
    public int avanco() {
        return avanco;
    }

    /** Linha em que uma pedra desta cor vira dama. */
    public int linhaDePromocao() {
        return linhaDePromocao;
    }

    public Cor oposta() {
        return this == BRANCA ? PRETA : BRANCA;
    }
}
