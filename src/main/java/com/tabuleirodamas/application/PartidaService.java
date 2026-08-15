package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Orquestra o dominio e a persistencia. Nao contem regra de jogo nem I/O de console. */
@Service
public class PartidaService {

    private final PartidaRepository repositorio;

    public PartidaService(PartidaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Partida criar() {
        return repositorio.salvar(Partida.nova());
    }

    public Partida buscar(UUID id) {
        return repositorio.buscar(id).orElseThrow(() -> new PartidaNaoEncontradaException(id));
    }

    public List<Movimento> movimentosLegais(UUID id) {
        return buscar(id).movimentosLegais();
    }

    public List<Movimento> movimentosLegaisDe(UUID id, Posicao origem) {
        return buscar(id).movimentosLegaisDe(origem);
    }

    /** So grava quando o lance e legal: uma jogada recusada nao altera o estado. */
    public Partida jogar(UUID id, List<Posicao> caminho) {
        return repositorio.salvar(buscar(id).jogar(caminho));
    }

    public void remover(UUID id) {
        repositorio.remover(id);
    }
}
