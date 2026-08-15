package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Movimento;

import java.util.List;

/**
 * Lei da maioria: havendo mais de uma sequencia de captura, o jogador e obrigado a
 * escolher uma das que capturam o maior numero de pecas.
 */
public final class LeiDaMaioria {

    private LeiDaMaioria() {
    }

    public static List<Movimento> filtrar(List<Movimento> capturas) {
        int maximo = capturas.stream()
                .mapToInt(Movimento::quantidadeCapturada)
                .max()
                .orElse(0);
        return capturas.stream()
                .filter(movimento -> movimento.quantidadeCapturada() == maximo)
                .toList();
    }
}
