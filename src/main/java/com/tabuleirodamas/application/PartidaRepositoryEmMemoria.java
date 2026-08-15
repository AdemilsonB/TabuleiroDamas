package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Partida;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PartidaRepositoryEmMemoria implements PartidaRepository {

    private final Map<UUID, Partida> partidas = new ConcurrentHashMap<>();

    @Override
    public Partida salvar(Partida partida) {
        partidas.put(partida.id(), partida);
        return partida;
    }

    @Override
    public Optional<Partida> buscar(UUID id) {
        return Optional.ofNullable(partidas.get(id));
    }

    @Override
    public void remover(UUID id) {
        partidas.remove(id);
    }
}
