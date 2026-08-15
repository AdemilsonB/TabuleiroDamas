package com.tabuleirodamas.api;

import com.tabuleirodamas.application.PartidaNaoEncontradaException;
import com.tabuleirodamas.domain.MovimentoIlegalException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as excecoes do dominio em RFC 7807. Substitui os System.out.println do
 * prototipo: o motivo da recusa agora chega ao cliente em formato legivel por maquina.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    @ExceptionHandler(MovimentoIlegalException.class)
    public ProblemDetail movimentoIlegal(MovimentoIlegalException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, excecao.motivo().mensagem());
        problema.setTitle("Movimento ilegal");
        problema.setProperty("motivo", excecao.motivo().name());
        return problema;
    }

    @ExceptionHandler(PartidaNaoEncontradaException.class)
    public ProblemDetail partidaNaoEncontrada(PartidaNaoEncontradaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Partida não encontrada");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail requisicaoInvalida(MethodArgumentNotValidException excecao) {
        String detalhe = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida.");
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Requisição inválida");
        return problema;
    }
}
