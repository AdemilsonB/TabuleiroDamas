package com.tabuleirodamas.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tabuleiro imutavel. Toda operacao de alteracao devolve uma nova instancia,
 * o que permite simular sequencias de captura sem afetar a partida em curso.
 */
public final class Tabuleiro {

    public static final int TAMANHO = Posicao.TAMANHO;
    private static final int LINHAS_POR_LADO = 3;

    private final Map<Posicao, Peca> casas;

    private Tabuleiro(Map<Posicao, Peca> casas) {
        this.casas = Collections.unmodifiableMap(casas);
    }

    public static Tabuleiro de(Map<Posicao, Peca> casas) {
        return new Tabuleiro(new LinkedHashMap<>(casas));
    }

    /** Posicao de abertura: 12 pedras de cada cor nas casas escuras das tres primeiras linhas. */
    public static Tabuleiro inicial() {
        Map<Posicao, Peca> casas = new LinkedHashMap<>();
        for (int linha = 0; linha < TAMANHO; linha++) {
            Cor cor = corInicialDaLinha(linha);
            if (cor == null) {
                continue;
            }
            for (int coluna = 0; coluna < TAMANHO; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                if (posicao.jogavel()) {
                    casas.put(posicao, Peca.pedra(cor));
                }
            }
        }
        return new Tabuleiro(casas);
    }

    private static Cor corInicialDaLinha(int linha) {
        if (linha < LINHAS_POR_LADO) {
            return Cor.PRETA;
        }
        if (linha >= TAMANHO - LINHAS_POR_LADO) {
            return Cor.BRANCA;
        }
        return null;
    }

    public Optional<Peca> pecaEm(Posicao posicao) {
        return Optional.ofNullable(casas.get(posicao));
    }

    public boolean ocupada(Posicao posicao) {
        return casas.containsKey(posicao);
    }

    public boolean vazia(Posicao posicao) {
        return !ocupada(posicao);
    }

    public Set<Posicao> posicoesDe(Cor cor) {
        return casas.entrySet().stream()
                .filter(entrada -> entrada.getValue().cor() == cor)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public int contar(Cor cor) {
        return (int) casas.values().stream().filter(peca -> peca.cor() == cor).count();
    }

    public Map<Posicao, Peca> casas() {
        return casas;
    }

    public Tabuleiro comPeca(Posicao posicao, Peca peca) {
        Map<Posicao, Peca> novas = new HashMap<>(casas);
        novas.put(posicao, peca);
        return new Tabuleiro(novas);
    }

    public Tabuleiro semPeca(Posicao posicao) {
        Map<Posicao, Peca> novas = new HashMap<>(casas);
        novas.remove(posicao);
        return new Tabuleiro(novas);
    }

    /** Executa o lance e devolve o tabuleiro resultante. Nao altera esta instancia. */
    public Tabuleiro aplicar(Movimento movimento) {
        Peca peca = pecaEm(movimento.origem())
                .orElseThrow(() -> new MovimentoIlegalException(MotivoIlegalidade.SEM_PECA_NA_ORIGEM));

        Map<Posicao, Peca> novas = new HashMap<>(casas);
        novas.remove(movimento.origem());
        movimento.capturas().forEach(novas::remove);
        novas.put(movimento.destino(), movimento.promove() ? peca.promovida() : peca);
        return new Tabuleiro(novas);
    }
}
