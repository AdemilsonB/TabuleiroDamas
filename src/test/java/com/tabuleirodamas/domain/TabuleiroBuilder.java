package com.tabuleirodamas.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixture de teste: monta posicoes arbitrarias sem precisar jogar a partida inteira.
 * Sem isto, cada regra so poderia ser testada a partir da posicao inicial.
 */
public final class TabuleiroBuilder {

    private final Map<Posicao, Peca> casas = new HashMap<>();

    private TabuleiroBuilder() {
    }

    public static TabuleiroBuilder vazio() {
        return new TabuleiroBuilder();
    }

    public TabuleiroBuilder pedra(String notacao, Cor cor) {
        casas.put(Posicao.de(notacao), Peca.pedra(cor));
        return this;
    }

    public TabuleiroBuilder dama(String notacao, Cor cor) {
        casas.put(Posicao.de(notacao), Peca.dama(cor));
        return this;
    }

    public Tabuleiro construir() {
        return Tabuleiro.de(casas);
    }
}
