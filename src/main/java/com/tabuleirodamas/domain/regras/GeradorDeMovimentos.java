package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Peca;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gera os lances legais para uma cor. Funcao pura: nunca altera o tabuleiro recebido.
 */
public final class GeradorDeMovimentos {

    static final int[][] DIRECOES = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private GeradorDeMovimentos() {
    }

    /**
     * Lances legais da cor. Havendo captura disponivel, so capturas sao legais, e
     * entre elas apenas as de quantidade maxima (lei da maioria).
     */
    public static List<Movimento> legais(Tabuleiro tabuleiro, Cor cor) {
        List<Movimento> capturas = capturas(tabuleiro, cor);
        return capturas.isEmpty() ? simples(tabuleiro, cor) : LeiDaMaioria.filtrar(capturas);
    }

    public static List<Movimento> legaisDe(Tabuleiro tabuleiro, Cor cor, Posicao origem) {
        return legais(tabuleiro, cor).stream()
                .filter(movimento -> movimento.origem().equals(origem))
                .toList();
    }

    public static boolean existeCaptura(Tabuleiro tabuleiro, Cor cor) {
        return !capturas(tabuleiro, cor).isEmpty();
    }

    /** Lances sem captura. */
    public static List<Movimento> simples(Tabuleiro tabuleiro, Cor cor) {
        List<Movimento> movimentos = new ArrayList<>();
        for (Posicao origem : tabuleiro.posicoesDe(cor)) {
            Peca peca = tabuleiro.pecaEm(origem).orElseThrow();
            if (peca.ehDama()) {
                adicionarDeslizesDeDama(tabuleiro, origem, movimentos);
            } else {
                adicionarPassosDePedra(tabuleiro, origem, cor, movimentos);
            }
        }
        return movimentos;
    }

    private static void adicionarPassosDePedra(Tabuleiro tabuleiro, Posicao origem, Cor cor,
                                               List<Movimento> destino) {
        for (int lado : new int[]{-1, 1}) {
            origem.deslocar(cor.avanco(), lado)
                    .filter(tabuleiro::vazia)
                    .ifPresent(casa -> destino.add(
                            Movimento.simples(origem, casa, promovePedra(cor, casa))));
        }
    }

    private static void adicionarDeslizesDeDama(Tabuleiro tabuleiro, Posicao origem,
                                                List<Movimento> destino) {
        for (int[] direcao : DIRECOES) {
            Optional<Posicao> casa = origem.deslocar(direcao[0], direcao[1]);
            while (casa.isPresent() && tabuleiro.vazia(casa.get())) {
                destino.add(Movimento.simples(origem, casa.get(), false));
                casa = casa.get().deslocar(direcao[0], direcao[1]);
            }
        }
    }

    /** Todas as sequencias de captura maximais da cor, sem aplicar a lei da maioria. */
    public static List<Movimento> capturas(Tabuleiro tabuleiro, Cor cor) {
        List<Movimento> encontradas = new ArrayList<>();
        for (Posicao origem : tabuleiro.posicoesDe(cor)) {
            Peca peca = tabuleiro.pecaEm(origem).orElseThrow();
            // A casa de origem fica livre durante a sequencia: a peca esta em transito.
            Tabuleiro emTransito = tabuleiro.semPeca(origem);
            List<Posicao> caminho = new ArrayList<>();
            caminho.add(origem);
            aprofundar(emTransito, peca, origem, caminho, new LinkedHashSet<>(), encontradas);
        }
        return encontradas;
    }

    private static void aprofundar(Tabuleiro tabuleiro, Peca peca, Posicao atual,
                                   List<Posicao> caminho, Set<Posicao> capturadas,
                                   List<Movimento> encontradas) {
        boolean estendeu = false;
        for (int[] direcao : DIRECOES) {
            for (Salto salto : saltosPossiveis(tabuleiro, peca, atual, direcao, capturadas)) {
                estendeu = true;
                caminho.add(salto.destino());
                capturadas.add(salto.capturada());

                aprofundar(tabuleiro, peca, salto.destino(), caminho, capturadas, encontradas);

                capturadas.remove(salto.capturada());
                caminho.remove(caminho.size() - 1);
            }
        }
        // So a sequencia maximal e lance legal: parar no meio de uma captura nao vale.
        // Como o Movimento so nasce na folha, a pedra que apenas atravessa a linha de
        // promocao durante a sequencia nao promove.
        if (!estendeu && caminho.size() > 1) {
            boolean promove = !peca.ehDama() && promovePedra(peca.cor(), atual);
            encontradas.add(new Movimento(caminho, capturadas, promove));
        }
    }

    private static List<Salto> saltosPossiveis(Tabuleiro tabuleiro, Peca peca, Posicao atual,
                                               int[] direcao, Set<Posicao> capturadas) {
        return peca.ehDama()
                ? saltosDeDama(tabuleiro, peca, atual, direcao, capturadas)
                : saltoDePedra(tabuleiro, peca, atual, direcao, capturadas);
    }

    private static List<Salto> saltoDePedra(Tabuleiro tabuleiro, Peca peca, Posicao atual,
                                            int[] direcao, Set<Posicao> capturadas) {
        Optional<Posicao> vizinha = atual.deslocar(direcao[0], direcao[1]);
        if (vizinha.isEmpty() || !capturavel(tabuleiro, vizinha.get(), peca.cor(), capturadas)) {
            return List.of();
        }
        Optional<Posicao> pouso = vizinha.get().deslocar(direcao[0], direcao[1]);
        if (pouso.isEmpty() || tabuleiro.ocupada(pouso.get())) {
            return List.of();
        }
        return List.of(new Salto(vizinha.get(), pouso.get()));
    }

    /**
     * A dama percorre a diagonal, captura a primeira peca adversaria que encontra e
     * pode pousar em qualquer casa livre depois dela (dama voadora).
     */
    private static List<Salto> saltosDeDama(Tabuleiro tabuleiro, Peca peca, Posicao atual,
                                            int[] direcao, Set<Posicao> capturadas) {
        Optional<Posicao> casa = atual.deslocar(direcao[0], direcao[1]);
        while (casa.isPresent() && tabuleiro.vazia(casa.get())) {
            casa = casa.get().deslocar(direcao[0], direcao[1]);
        }
        if (casa.isEmpty() || !capturavel(tabuleiro, casa.get(), peca.cor(), capturadas)) {
            return List.of();
        }

        Posicao alvo = casa.get();
        List<Salto> saltos = new ArrayList<>();
        Optional<Posicao> pouso = alvo.deslocar(direcao[0], direcao[1]);
        while (pouso.isPresent() && tabuleiro.vazia(pouso.get())) {
            saltos.add(new Salto(alvo, pouso.get()));
            pouso = pouso.get().deslocar(direcao[0], direcao[1]);
        }
        return saltos;
    }

    /**
     * Uma peca ja capturada nesta sequencia continua no tabuleiro bloqueando o caminho,
     * mas nao pode ser tomada de novo.
     */
    private static boolean capturavel(Tabuleiro tabuleiro, Posicao posicao, Cor cor,
                                      Set<Posicao> capturadas) {
        return !capturadas.contains(posicao)
                && tabuleiro.pecaEm(posicao).filter(alvo -> alvo.cor() != cor).isPresent();
    }

    static boolean promovePedra(Cor cor, Posicao destino) {
        return destino.linha() == cor.linhaDePromocao();
    }

    private record Salto(Posicao capturada, Posicao destino) {
    }
}
