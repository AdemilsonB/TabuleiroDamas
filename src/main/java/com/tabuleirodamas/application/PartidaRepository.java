package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Partida;

import java.util.Optional;
import java.util.UUID;

/**
 * Guarda partidas. A implementacao em memoria basta para a demonstracao; trocar
 * por JPA nao exige tocar no dominio.
 */
public interface PartidaRepository {

    Partida salvar(Partida partida);

    Optional<Partida> buscar(UUID id);

    void remover(UUID id);
}
