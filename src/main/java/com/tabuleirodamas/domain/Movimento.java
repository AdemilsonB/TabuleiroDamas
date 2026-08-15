package com.tabuleirodamas.domain;

import java.util.List;
import java.util.Set;

/**
 * Um lance completo. Movimento simples tem caminho de duas casas e nenhuma captura;
 * uma sequencia de captura tem tres ou mais casas. O caminho completo e guardado
 * porque duas sequencias diferentes podem terminar na mesma casa.
 */
public record Movimento(List<Posicao> caminho, Set<Posicao> capturas, boolean promove) {

    public Movimento {
        if (caminho == null || caminho.size() < 2) {
            throw new IllegalArgumentException("O caminho precisa de ao menos duas casas.");
        }
        caminho = List.copyOf(caminho);
        capturas = Set.copyOf(capturas);
    }

    public static Movimento simples(Posicao origem, Posicao destino, boolean promove) {
        return new Movimento(List.of(origem, destino), Set.of(), promove);
    }

    public Posicao origem() {
        return caminho.get(0);
    }

    public Posicao destino() {
        return caminho.get(caminho.size() - 1);
    }

    public boolean ehCaptura() {
        return !capturas.isEmpty();
    }

    public int quantidadeCapturada() {
        return capturas.size();
    }

    public List<String> caminhoEmNotacao() {
        return caminho.stream().map(Posicao::notacao).toList();
    }
}
