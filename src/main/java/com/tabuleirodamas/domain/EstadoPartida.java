package com.tabuleirodamas.domain;

import java.util.Optional;

public enum EstadoPartida {

    EM_ANDAMENTO(null),
    VITORIA_BRANCA(Cor.BRANCA),
    VITORIA_PRETA(Cor.PRETA),
    EMPATE(null);

    private final Cor vencedor;

    EstadoPartida(Cor vencedor) {
        this.vencedor = vencedor;
    }

    public static EstadoPartida vitoriaDe(Cor cor) {
        return cor == Cor.BRANCA ? VITORIA_BRANCA : VITORIA_PRETA;
    }

    public boolean encerrada() {
        return this != EM_ANDAMENTO;
    }

    public Optional<Cor> vencedor() {
        return Optional.ofNullable(vencedor);
    }
}
