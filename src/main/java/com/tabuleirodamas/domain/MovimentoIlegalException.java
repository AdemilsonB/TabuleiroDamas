package com.tabuleirodamas.domain;

/** Lancada quando um lance viola as regras. Carrega o motivo tipado ate a camada REST. */
public class MovimentoIlegalException extends RuntimeException {

    private final transient MotivoIlegalidade motivo;

    public MovimentoIlegalException(MotivoIlegalidade motivo) {
        super(motivo.mensagem());
        this.motivo = motivo;
    }

    public MotivoIlegalidade motivo() {
        return motivo;
    }
}
