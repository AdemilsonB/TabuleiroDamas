package com.tabuleirodamas.application;

import java.util.UUID;

public class PartidaNaoEncontradaException extends RuntimeException {

    private final transient UUID id;

    public PartidaNaoEncontradaException(UUID id) {
        super("Partida não encontrada: " + id);
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
