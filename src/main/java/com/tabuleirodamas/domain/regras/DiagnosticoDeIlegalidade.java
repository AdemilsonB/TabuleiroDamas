package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.MotivoIlegalidade;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Peca;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;

import java.util.List;

/**
 * Explica por que um caminho nao consta entre os lances legais. Existe para que o
 * cliente receba um motivo tipado em vez de uma recusa generica.
 */
public final class DiagnosticoDeIlegalidade {

    private DiagnosticoDeIlegalidade() {
    }

    public static MotivoIlegalidade motivo(Tabuleiro tabuleiro, Cor vezDe, List<Posicao> caminho) {
        Posicao origem = caminho.get(0);
        Peca peca = tabuleiro.pecaEm(origem).orElse(null);
        if (peca == null) {
            return MotivoIlegalidade.SEM_PECA_NA_ORIGEM;
        }
        if (peca.cor() != vezDe) {
            return MotivoIlegalidade.PECA_DO_ADVERSARIO;
        }
        if (!todosOsTrechosSaoDiagonais(caminho)) {
            return MotivoIlegalidade.CAMINHO_NAO_DIAGONAL;
        }
        if (tabuleiro.ocupada(caminho.get(caminho.size() - 1))) {
            return MotivoIlegalidade.DESTINO_OCUPADO;
        }

        List<Movimento> capturas = GeradorDeMovimentos.capturas(tabuleiro, vezDe);
        if (capturas.isEmpty()) {
            return MotivoIlegalidade.CAMINHO_INEXISTENTE;
        }
        boolean seriaCapturaValidaSemALei = capturas.stream()
                .anyMatch(movimento -> movimento.caminho().equals(caminho));
        return seriaCapturaValidaSemALei
                ? MotivoIlegalidade.NAO_CAPTURA_O_MAXIMO
                : MotivoIlegalidade.CAPTURA_OBRIGATORIA;
    }

    private static boolean todosOsTrechosSaoDiagonais(List<Posicao> caminho) {
        for (int i = 0; i < caminho.size() - 1; i++) {
            int deltaLinha = Math.abs(caminho.get(i + 1).linha() - caminho.get(i).linha());
            int deltaColuna = Math.abs(caminho.get(i + 1).coluna() - caminho.get(i).coluna());
            if (deltaLinha == 0 || deltaLinha != deltaColuna) {
                return false;
            }
        }
        return true;
    }
}
