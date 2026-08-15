# Backend de Damas Brasileiras — Especificação de Design

**Data:** 2026-08-15
**Estado:** Aprovado para implementação

## 1. Objetivo

Transformar o protótipo de console existente em um backend Java completo e testado,
que implemente as regras oficiais das damas brasileiras e exponha uma API REST pronta
para consumo por um front end Angular.

O projeto serve como demonstração técnica interna. Isso impõe dois critérios além do
funcionamento: o código precisa ser legível por outro desenvolvedor sem explicação
verbal, e cada regra do jogo precisa ter um teste que a comprove.

## 2. Diagnóstico do código atual

O protótipo acerta o algoritmo de varredura diagonal da dama e a intenção de captura
encadeada. Os problemas que motivam a reescrita do núcleo:

| # | Problema | Local |
|---|---|---|
| 1 | `Models.Tabuleiro` instancia `Controllers.TabuleiroService` — dependência circular entre camadas | `Models/Tabuleiro.java:12` |
| 2 | Validação de limites ocorre depois do acesso ao array — `ArrayIndexOutOfBoundsException` | `Controllers/TabuleiroService.java:134` vs `:165` |
| 3 | Dama retorna cedo e pula verificação de turno, destino ocupado e limites | `Controllers/TabuleiroService.java:146` |
| 4 | Captura por dama zera a casa direto no array e não pontua | `Controllers/TabuleiroService.java:111` |
| 5 | Dama movida para a própria casa percorre fora do tabuleiro | `Controllers/TabuleiroService.java:89` |
| 6 | `verificarAcaoCaptura` executa a captura dentro da validação — consultar altera estado | `Controllers/TabuleiroService.java:65` |
| 7 | `verificarCapturaPossivel` aplica varredura de dama a pedras — falso positivo concede jogada extra | `Controllers/TabuleiroService.java:197` |
| 8 | Condição de vitória testa o jogador errado; vitória por contagem de pontos | `Main.java:31` |
| 9 | Motivo da recusa só existe em `System.out` — inacessível a um cliente HTTP | disperso |
| 10 | Posição da peça duplicada entre índice do array e campos da `Peca` | `Models/Peca.java:6-7` |
| 11 | Sem captura obrigatória, sem lei da maioria, sem empate | ausente |
| 12 | Zero testes; `target/*.class` versionados; sem `.gitignore` | repositório |

O item 6 é o bloqueador estrutural: gerar a lista de movimentos legais — exatamente o
que o Angular precisa para destacar casas clicáveis — exige simular jogadas, e hoje
simular destrói a partida.

## 3. Decisões

| Decisão | Escolha | Motivo |
|---|---|---|
| Stack | Spring Boot 3 + Java 17 + Maven | JDK 17.0.8 e Maven 3.9.9 já instalados; Spring Boot 3 exige 17+ |
| Regras | Damas brasileiras oficiais | Jogo completo; demonstra domínio das regras |
| Transporte | REST puro | Suficiente para o front; sem custo de manutenção de WebSocket |
| Código existente | Núcleo de domínio reescrito | Elimina a dependência circular e o estado duplicado de uma vez |
| Persistência | Em memória (`ConcurrentHashMap`) atrás de uma interface | Demonstração não precisa de banco; troca por JPA fica aberta |
| Encoding | UTF-8 forçado no `maven-compiler-plugin` | Plataforma local é `Cp1252` e as mensagens têm acento |

## 4. Arquitetura

Dependências em uma direção só: `api → application → domain`. O domínio não importa
Spring nem faz I/O.

```
com.tabuleirodamas
├── DamasApplication
├── domain/
│   ├── Cor, TipoPeca, EstadoPartida, MotivoIlegalidade
│   ├── Posicao, Peca, Tabuleiro, Movimento, Partida
│   ├── MovimentoIlegalException
│   └── regras/ GeradorDeMovimentos, LeiDaMaioria, AvaliadorDeFimDeJogo
├── application/ PartidaService, PartidaRepository, PartidaRepositoryEmMemoria
├── api/ PartidaController, dto/, PartidaMapper, TratadorGlobalDeErros
└── config/ ConfiguracaoCors, ConfiguracaoOpenApi
```

### 4.1 Modelo de domínio

**`Posicao`** — record `(int linha, int coluna)`. Valida `0..7` na construção. Fábrica
`Posicao.de("b6")` e método `notacao()` para conversão com notação algébrica. Coluna 0
é `a`, linha 0 é a linha 8 do tabuleiro (topo, lado das pretas).

**`Peca`** — record `(Cor cor, TipoPeca tipo)`. **Não guarda posição.** Elimina o estado
duplicado do item 10. O tabuleiro é a única fonte de verdade sobre onde a peça está.

**`Tabuleiro`** — imutável. Internamente um `Map<Posicao, Peca>` não modificável.
`aplicar(Movimento)` devolve **um novo** `Tabuleiro`. Métodos de consulta:
`pecaEm(Posicao)`, `ocupada(Posicao)`, `posicoesDe(Cor)`, `contar(Cor)`.

**`Movimento`** — record `(List<Posicao> caminho, Set<Posicao> capturas, boolean promove)`.
Um movimento simples tem caminho de 2 elementos e capturas vazias. Uma sequência de
captura tem caminho de 3+ elementos.

**`Partida`** — raiz do agregado: `id`, `tabuleiro`, `vezDe`, `estado`, `historico`,
`lancesSemProgresso`, `placar`. Expõe `jogar(Movimento)` devolvendo uma nova `Partida`.

### 4.2 Geração de movimentos

`GeradorDeMovimentos.legais(Tabuleiro, Cor)` é uma função pura. Algoritmo:

1. Para cada peça da cor, gerar todas as sequências de captura por busca em
   profundidade.
2. Se existir ao menos uma captura, aplicar `LeiDaMaioria` — filtrar as sequências de
   cardinalidade máxima — e devolver só essas. Movimentos simples ficam ilegais.
3. Se não existir captura, gerar os movimentos simples.

Regras respeitadas na busca em profundidade:

- Peças capturadas **permanecem no tabuleiro como bloqueio** durante a sequência e são
  removidas apenas ao final. Uma peça não pode ser capturada duas vezes.
- Pedra captura para frente e para trás; move simples só para frente.
- Dama move e captura à distância pela diagonal (dama voadora), podendo pousar em
  qualquer casa vazia após a peça capturada, desde que o trecho esteja livre.
- Uma pedra que atinge a última linha **em trânsito** durante uma captura e ainda pode
  continuar capturando **não promove** e prossegue como pedra. Promove apenas se a
  sequência terminar ali.

Preserva o espírito da varredura diagonal do protótipo, agora sem efeito colateral.

### 4.3 Fim de jogo

`AvaliadorDeFimDeJogo` roda após cada lance:

- Adversário sem peças → vitória.
- Adversário sem movimentos legais (afogamento) → vitória.
- 20 lances de cada lado sem captura e sem movimento de pedra → empate.
  O contador zera a cada captura ou movimento de pedra.

Substitui a contagem de 12 pontos, que testava o jogador errado.

## 5. API REST

Formato de fio: notação algébrica. As respostas incluem também `linha`/`coluna` para
facilitar a renderização do grid no Angular.

| Método | Rota | Resposta |
|---|---|---|
| `POST` | `/api/partidas` | `201` + estado inicial + header `Location` |
| `GET` | `/api/partidas/{id}` | `200` estado completo |
| `GET` | `/api/partidas/{id}/movimentos` | `200` todos os lances legais da vez |
| `GET` | `/api/partidas/{id}/movimentos?origem=b6` | `200` lances legais daquela casa |
| `POST` | `/api/partidas/{id}/jogadas` | `200` estado após o lance |
| `DELETE` | `/api/partidas/{id}` | `204` |

Uma jogada é sempre um caminho completo, o que remove a ambiguidade de duas sequências
distintas terminando na mesma casa:

```json
{ "caminho": ["b6", "d4", "f2"] }
```

Um movimento simples é `{ "caminho": ["c3", "d4"] }`.

`GET /movimentos?origem=…` devolve `200` com lista vazia quando a casa não tem peça, tem
peça do adversário, ou tem peça sem lance legal — a consulta é informativa e não recusa.
Só `POST /jogadas` recusa.

Estado da partida:

```json
{
  "id": "0f7c…",
  "estado": "EM_ANDAMENTO",
  "vezDe": "BRANCA",
  "capturaObrigatoria": true,
  "placar": { "BRANCA": 3, "PRETA": 1 },
  "lancesSemProgresso": 4,
  "vencedor": null,
  "casas": [
    { "notacao": "b8", "linha": 0, "coluna": 1, "cor": "PRETA", "tipo": "PEDRA" }
  ],
  "historico": [ { "caminho": ["c3", "d4"], "capturas": [] } ]
}
```

`casas` lista apenas as casas ocupadas.

## 6. Tratamento de erros

Cada recusa carrega um `MotivoIlegalidade` tipado, substituindo os `System.out.println`
espalhados pelo domínio:

`SEM_PECA_NA_ORIGEM`, `PECA_DO_ADVERSARIO`, `DESTINO_OCUPADO`, `FORA_DO_TABULEIRO`,
`CAMINHO_NAO_DIAGONAL`, `CAPTURA_OBRIGATORIA`, `NAO_CAPTURA_O_MAXIMO`,
`CAMINHO_INEXISTENTE`, `PARTIDA_ENCERRADA`.

`TratadorGlobalDeErros` (`@RestControllerAdvice`) traduz em RFC 7807 `ProblemDetail`:

```json
{
  "status": 422,
  "title": "Movimento ilegal",
  "motivo": "CAPTURA_OBRIGATORIA",
  "detail": "Existe captura disponível; movimento simples não é permitido."
}
```

Mapeamento: `MovimentoIlegalException` → `422`; partida inexistente → `404`; corpo
malformado → `400`.

## 7. Testes

JUnit 5 + AssertJ. Fixture `TabuleiroBuilder` para montar posições arbitrárias sem
jogar a partida inteira — pré-requisito para testar regras isoladas.

**Domínio:**

- Posição inicial: 12 peças por cor, todas em casas escuras
- Pedra move uma diagonal para frente; recusa mover para trás sem captura
- Pedra captura para trás
- Captura obrigatória torna o movimento simples ilegal
- Lei da maioria: sequência de 3 capturas escolhida sobre a de 2
- Dama move e captura à distância
- Dama barrada por duas peças consecutivas na diagonal
- Peça não pode ser capturada duas vezes na mesma sequência
- Promoção ao terminar na última linha
- Não promove ao atravessar a última linha em trânsito de captura
- Vitória por ausência de peças
- Vitória por afogamento
- Empate por 20 lances sem progresso
- Mover peça do adversário é recusado
- Coordenada fora do tabuleiro é recusada sem lançar exceção não tratada
- Dama para a própria casa é recusada (regressão do item 5)
- Gerar movimentos não altera o tabuleiro (regressão do item 6)

**API:** `MockMvc` cobrindo criação, consulta, lances legais por origem, jogada válida,
jogada ilegal com `motivo` correto no corpo, partida inexistente.

## 8. Infraestrutura

- `.gitignore` para `target/`, `.idea/`, `*.class`; remoção do `target/` do versionamento
- `ConfiguracaoCors` liberando `http://localhost:4200`
- `springdoc-openapi` com Swagger UI em `/swagger-ui.html`, permitindo ao front gerar o
  client TypeScript a partir do contrato
- `maven-compiler-plugin` com `<encoding>UTF-8</encoding>`
- `README.md` com instruções de execução e a lista de endpoints
- `RunnerConsole` opcional, ativado por profile, preservando a demo de terminal sobre o
  novo domínio

## 9. Fora de escopo

WebSocket, oponente por IA, persistência em banco, autenticação, e o próprio front end
Angular. A interface `PartidaRepository` deixa a troca por JPA aberta sem tocar no
domínio.
