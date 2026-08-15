package com.tabuleirodamas.domain;

/** Razao pela qual um lance foi recusado, em formato legivel por maquina e por humano. */
public enum MotivoIlegalidade {

    SEM_PECA_NA_ORIGEM("Não há peça na casa de origem."),
    PECA_DO_ADVERSARIO("A peça pertence ao adversário."),
    DESTINO_OCUPADO("A casa de destino já está ocupada."),
    FORA_DO_TABULEIRO("Casa fora dos limites do tabuleiro."),
    CAMINHO_NAO_DIAGONAL("O movimento deve ser feito na diagonal."),
    CAPTURA_OBRIGATORIA("Existe captura disponível; movimento simples não é permitido."),
    NAO_CAPTURA_O_MAXIMO("Pela lei da maioria é obrigatório capturar o maior número de peças."),
    CAMINHO_INEXISTENTE("Não existe lance legal com esse caminho."),
    PARTIDA_ENCERRADA("A partida já está encerrada.");

    private final String mensagem;

    MotivoIlegalidade(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }
}
