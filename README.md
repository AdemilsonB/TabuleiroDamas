# Damas Brasileiras — Backend

Backend do jogo de damas brasileiras em Java, com API REST pronta para consumo por um
front end Angular.

## Regras implementadas

- Tabuleiro 8x8, 12 pedras por lado nas casas escuras
- Pedra avança uma casa na diagonal e captura para frente e para trás
- Dama voadora: move e captura à distância pela diagonal
- **Captura obrigatória**: havendo captura disponível, movimento simples é ilegal
- **Lei da maioria**: entre as capturas, só valem as de quantidade máxima
- Promoção ao terminar o lance na última linha; atravessá-la em trânsito não promove
- Peça já capturada bloqueia o caminho, mas não pode ser tomada duas vezes na sequência
- Vitória por ausência de peças ou por afogamento
- Empate após 20 lances de cada lado sem captura e sem movimento de pedra

## Requisitos

JDK 17 e Maven 3.9+.

## Executar

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa em
`http://localhost:8080/swagger-ui.html`, contrato em `/v3/api-docs` — use-o para gerar
o client TypeScript do Angular.

Demonstração em terminal:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=console
```

## Testes

```bash
mvn test
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/partidas` | Cria uma partida e devolve o estado inicial |
| `GET` | `/api/partidas/{id}` | Estado completo da partida |
| `GET` | `/api/partidas/{id}/movimentos` | Todos os lances legais da vez |
| `GET` | `/api/partidas/{id}/movimentos?origem=b6` | Lances legais a partir de uma casa |
| `POST` | `/api/partidas/{id}/jogadas` | Executa um lance |
| `DELETE` | `/api/partidas/{id}` | Remove a partida |

### Formato de casa

Notação algébrica: coluna `a`–`h`, linha `1`–`8`. As pretas ocupam as linhas 8, 7 e 6;
as brancas, as linhas 1, 2 e 3. As respostas trazem também os índices `linha`/`coluna`
(0 a 7, com `linha` 0 no topo) para facilitar a renderização do grid.

### Executar um lance

Uma jogada é sempre o caminho completo, porque duas sequências de captura distintas
podem terminar na mesma casa:

```bash
curl -X POST http://localhost:8080/api/partidas/{id}/jogadas \
  -H "Content-Type: application/json" \
  -d '{"caminho": ["c3", "d4"]}'
```

Sequência de captura múltipla:

```json
{ "caminho": ["b6", "d4", "f2"] }
```

### Estado da partida

```json
{
  "id": "0f7c…",
  "estado": "EM_ANDAMENTO",
  "vezDe": "BRANCA",
  "capturaObrigatoria": true,
  "placar": { "BRANCA": 3, "PRETA": 1 },
  "lancesSemProgresso": 4,
  "casas": [
    { "notacao": "b8", "linha": 0, "coluna": 1, "cor": "PRETA", "tipo": "PEDRA" }
  ],
  "historico": [ { "caminho": ["c3", "d4"], "capturas": [], "promove": false } ]
}
```

`casas` lista somente as casas ocupadas. `vencedor` só aparece quando a partida termina.

### Erros

Recusas seguem RFC 7807 e carregam um motivo tipado:

```json
{
  "status": 422,
  "title": "Movimento ilegal",
  "motivo": "CAPTURA_OBRIGATORIA",
  "detail": "Existe captura disponível; movimento simples não é permitido."
}
```

Motivos possíveis: `SEM_PECA_NA_ORIGEM`, `PECA_DO_ADVERSARIO`, `DESTINO_OCUPADO`,
`FORA_DO_TABULEIRO`, `CAMINHO_NAO_DIAGONAL`, `CAPTURA_OBRIGATORIA`,
`NAO_CAPTURA_O_MAXIMO`, `CAMINHO_INEXISTENTE`, `PARTIDA_ENCERRADA`.

## Integração com o Angular

CORS está liberado para `http://localhost:4200`. Para outras origens, defina
`damas.cors.origens` em `application.yml` ou via variável de ambiente.

Fluxo sugerido na tela: ao clicar numa peça, chame
`GET /api/partidas/{id}/movimentos?origem=<casa>` e destaque a última casa do `caminho`
de cada lance devolvido; ao clicar no destino, envie o `caminho` correspondente para
`POST /api/partidas/{id}/jogadas`. Como a geração de lances é uma consulta pura, o
front pode chamá-la à vontade sem alterar a partida.

## Arquitetura

```
com.tabuleirodamas
├── domain/       regras puras, imutáveis, sem Spring e sem I/O
│   └── regras/   geração de lances, lei da maioria, fim de jogo
├── application/  serviço e repositório
├── api/          controller, DTOs e tradução de erros
├── config/       CORS e OpenAPI
└── console/      demonstração em terminal (profile "console")
```

As dependências apontam numa direção só: `api → application → domain`. O tabuleiro é
imutável e `Tabuleiro.aplicar(Movimento)` devolve uma nova instância, então simular
sequências de captura para descobrir os lances legais nunca altera a partida em curso.

A peça não guarda a própria posição — o tabuleiro é a única fonte de verdade — e uma
sequência de captura inteira é um único `Movimento`, de modo que o turno passa
exatamente uma vez por jogada.

## Persistência

As partidas ficam em memória (`ConcurrentHashMap`) e se perdem ao reiniciar. A
interface `PartidaRepository` isola essa escolha: trocar por JPA não exige tocar no
domínio.
