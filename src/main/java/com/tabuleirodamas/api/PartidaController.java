package com.tabuleirodamas.api;

import com.tabuleirodamas.api.dto.JogadaRequest;
import com.tabuleirodamas.api.dto.MovimentoDTO;
import com.tabuleirodamas.api.dto.PartidaDTO;
import com.tabuleirodamas.application.PartidaService;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    private final PartidaService servico;

    public PartidaController(PartidaService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PartidaDTO> criar(UriComponentsBuilder uri) {
        Partida partida = servico.criar();
        return ResponseEntity
                .created(uri.path("/api/partidas/{id}").build(partida.id()))
                .body(PartidaMapper.dePartida(partida));
    }

    @GetMapping("/{id}")
    public PartidaDTO buscar(@PathVariable UUID id) {
        return PartidaMapper.dePartida(servico.buscar(id));
    }

    /** Sem "origem", devolve todos os lances legais da vez. A consulta nunca recusa. */
    @GetMapping("/{id}/movimentos")
    public List<MovimentoDTO> movimentos(@PathVariable UUID id,
                                         @RequestParam(required = false) String origem) {
        List<Movimento> movimentos = origem == null
                ? servico.movimentosLegais(id)
                : servico.movimentosLegaisDe(id, Posicao.de(origem));
        return movimentos.stream().map(PartidaMapper::deMovimento).toList();
    }

    @PostMapping("/{id}/jogadas")
    public PartidaDTO jogar(@PathVariable UUID id, @Valid @RequestBody JogadaRequest requisicao) {
        return PartidaMapper.dePartida(servico.jogar(id, requisicao.paraPosicoes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        servico.remover(id);
        return ResponseEntity.noContent().build();
    }
}
