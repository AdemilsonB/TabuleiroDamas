package com.tabuleirodamas.domain;

import java.util.Optional;

/**
 * Casa do tabuleiro. Linha 0 e o topo (lado das pretas), coluna 0 e a coluna "a".
 * Valida os limites na construcao, de modo que uma Posicao existente e sempre valida.
 */
public record Posicao(int linha, int coluna) {

    public static final int TAMANHO = 8;

    public Posicao {
        if (!existe(linha, coluna)) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
    }

    public static boolean existe(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    /** Converte notacao algebrica ("b6", sem distincao de caixa) em posicao. */
    public static Posicao de(String notacao) {
        if (notacao == null || notacao.length() != 2) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
        String normalizada = notacao.toLowerCase();
        int coluna = normalizada.charAt(0) - 'a';
        int rank = normalizada.charAt(1) - '0';
        if (!existe(TAMANHO - rank, coluna)) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
        return new Posicao(TAMANHO - rank, coluna);
    }

    /** Deslocamento relativo; vazio quando cairia fora do tabuleiro. */
    public Optional<Posicao> deslocar(int deltaLinha, int deltaColuna) {
        int novaLinha = linha + deltaLinha;
        int novaColuna = coluna + deltaColuna;
        return existe(novaLinha, novaColuna)
                ? Optional.of(new Posicao(novaLinha, novaColuna))
                : Optional.empty();
    }

    /** So as casas escuras participam do jogo. */
    public boolean jogavel() {
        return (linha + coluna) % 2 != 0;
    }

    public String notacao() {
        return String.valueOf((char) ('a' + coluna)) + (TAMANHO - linha);
    }

    @Override
    public String toString() {
        return notacao();
    }
}
