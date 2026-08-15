package com.tabuleirodamas.domain;

import com.tabuleirodamas.domain.regras.AvaliadorDeFimDeJogo;
import com.tabuleirodamas.domain.regras.DiagnosticoDeIlegalidade;
import com.tabuleirodamas.domain.regras.GeradorDeMovimentos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Raiz do agregado. Imutavel: cada lance devolve uma nova Partida.
 * Uma sequencia de captura inteira e um unico lance, de modo que o turno passa
 * exatamente uma vez por jogada.
 */
public final class Partida {

    private static final int PECAS_INICIAIS = 12;

    private final UUID id;
    private final Tabuleiro tabuleiro;
    private final Cor vezDe;
    private final EstadoPartida estado;
    private final List<Movimento> historico;
    private final int lancesSemProgresso;

    private Partida(UUID id, Tabuleiro tabuleiro, Cor vezDe, EstadoPartida estado,
                    List<Movimento> historico, int lancesSemProgresso) {
        this.id = id;
        this.tabuleiro = tabuleiro;
        this.vezDe = vezDe;
        this.estado = estado;
        this.historico = List.copyOf(historico);
        this.lancesSemProgresso = lancesSemProgresso;
    }

    public static Partida nova() {
        return new Partida(UUID.randomUUID(), Tabuleiro.inicial(), Cor.BRANCA,
                EstadoPartida.EM_ANDAMENTO, List.of(), 0);
    }

    /** Fabrica para testes e para retomar uma posicao arbitraria. */
    public static Partida de(Tabuleiro tabuleiro, Cor vezDe) {
        return new Partida(UUID.randomUUID(), tabuleiro, vezDe,
                AvaliadorDeFimDeJogo.avaliar(tabuleiro, vezDe, 0), List.of(), 0);
    }

    public Partida jogar(List<Posicao> caminho) {
        if (estado.encerrada()) {
            throw new MovimentoIlegalException(MotivoIlegalidade.PARTIDA_ENCERRADA);
        }
        Movimento escolhido = movimentosLegais().stream()
                .filter(movimento -> movimento.caminho().equals(caminho))
                .findFirst()
                .orElseThrow(() -> new MovimentoIlegalException(
                        DiagnosticoDeIlegalidade.motivo(tabuleiro, vezDe, caminho)));
        return aplicar(escolhido);
    }

    private Partida aplicar(Movimento movimento) {
        boolean progrediu = movimento.ehCaptura() || !pecaQueSeMove(movimento).ehDama();
        Tabuleiro proximoTabuleiro = tabuleiro.aplicar(movimento);
        Cor proximaVez = vezDe.oposta();
        int proximoContador = progrediu ? 0 : lancesSemProgresso + 1;

        List<Movimento> proximoHistorico = new ArrayList<>(historico);
        proximoHistorico.add(movimento);

        return new Partida(id, proximoTabuleiro, proximaVez,
                AvaliadorDeFimDeJogo.avaliar(proximoTabuleiro, proximaVez, proximoContador),
                proximoHistorico, proximoContador);
    }

    private Peca pecaQueSeMove(Movimento movimento) {
        return tabuleiro.pecaEm(movimento.origem())
                .orElseThrow(() -> new MovimentoIlegalException(MotivoIlegalidade.SEM_PECA_NA_ORIGEM));
    }

    public List<Movimento> movimentosLegais() {
        return estado.encerrada() ? List.of() : GeradorDeMovimentos.legais(tabuleiro, vezDe);
    }

    public List<Movimento> movimentosLegaisDe(Posicao origem) {
        return movimentosLegais().stream()
                .filter(movimento -> movimento.origem().equals(origem))
                .toList();
    }

    public boolean capturaObrigatoria() {
        return !estado.encerrada() && GeradorDeMovimentos.existeCaptura(tabuleiro, vezDe);
    }

    /** Pecas que cada cor ja capturou, derivado do tabuleiro. */
    public Map<Cor, Integer> placar() {
        return Map.of(
                Cor.BRANCA, PECAS_INICIAIS - tabuleiro.contar(Cor.PRETA),
                Cor.PRETA, PECAS_INICIAIS - tabuleiro.contar(Cor.BRANCA));
    }

    public UUID id() {
        return id;
    }

    public Tabuleiro tabuleiro() {
        return tabuleiro;
    }

    public Cor vezDe() {
        return vezDe;
    }

    public EstadoPartida estado() {
        return estado;
    }

    public List<Movimento> historico() {
        return historico;
    }

    public int lancesSemProgresso() {
        return lancesSemProgresso;
    }
}
