# Backend de Damas Brasileiras — Plano de Implementação

> Plano executado tarefa a tarefa em ciclo TDD. Os itens usam checkbox (`- [ ]`) para acompanhamento.

**Goal:** Substituir o protótipo de console por um backend Spring Boot testado que implementa as damas brasileiras oficiais e expõe uma API REST pronta para o front end Angular.

**Architecture:** Três camadas com dependência em direção única — `api → application → domain`. O domínio é puro (sem Spring, sem I/O) e imutável: `Tabuleiro.aplicar(Movimento)` devolve um novo tabuleiro, o que permite simular sequências de captura sem alterar a partida. A geração de movimentos legais é uma função pura sobre `(Tabuleiro, Cor)`.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Maven 3.9.9, JUnit 5, AssertJ, springdoc-openapi 2.6.0.

**Spec:** `docs/especificacao-backend.md`

## Global Constraints

- Java 17 (`maven.compiler.release` = `17`). JDK local é 17.0.8 — não usar recursos de 18+.
- Encoding UTF-8 explícito no `maven-compiler-plugin` e em `project.build.sourceEncoding`. A plataforma local é `Cp1252`; sem isso as mensagens acentuadas corrompem.
- Pacote raiz: `com.tabuleirodamas`. Nenhuma classe no default package.
- O pacote `domain` não pode importar nada de `org.springframework`, nem chamar `System.out`/`System.err`.
- `domain.Partida` usa `domain.regras.*` e `domain.regras.*` usa `domain.*`. Essa relação entre pacote e subpacote é deliberada: as regras são funções puras e sem estado, e a raiz do agregado precisa delas para garantir as próprias invariantes. Não confundir com o ciclo `Models ↔ Controllers` do protótipo, que atravessava camadas e carregava I/O.
- Nomenclatura de domínio em português (`Tabuleiro`, `Peca`, `Movimento`, `Partida`); nomes de pacote minúsculos.
- Mensagens de commit terminam no próprio texto, sem trailers de coautoria.
- Todo `Movimento` recusado carrega um `MotivoIlegalidade` tipado. Proibido sinalizar erro via `System.out`.
- Cada tarefa termina com os testes passando e um commit.

## Convenções de coordenada

`linha` 0 é o topo do tabuleiro (lado das PRETAS); `linha` 7 é a base (lado das BRANCAS).
`coluna` 0 é a coluna `a`. Notação algébrica: `notacao = (char)('a' + coluna) + (8 - linha)`.
Assim `(0,1)` = `"b8"` e `(7,0)` = `"a1"`. Casas jogáveis são as escuras, onde `(linha + coluna)` é ímpar.
BRANCA avança no sentido de `linha` decrescente e promove em `linha == 0`; PRETA avança no sentido crescente e promove em `linha == 7`.

---

### Task 1: Bootstrap do projeto Spring Boot

Remove o protótipo (preservado no histórico do git), sobe o `pom.xml` para Spring Boot 3 + Java 17, e para de versionar artefatos de build.

**Files:**
- Modify: `pom.xml`
- Create: `.gitignore`
- Create: `src/main/java/com/tabuleirodamas/DamasApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/tabuleirodamas/DamasApplicationTest.java`
- Delete: `src/main/java/Main.java`, `src/main/java/Controllers/`, `src/main/java/Models/`, `src/main/java/Enumeration/`, `target/` (do índice do git)

**Interfaces:**
- Consumes: nada
- Produces: `com.tabuleirodamas.DamasApplication` — classe `@SpringBootApplication` usada como âncora de contexto por todos os testes `@SpringBootTest`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/DamasApplicationTest.java`:

```java
package com.tabuleirodamas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DamasApplicationTest {

    @Test
    void contextoCarrega() {
        // Falha se o contexto Spring não subir.
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=DamasApplicationTest`
Expected: FAIL — o `pom.xml` atual não tem Spring Boot nem JUnit, então a compilação do teste quebra com `package org.junit.jupiter.api does not exist`.

- [ ] **Step 3: Write minimal implementation**

Substitua `pom.xml` inteiro por:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.tabuleirodamas</groupId>
    <artifactId>damas</artifactId>
    <version>1.0.0</version>
    <name>Damas</name>
    <description>Backend das damas brasileiras</description>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>-Dfile.encoding=UTF-8</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

`.gitignore`:

```gitignore
target/
!.mvn/wrapper/maven-wrapper.jar
*.class

.idea/
*.iml
*.iws
*.ipr
.vscode/

*.log
.DS_Store
Thumbs.db
```

`src/main/java/com/tabuleirodamas/DamasApplication.java`:

```java
package com.tabuleirodamas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DamasApplication {

    public static void main(String[] args) {
        SpringApplication.run(DamasApplication.class, args);
    }
}
```

`src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: damas
  jackson:
    default-property-inclusion: non_null

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

Remova o protótipo e os artefatos versionados:

```bash
git rm -r --cached target
rm -rf target
git rm -r src/main/java/Main.java src/main/java/Controllers src/main/java/Models src/main/java/Enumeration
git rm -r --cached .idea
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=DamasApplicationTest`
Expected: PASS. Se o Maven não resolver `3.3.5` ou `springdoc 2.6.0` no repositório local/proxy, suba para a versão 3.3.x mais recente disponível e ajuste `springdoc.version` para a 2.x compatível — não baixe para Spring Boot 2, que não roda em Java 17 com estas dependências.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Migra o projeto para Spring Boot 3 e Java 17

Substitui o prototipo de console por um esqueleto Spring Boot com
web, validation e springdoc. Forca encoding UTF-8 no compilador e no
surefire porque o encoding da plataforma local e Cp1252. Remove
target/ e .idea/ do versionamento."
```

---

### Task 2: Enums de base, exceção de domínio e `Posicao`

O vocabulário mínimo do domínio. `Posicao` valida os limites **na construção**, o que elimina de raiz o `ArrayIndexOutOfBoundsException` do protótipo (item 2 do diagnóstico).

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/Cor.java`
- Create: `src/main/java/com/tabuleirodamas/domain/TipoPeca.java`
- Create: `src/main/java/com/tabuleirodamas/domain/MotivoIlegalidade.java`
- Create: `src/main/java/com/tabuleirodamas/domain/MovimentoIlegalException.java`
- Create: `src/main/java/com/tabuleirodamas/domain/Posicao.java`
- Test: `src/test/java/com/tabuleirodamas/domain/PosicaoTest.java`

**Interfaces:**
- Consumes: nada
- Produces:
  - `enum Cor { BRANCA, PRETA }` com `Cor oposta()`, `int avanco()` (`-1` para BRANCA, `+1` para PRETA), `int linhaDePromocao()` (`0` para BRANCA, `7` para PRETA)
  - `enum TipoPeca { PEDRA, DAMA }`
  - `enum MotivoIlegalidade` com `String mensagem()`
  - `class MovimentoIlegalException extends RuntimeException` com `MotivoIlegalidade motivo()`
  - `record Posicao(int linha, int coluna)` com `static final int TAMANHO = 8`, `static boolean existe(int, int)`, `static Posicao de(String notacao)`, `static Posicao deIndices(int, int)`, `Optional<Posicao> deslocar(int dLinha, int dColuna)`, `String notacao()`, `boolean jogavel()`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/PosicaoTest.java`:

```java
package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PosicaoTest {

    @Test
    void converteIndicesParaNotacaoAlgebrica() {
        assertThat(new Posicao(0, 1).notacao()).isEqualTo("b8");
        assertThat(new Posicao(7, 0).notacao()).isEqualTo("a1");
        assertThat(new Posicao(4, 7).notacao()).isEqualTo("h4");
    }

    @Test
    void converteNotacaoAlgebricaParaIndices() {
        assertThat(Posicao.de("b8")).isEqualTo(new Posicao(0, 1));
        assertThat(Posicao.de("a1")).isEqualTo(new Posicao(7, 0));
        assertThat(Posicao.de("H4")).isEqualTo(new Posicao(4, 7));
    }

    @Test
    void recusaCoordenadaForaDoTabuleiro() {
        assertThatThrownBy(() -> new Posicao(8, 0))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.FORA_DO_TABULEIRO);

        assertThatThrownBy(() -> new Posicao(0, -1))
                .isInstanceOf(MovimentoIlegalException.class);
    }

    @Test
    void recusaNotacaoMalformada() {
        assertThatThrownBy(() -> Posicao.de("z9")).isInstanceOf(MovimentoIlegalException.class);
        assertThatThrownBy(() -> Posicao.de("b")).isInstanceOf(MovimentoIlegalException.class);
        assertThatThrownBy(() -> Posicao.de(null)).isInstanceOf(MovimentoIlegalException.class);
    }

    @Test
    void deslocarDevolveVazioQuandoSaiDoTabuleiro() {
        assertThat(new Posicao(0, 0).deslocar(-1, 0)).isEmpty();
        assertThat(new Posicao(7, 7).deslocar(1, 1)).isEmpty();
        assertThat(new Posicao(4, 4).deslocar(-1, 1)).contains(new Posicao(3, 5));
    }

    @Test
    void existeNaoLancaParaCoordenadaInvalida() {
        assertThat(Posicao.existe(8, 0)).isFalse();
        assertThat(Posicao.existe(-1, 3)).isFalse();
        assertThat(Posicao.existe(3, 3)).isTrue();
    }

    @Test
    void apenasCasasEscurasSaoJogaveis() {
        assertThat(new Posicao(0, 1).jogavel()).isTrue();
        assertThat(new Posicao(0, 0).jogavel()).isFalse();
    }

    @Test
    void corConheceSentidoDeAvancoEPromocao() {
        assertThat(Cor.BRANCA.avanco()).isEqualTo(-1);
        assertThat(Cor.PRETA.avanco()).isEqualTo(1);
        assertThat(Cor.BRANCA.linhaDePromocao()).isZero();
        assertThat(Cor.PRETA.linhaDePromocao()).isEqualTo(7);
        assertThat(Cor.BRANCA.oposta()).isEqualTo(Cor.PRETA);
    }

    @Test
    void ehIdentificadaPorValor() {
        assertThat(new Posicao(2, 3)).isEqualTo(new Posicao(2, 3));
        assertThat(Optional.of(new Posicao(2, 3))).contains(Posicao.de("d6"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PosicaoTest`
Expected: FAIL na compilação — `cannot find symbol: class Posicao`.

- [ ] **Step 3: Write minimal implementation**

`Cor.java`:

```java
package com.tabuleirodamas.domain;

public enum Cor {
    BRANCA(-1, 0),
    PRETA(1, Posicao.TAMANHO - 1);

    private final int avanco;
    private final int linhaDePromocao;

    Cor(int avanco, int linhaDePromocao) {
        this.avanco = avanco;
        this.linhaDePromocao = linhaDePromocao;
    }

    /** Sentido em que a pedra desta cor avança: -1 sobe no tabuleiro, +1 desce. */
    public int avanco() {
        return avanco;
    }

    /** Linha em que uma pedra desta cor vira dama. */
    public int linhaDePromocao() {
        return linhaDePromocao;
    }

    public Cor oposta() {
        return this == BRANCA ? PRETA : BRANCA;
    }
}
```

`TipoPeca.java`:

```java
package com.tabuleirodamas.domain;

public enum TipoPeca {
    PEDRA,
    DAMA
}
```

`MotivoIlegalidade.java`:

```java
package com.tabuleirodamas.domain;

/** Razão pela qual um lance foi recusado, em formato legível por máquina e por humano. */
public enum MotivoIlegalidade {

    SEM_PECA_NA_ORIGEM("Não há peça na casa de origem."),
    PECA_DO_ADVERSARIO("A peça pertence ao adversário."),
    DESTINO_OCUPADO("A casa de destino já está ocupada."),
    FORA_DO_TABULEIRO("Casa fora dos limites do tabuleiro."),
    CAMINHO_NAO_DIAGONAL("O movimento deve ser feito na diagonal."),
    CAPTURA_OBRIGATORIA("Existe captura disponível; movimento simples não é permitido."),
    NAO_CAPTURA_O_MAXIMO("Pela lei da maioria é obrigatório capturar o maior número de peças."),
    CAMINHO_INEXISTENTE("Não existe lance legal com esse caminho."),
    PARTIDA_ENCERRADA("A partida já está encerrada.");

    private final String mensagem;

    MotivoIlegalidade(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }
}
```

`MovimentoIlegalException.java`:

```java
package com.tabuleirodamas.domain;

/** Lançada quando um lance viola as regras. Carrega o motivo tipado até a camada REST. */
public class MovimentoIlegalException extends RuntimeException {

    private final transient MotivoIlegalidade motivo;

    public MovimentoIlegalException(MotivoIlegalidade motivo) {
        super(motivo.mensagem());
        this.motivo = motivo;
    }

    public MotivoIlegalidade motivo() {
        return motivo;
    }
}
```

`Posicao.java`:

```java
package com.tabuleirodamas.domain;

import java.util.Optional;

/**
 * Casa do tabuleiro. Linha 0 é o topo (lado das pretas), coluna 0 é a coluna "a".
 * Valida os limites na construção, de modo que uma Posicao existente é sempre válida.
 */
public record Posicao(int linha, int coluna) {

    public static final int TAMANHO = 8;

    public Posicao {
        if (!existe(linha, coluna)) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
    }

    public static boolean existe(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    public static Posicao deIndices(int linha, int coluna) {
        return new Posicao(linha, coluna);
    }

    /** Converte notação algébrica ("b6", case-insensitive) em posição. */
    public static Posicao de(String notacao) {
        if (notacao == null || notacao.length() != 2) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
        String normalizada = notacao.toLowerCase();
        int coluna = normalizada.charAt(0) - 'a';
        int rank = normalizada.charAt(1) - '0';
        if (!existe(TAMANHO - rank, coluna)) {
            throw new MovimentoIlegalException(MotivoIlegalidade.FORA_DO_TABULEIRO);
        }
        return new Posicao(TAMANHO - rank, coluna);
    }

    /** Deslocamento relativo; vazio quando cairia fora do tabuleiro. */
    public Optional<Posicao> deslocar(int dLinha, int dColuna) {
        int novaLinha = linha + dLinha;
        int novaColuna = coluna + dColuna;
        return existe(novaLinha, novaColuna)
                ? Optional.of(new Posicao(novaLinha, novaColuna))
                : Optional.empty();
    }

    /** Só as casas escuras participam do jogo. */
    public boolean jogavel() {
        return (linha + coluna) % 2 != 0;
    }

    public String notacao() {
        return String.valueOf((char) ('a' + coluna)) + (TAMANHO - linha);
    }

    @Override
    public String toString() {
        return notacao();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PosicaoTest`
Expected: PASS — 9 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain src/test/java/com/tabuleirodamas/domain
git commit -m "Adiciona vocabulario base do dominio

Cor, TipoPeca, MotivoIlegalidade, MovimentoIlegalException e Posicao.
Posicao valida os limites na construcao, de modo que uma instancia
existente e sempre valida - o prototipo acessava o array antes de
conferir os limites."
```

---

### Task 3: `Peca` e `Tabuleiro` imutável

O coração da correção arquitetural. A peça deixa de guardar a própria posição (item 10 do diagnóstico) e o tabuleiro passa a ser imutável, o que torna possível simular lances sem corromper a partida (item 6).

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/Peca.java`
- Create: `src/main/java/com/tabuleirodamas/domain/Tabuleiro.java`
- Create: `src/test/java/com/tabuleirodamas/domain/TabuleiroBuilder.java`
- Test: `src/test/java/com/tabuleirodamas/domain/TabuleiroTest.java`

**Interfaces:**
- Consumes: `Posicao`, `Cor`, `TipoPeca` (Task 2)
- Produces:
  - `record Peca(Cor cor, TipoPeca tipo)` com `static Peca pedra(Cor)`, `static Peca dama(Cor)`, `boolean ehDama()`, `Peca promovida()`
  - `final class Tabuleiro` com `static Tabuleiro inicial()`, `static Tabuleiro de(Map<Posicao, Peca>)`, `Optional<Peca> pecaEm(Posicao)`, `boolean ocupada(Posicao)`, `boolean vazia(Posicao)`, `Set<Posicao> posicoesDe(Cor)`, `int contar(Cor)`, `Map<Posicao, Peca> casas()`, `Tabuleiro comPeca(Posicao, Peca)`, `Tabuleiro semPeca(Posicao)`
  - `TabuleiroBuilder` (fixture de teste) com `static TabuleiroBuilder vazio()`, `pedra(String, Cor)`, `dama(String, Cor)`, `Tabuleiro construir()`

`Tabuleiro.aplicar(Movimento)` **não** entra aqui — depende de `Movimento`, que chega na Task 4.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/TabuleiroBuilder.java`:

```java
package com.tabuleirodamas.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixture de teste: monta posições arbitrárias sem precisar jogar a partida inteira.
 * Sem isto, cada regra só poderia ser testada a partir da posição inicial.
 */
public final class TabuleiroBuilder {

    private final Map<Posicao, Peca> casas = new HashMap<>();

    private TabuleiroBuilder() {
    }

    public static TabuleiroBuilder vazio() {
        return new TabuleiroBuilder();
    }

    public TabuleiroBuilder pedra(String notacao, Cor cor) {
        casas.put(Posicao.de(notacao), Peca.pedra(cor));
        return this;
    }

    public TabuleiroBuilder dama(String notacao, Cor cor) {
        casas.put(Posicao.de(notacao), Peca.dama(cor));
        return this;
    }

    public Tabuleiro construir() {
        return Tabuleiro.de(casas);
    }
}
```

`src/test/java/com/tabuleirodamas/domain/TabuleiroTest.java`:

```java
package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabuleiroTest {

    @Test
    void posicaoInicialTemDozePecasDeCadaCor() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(12);
    }

    @Test
    void posicaoInicialUsaSomenteCasasEscuras() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.casas().keySet()).allMatch(Posicao::jogavel);
    }

    @Test
    void posicaoInicialColocaPretasNoTopoEBrancasNaBase() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.pecaEm(Posicao.de("b8"))).contains(Peca.pedra(Cor.PRETA));
        assertThat(tabuleiro.pecaEm(Posicao.de("a1"))).contains(Peca.pedra(Cor.BRANCA));
        assertThat(tabuleiro.posicoesDe(Cor.PRETA)).allMatch(p -> p.linha() <= 2);
        assertThat(tabuleiro.posicoesDe(Cor.BRANCA)).allMatch(p -> p.linha() >= 5);
    }

    @Test
    void posicaoInicialDeixaAsDuasLinhasCentraisVazias() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.casas().keySet()).noneMatch(p -> p.linha() == 3 || p.linha() == 4);
    }

    @Test
    void todasAsPecasIniciaisSaoPedras() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThat(tabuleiro.casas().values()).noneMatch(Peca::ehDama);
    }

    @Test
    void comPecaDevolveNovoTabuleiroSemAlterarOOriginal() {
        Tabuleiro original = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro novo = original.comPeca(Posicao.de("d4"), Peca.dama(Cor.PRETA));

        assertThat(original.contar(Cor.PRETA)).isZero();
        assertThat(novo.contar(Cor.PRETA)).isEqualTo(1);
        assertThat(novo.pecaEm(Posicao.de("c3"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void semPecaDevolveNovoTabuleiroSemAlterarOOriginal() {
        Tabuleiro original = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro novo = original.semPeca(Posicao.de("c3"));

        assertThat(original.ocupada(Posicao.de("c3"))).isTrue();
        assertThat(novo.vazia(Posicao.de("c3"))).isTrue();
    }

    @Test
    void mapaDeCasasNaoPodeSerModificadoPeloChamador() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        assertThatThrownBy(() -> tabuleiro.casas().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pecaPromovidaViraDamaMantendoACor() {
        assertThat(Peca.pedra(Cor.BRANCA).promovida()).isEqualTo(Peca.dama(Cor.BRANCA));
        assertThat(Peca.dama(Cor.PRETA).promovida()).isEqualTo(Peca.dama(Cor.PRETA));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=TabuleiroTest`
Expected: FAIL na compilação — `cannot find symbol: class Peca`.

- [ ] **Step 3: Write minimal implementation**

`Peca.java`:

```java
package com.tabuleirodamas.domain;

/**
 * Peça do jogo. Não guarda a própria posição: o tabuleiro é a única fonte de
 * verdade sobre onde ela está, o que elimina o risco de dessincronização.
 */
public record Peca(Cor cor, TipoPeca tipo) {

    public static Peca pedra(Cor cor) {
        return new Peca(cor, TipoPeca.PEDRA);
    }

    public static Peca dama(Cor cor) {
        return new Peca(cor, TipoPeca.DAMA);
    }

    public boolean ehDama() {
        return tipo == TipoPeca.DAMA;
    }

    public Peca promovida() {
        return ehDama() ? this : dama(cor);
    }
}
```

`Tabuleiro.java`:

```java
package com.tabuleirodamas.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tabuleiro imutável. Toda operação de alteração devolve uma nova instância,
 * o que permite simular sequências de captura sem afetar a partida em curso.
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

    /** Posição de abertura: 12 pedras de cada cor nas casas escuras das três primeiras linhas. */
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
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
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
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=TabuleiroTest`
Expected: PASS — 9 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain src/test/java/com/tabuleirodamas/domain
git commit -m "Adiciona Peca e Tabuleiro imutavel

A peca deixa de guardar a propria posicao: o tabuleiro passa a ser a
unica fonte de verdade. Toda alteracao devolve nova instancia, o que
permite simular lances sem corromper a partida. Inclui TabuleiroBuilder
para montar posicoes arbitrarias nos testes."
```

---

### Task 4: `Movimento` e `Tabuleiro.aplicar`

Representa um lance completo — caminho, peças capturadas e se promove — e ensina o tabuleiro a executá-lo.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/Movimento.java`
- Modify: `src/main/java/com/tabuleirodamas/domain/Tabuleiro.java` (adicionar `aplicar`)
- Test: `src/test/java/com/tabuleirodamas/domain/MovimentoTest.java`

**Interfaces:**
- Consumes: `Posicao`, `Peca`, `Tabuleiro` (Tasks 2-3)
- Produces:
  - `record Movimento(List<Posicao> caminho, Set<Posicao> capturas, boolean promove)` com `static Movimento simples(Posicao, Posicao, boolean)`, `Posicao origem()`, `Posicao destino()`, `boolean ehCaptura()`, `int quantidadeCapturada()`, `List<String> caminhoEmNotacao()`
  - `Tabuleiro aplicar(Movimento)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/MovimentoTest.java`:

```java
package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimentoTest {

    @Test
    void movimentoSimplesTemCaminhoDeDoisPontosESemCapturas() {
        Movimento movimento = Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false);

        assertThat(movimento.origem()).isEqualTo(Posicao.de("c3"));
        assertThat(movimento.destino()).isEqualTo(Posicao.de("d4"));
        assertThat(movimento.ehCaptura()).isFalse();
        assertThat(movimento.quantidadeCapturada()).isZero();
    }

    @Test
    void sequenciaDeCapturaConheceOrigemDestinoEQuantidade() {
        Movimento movimento = new Movimento(
                List.of(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2")),
                Set.of(Posicao.de("c5"), Posicao.de("e3")),
                false);

        assertThat(movimento.origem()).isEqualTo(Posicao.de("b6"));
        assertThat(movimento.destino()).isEqualTo(Posicao.de("f2"));
        assertThat(movimento.ehCaptura()).isTrue();
        assertThat(movimento.quantidadeCapturada()).isEqualTo(2);
        assertThat(movimento.caminhoEmNotacao()).containsExactly("b6", "d4", "f2");
    }

    @Test
    void recusaCaminhoComMenosDeDuasCasas() {
        assertThatThrownBy(() -> new Movimento(List.of(Posicao.de("c3")), Set.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caminhoECapturasSaoImutaveis() {
        Movimento movimento = Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false);

        assertThatThrownBy(() -> movimento.caminho().add(Posicao.de("e5")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aplicarMoveAPecaDaOrigemParaODestino() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("c3", Cor.BRANCA).construir();

        Tabuleiro depois = tabuleiro.aplicar(
                Movimento.simples(Posicao.de("c3"), Posicao.de("d4"), false));

        assertThat(depois.vazia(Posicao.de("c3"))).isTrue();
        assertThat(depois.pecaEm(Posicao.de("d4"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void aplicarRemoveTodasAsPecasCapturadas() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e3", Cor.PRETA)
                .construir();

        Tabuleiro depois = tabuleiro.aplicar(new Movimento(
                List.of(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2")),
                Set.of(Posicao.de("c5"), Posicao.de("e3")),
                false));

        assertThat(depois.contar(Cor.PRETA)).isZero();
        assertThat(depois.pecaEm(Posicao.de("f2"))).contains(Peca.pedra(Cor.BRANCA));
    }

    @Test
    void aplicarPromoveQuandoOMovimentoIndica() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("b2", Cor.BRANCA).construir();

        Tabuleiro depois = tabuleiro.aplicar(
                Movimento.simples(Posicao.de("b2"), Posicao.de("a1"), true));

        assertThat(depois.pecaEm(Posicao.de("a1"))).contains(Peca.dama(Cor.BRANCA));
    }

    @Test
    void aplicarNaoAlteraOTabuleiroOriginal() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        tabuleiro.aplicar(new Movimento(
                List.of(Posicao.de("c3"), Posicao.de("e5")),
                Set.of(Posicao.de("d4")),
                false));

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(1);
        assertThat(tabuleiro.pecaEm(Posicao.de("c3"))).contains(Peca.pedra(Cor.BRANCA));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=MovimentoTest`
Expected: FAIL na compilação — `cannot find symbol: class Movimento`.

- [ ] **Step 3: Write minimal implementation**

`Movimento.java`:

```java
package com.tabuleirodamas.domain;

import java.util.List;
import java.util.Set;

/**
 * Um lance completo. Movimento simples tem caminho de duas casas e nenhuma captura;
 * uma sequência de captura tem três ou mais casas. O caminho completo é guardado
 * porque duas sequências diferentes podem terminar na mesma casa.
 */
public record Movimento(List<Posicao> caminho, Set<Posicao> capturas, boolean promove) {

    public Movimento {
        if (caminho == null || caminho.size() < 2) {
            throw new IllegalArgumentException("O caminho precisa de ao menos duas casas.");
        }
        caminho = List.copyOf(caminho);
        capturas = Set.copyOf(capturas);
    }

    public static Movimento simples(Posicao origem, Posicao destino, boolean promove) {
        return new Movimento(List.of(origem, destino), Set.of(), promove);
    }

    public Posicao origem() {
        return caminho.get(0);
    }

    public Posicao destino() {
        return caminho.get(caminho.size() - 1);
    }

    public boolean ehCaptura() {
        return !capturas.isEmpty();
    }

    public int quantidadeCapturada() {
        return capturas.size();
    }

    public List<String> caminhoEmNotacao() {
        return caminho.stream().map(Posicao::notacao).toList();
    }
}
```

Acrescente a `Tabuleiro.java`, logo após `semPeca`:

```java
    /** Executa o lance e devolve o tabuleiro resultante. Não altera esta instância. */
    public Tabuleiro aplicar(Movimento movimento) {
        Peca peca = pecaEm(movimento.origem())
                .orElseThrow(() -> new MovimentoIlegalException(MotivoIlegalidade.SEM_PECA_NA_ORIGEM));

        Map<Posicao, Peca> novas = new HashMap<>(casas);
        novas.remove(movimento.origem());
        movimento.capturas().forEach(novas::remove);
        novas.put(movimento.destino(), movimento.promove() ? peca.promovida() : peca);
        return new Tabuleiro(novas);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=MovimentoTest`
Expected: PASS — 8 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain src/test/java/com/tabuleirodamas/domain
git commit -m "Adiciona Movimento e Tabuleiro.aplicar

O lance guarda o caminho completo, e nao apenas origem e destino,
porque duas sequencias de captura distintas podem terminar na mesma
casa. Aplicar devolve um novo tabuleiro."
```

---


### Task 5: Gerador de movimentos simples

Primeira metade do gerador: lances sem captura. Pedra anda uma casa na diagonal para frente; dama desliza pela diagonal.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentos.java`
- Test: `src/test/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentosSimplesTest.java`

**Interfaces:**
- Consumes: `Posicao`, `Peca`, `Cor`, `Tabuleiro`, `Movimento` (Tasks 2-4)
- Produces:
  - `final class GeradorDeMovimentos` (construtor privado, métodos estáticos)
  - `static List<Movimento> simples(Tabuleiro, Cor)`
  - `static final int[][] DIRECOES = {{-1,-1},{-1,1},{1,-1},{1,1}}`
  - `static boolean promovePedra(Cor, Posicao)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentosSimplesTest.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeMovimentosSimplesTest {

    @Test
    void pedraBrancaAvancaUmaDiagonalParaCima() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("c5"), Posicao.de("e5"));
    }

    @Test
    void pedraPretaAvancaUmaDiagonalParaBaixo() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.PRETA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.PRETA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("c3"), Posicao.de("e3"));
    }

    @Test
    void pedraNaoAndaParaTrasSemCaptura() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .doesNotContain(Posicao.de("c3"), Posicao.de("e3"));
    }

    @Test
    void pedraNaoAndaParaCasaOcupada() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::origem).doesNotContain(Posicao.de("d4"));
    }

    @Test
    void pedraQueChegaNaUltimaLinhaPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("b8", Cor.PRETA).construir();
        Tabuleiro brancaQuaseLa = TabuleiroBuilder.vazio().pedra("b2", Cor.PRETA).construir();

        assertThat(GeradorDeMovimentos.simples(brancaQuaseLa, Cor.PRETA))
                .isNotEmpty()
                .allMatch(Movimento::promove);
        assertThat(GeradorDeMovimentos.simples(tabuleiro, Cor.PRETA))
                .noneMatch(Movimento::promove);
    }

    @Test
    void pedraBrancaPromoveAoAlcancarALinhaOito() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("a7", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .allMatch(Movimento::promove);
    }

    @Test
    void damaDeslizaPelaDiagonalInteira() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a1", Cor.BRANCA).construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(
                        Posicao.de("b2"), Posicao.de("c3"), Posicao.de("d4"),
                        Posicao.de("e5"), Posicao.de("f6"), Posicao.de("g7"), Posicao.de("h8"));
    }

    @Test
    void damaParaAntesDeQualquerPeca() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("b2"), Posicao.de("c3"));
    }

    @Test
    void damaNuncaPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a7", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA)).noneMatch(Movimento::promove);
    }

    @Test
    void geraSomenteMovimentosDaCorPedida() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a7", Cor.PRETA)
                .construir();

        List<Movimento> movimentos = GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(movimentos).extracting(Movimento::origem).containsOnly(Posicao.de("d4"));
    }

    @Test
    void gerarNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = Tabuleiro.inicial();

        GeradorDeMovimentos.simples(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(12);
    }

    @Test
    void posicaoInicialTemSeteAberturasParaCadaLado() {
        assertThat(GeradorDeMovimentos.simples(Tabuleiro.inicial(), Cor.BRANCA)).hasSize(7);
        assertThat(GeradorDeMovimentos.simples(Tabuleiro.inicial(), Cor.PRETA)).hasSize(7);
    }
}
```

Conferência de `posicaoInicialTemSeteAberturasParaCadaLado`: as brancas móveis são as da linha 5 (`a3`, `c3`, `e3`, `g3`); `a3` só alcança `b4`, as outras três alcançam duas casas cada — total 7. As linhas 6 e 7 estão bloqueadas pelas próprias peças.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=GeradorDeMovimentosSimplesTest`
Expected: FAIL na compilação — `cannot find symbol: class GeradorDeMovimentos`.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentos.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Peca;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gera os lances legais para uma cor. Funcao pura: nunca altera o tabuleiro recebido.
 */
public final class GeradorDeMovimentos {

    static final int[][] DIRECOES = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private GeradorDeMovimentos() {
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

    static boolean promovePedra(Cor cor, Posicao destino) {
        return destino.linha() == cor.linhaDePromocao();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=GeradorDeMovimentosSimplesTest`
Expected: PASS — 12 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain/regras src/test/java/com/tabuleirodamas/domain/regras
git commit -m "Gera movimentos simples de pedra e dama

Pedra anda uma diagonal no sentido de avanco da cor; dama desliza pela
diagonal ate encontrar peca. A geracao e uma funcao pura sobre o
tabuleiro."
```

---

### Task 6: Capturas de pedra em sequência

Busca em profundidade sobre a árvore de capturas. Só sequências maximais viram lance — parar no meio de uma captura possível é ilegal nas regras brasileiras.

**Files:**
- Modify: `src/main/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentos.java`
- Test: `src/test/java/com/tabuleirodamas/domain/regras/GeradorDeCapturasDePedraTest.java`

**Interfaces:**
- Consumes: tudo da Task 5
- Produces: `static List<Movimento> capturas(Tabuleiro, Cor)` — todas as sequências de captura maximais, ainda sem o filtro da lei da maioria (Task 8)

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/regras/GeradorDeCapturasDePedraTest.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeCapturasDePedraTest {

    @Test
    void capturaPecaAdjacenteComCasaLivreAtras() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.destino()).isEqualTo(Posicao.de("e5"));
                    assertThat(movimento.capturas()).containsExactly(Posicao.de("d4"));
                });
    }

    @Test
    void pedraCapturaParaTras() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d2", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino).containsExactly(Posicao.de("e1"));
    }

    @Test
    void naoCapturaQuandoACasaAtrasEstaOcupada() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void naoCapturaPecaDaPropriaCor() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void naoCapturaQuandoOPousoCairiaForaDoTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("g7", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void encadeiaTresCapturasEmUmUnicoLance() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("a1", "c3", "e5", "g7");
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(3);
                });
    }

    @Test
    void registraApenasSequenciasMaximaisNaoOsPrefixos() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .construir();

        List<Movimento> capturas = GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(capturas).hasSize(1);
        assertThat(capturas.get(0).quantidadeCapturada()).isEqualTo(2);
    }

    @Test
    void pecaJaCapturadaBloqueiaENaoPodeSerTomadaDeNovo() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("f4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("c3", "e5", "g3");
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(2);
                });
    }

    @Test
    void ofereceAmbosOsRamosQuandoHaEscolha() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino)
                .containsExactlyInAnyOrder(Posicao.de("b6"), Posicao.de("f6"));
    }

    @Test
    void pedraQueTerminaNaLinhaDePromocaoPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.destino()).isEqualTo(Posicao.de("d8"));
                    assertThat(movimento.promove()).isTrue();
                });
    }

    @Test
    void pedraQueAtravessaALinhaDePromocaoEmTransitoNaoPromove() {
        // b6 captura c7 caindo em d8 (linha de promocao), mas ainda pode capturar
        // e7 e cair em f6. Pela regra oficial ela continua pedra.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .pedra("e7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.caminhoEmNotacao()).containsExactly("b6", "d8", "f6");
                    assertThat(movimento.promove()).isFalse();
                });
    }

    @Test
    void gerarCapturasNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .construir();

        GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(2);
        assertThat(tabuleiro.contar(Cor.BRANCA)).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=GeradorDeCapturasDePedraTest`
Expected: FAIL na compilação — `cannot find symbol: method capturas(Tabuleiro,Cor)`.

- [ ] **Step 3: Write minimal implementation**

Acrescente os imports `java.util.LinkedHashSet` e `java.util.Set` a `GeradorDeMovimentos.java`, e os métodos abaixo:

```java
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

    /** Preenchido na Task 7. */
    private static List<Salto> saltosDeDama(Tabuleiro tabuleiro, Peca peca, Posicao atual,
                                            int[] direcao, Set<Posicao> capturadas) {
        return List.of();
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

    private record Salto(Posicao capturada, Posicao destino) {
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=GeradorDeCapturasDePedraTest`
Expected: PASS — 12 testes verdes.

Se `pecaJaCapturadaBloqueiaENaoPodeSerTomadaDeNovo` falhar, confirme que `aprofundar` recebe o tabuleiro `emTransito` (com as capturadas ainda presentes) e nunca um tabuleiro já limpo.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain/regras src/test/java/com/tabuleirodamas/domain/regras
git commit -m "Gera sequencias de captura de pedra em profundidade

A pedra captura para frente e para tras, encadeando saltos ate nao
haver mais captura possivel. Pecas ja capturadas seguem no tabuleiro
como bloqueio e nao podem ser tomadas duas vezes. So sequencias
maximais viram lance legal, e a pedra que apenas atravessa a linha de
promocao em transito nao vira dama."
```

---

### Task 7: Capturas de dama voadora

Preenche `saltosDeDama`: a dama percorre a diagonal, captura a primeira peça adversária e pode pousar em qualquer casa livre depois dela.

**Files:**
- Modify: `src/main/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentos.java`
- Test: `src/test/java/com/tabuleirodamas/domain/regras/GeradorDeCapturasDeDamaTest.java`

**Interfaces:**
- Consumes: tudo das Tasks 5-6
- Produces: nenhuma assinatura nova — completa o comportamento de `capturas(Tabuleiro, Cor)` para damas

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/regras/GeradorDeCapturasDeDamaTest.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorDeCapturasDeDamaTest {

    @Test
    void damaCapturaADistanciaEPousaEmQualquerCasaLivreDepois() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .extracting(Movimento::destino)
                .containsExactlyInAnyOrder(
                        Posicao.de("e5"), Posicao.de("f6"), Posicao.de("g7"), Posicao.de("h8"));
    }

    @Test
    void damaEBarradaPorDuasPecasConsecutivas() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void damaNaoPulaPecaDaPropriaCor() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("d4", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA)).isEmpty();
    }

    @Test
    void damaEncadeiaCapturasMudandoDeDirecao() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("c3", Cor.PRETA)
                .pedra("c5", Cor.PRETA)
                .construir();

        List<Movimento> capturas = GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(capturas).extracting(Movimento::caminhoEmNotacao)
                .contains(List.of("a1", "d4", "b6"), List.of("a1", "d4", "a7"));
        assertThat(capturas).anyMatch(movimento -> movimento.quantidadeCapturada() == 2);
    }

    @Test
    void damaNuncaCapturaAMesmaPecaDuasVezes() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .allMatch(movimento -> movimento.quantidadeCapturada() == 1);
    }

    @Test
    void damaNaoCapturaQuandoNaoHaCasaLivreDepoisDaPeca() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("f6", Cor.BRANCA)
                .pedra("g7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .noneMatch(movimento -> movimento.capturas().contains(Posicao.de("g7")));
    }

    @Test
    void damaNaoPromove() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("b6", Cor.BRANCA)
                .pedra("c7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA))
                .isNotEmpty()
                .noneMatch(Movimento::promove);
    }

    @Test
    void gerarCapturasDeDamaNaoAlteraOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("c3", Cor.PRETA)
                .pedra("c5", Cor.PRETA)
                .construir();

        GeradorDeMovimentos.capturas(tabuleiro, Cor.BRANCA);

        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(2);
        assertThat(tabuleiro.pecaEm(Posicao.de("a1"))).isPresent();
    }
}
```

Conferência de `damaCapturaADistanciaEPousaEmQualquerCasaLivreDepois`: de `a1` a dama desliza por `b2` e `c3` vazias, encontra a preta em `d4` e pode pousar em `e5`, `f6`, `g7` ou `h8`. Em nenhum desses pousos há continuação — a única peça restante já foi capturada e bloqueia —, então são quatro lances de uma captura.

Conferência de `damaEncadeiaCapturasMudandoDeDirecao`: de `a1` a dama captura `c3` e pode pousar em `d4`, `e5`, `f6`, `g7` ou `h8`. Só de `d4` existe continuação: na direção contrária ela alcança `c5` e pousa em `b6` ou `a7`. Os demais pousos são folhas de uma captura, e a lei da maioria (Task 8) descarta todos eles.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=GeradorDeCapturasDeDamaTest`
Expected: FAIL — `saltosDeDama` devolve lista vazia, então `damaCapturaADistanciaEPousaEmQualquerCasaLivreDepois` falha com "expected 4 elements but was empty".

- [ ] **Step 3: Write minimal implementation**

Substitua o esqueleto de `saltosDeDama` em `GeradorDeMovimentos.java` por:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=GeradorDeCapturasDeDamaTest`
Expected: PASS — 8 testes verdes.

Rode também a suíte inteira para garantir que a dama não quebrou as pedras:
Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain/regras src/test/java/com/tabuleirodamas/domain/regras
git commit -m "Implementa captura da dama voadora

A dama percorre a diagonal, captura a primeira peca adversaria e pousa
em qualquer casa livre apos ela, encadeando saltos e mudando de direcao.
Substitui a varredura do prototipo, que alterava o tabuleiro durante a
validacao e nao contabilizava a captura."
```

---


### Task 8: Captura obrigatória e lei da maioria

Junta as duas metades do gerador. Se existe captura, movimento simples é ilegal; entre as capturas, só valem as de quantidade máxima.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/regras/LeiDaMaioria.java`
- Modify: `src/main/java/com/tabuleirodamas/domain/regras/GeradorDeMovimentos.java`
- Test: `src/test/java/com/tabuleirodamas/domain/regras/MovimentosLegaisTest.java`

**Interfaces:**
- Consumes: `GeradorDeMovimentos.simples`, `GeradorDeMovimentos.capturas` (Tasks 5-7)
- Produces:
  - `final class LeiDaMaioria` com `static List<Movimento> filtrar(List<Movimento>)`
  - `static List<Movimento> GeradorDeMovimentos.legais(Tabuleiro, Cor)`
  - `static List<Movimento> GeradorDeMovimentos.legaisDe(Tabuleiro, Cor, Posicao)`
  - `static boolean GeradorDeMovimentos.existeCaptura(Tabuleiro, Cor)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/regras/MovimentosLegaisTest.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentosLegaisTest {

    @Test
    void semCapturaDisponivelDevolveOsMovimentosSimples() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA))
                .hasSize(2)
                .noneMatch(Movimento::ehCaptura);
    }

    @Test
    void havendoCapturaOMovimentoSimplesDeixaDeSerLegal() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .pedra("a1", Cor.BRANCA)
                .construir();

        List<Movimento> legais = GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA);

        assertThat(legais).singleElement().satisfies(movimento -> {
            assertThat(movimento.ehCaptura()).isTrue();
            assertThat(movimento.destino()).isEqualTo(Posicao.de("f6"));
        });
        assertThat(legais).extracting(Movimento::origem).doesNotContain(Posicao.de("a1"));
    }

    @Test
    void leiDaMaioriaDescartaAsCapturasMenores() {
        // a1 encadeia tres capturas; h2 so captura uma. So a de tres e legal.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .pedra("h2", Cor.BRANCA)
                .pedra("g3", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA)).singleElement()
                .satisfies(movimento -> {
                    assertThat(movimento.quantidadeCapturada()).isEqualTo(3);
                    assertThat(movimento.origem()).isEqualTo(Posicao.de("a1"));
                });
    }

    @Test
    void leiDaMaioriaMantemEmpatesDeQuantidade() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("c5", Cor.PRETA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legais(tabuleiro, Cor.BRANCA))
                .hasSize(2)
                .allMatch(movimento -> movimento.quantidadeCapturada() == 1);
    }

    @Test
    void legaisDeFiltraPelaCasaDeOrigem() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a1", Cor.BRANCA)
                .construir();

        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("a1")))
                .extracting(Movimento::destino).containsExactly(Posicao.de("b2"));
    }

    @Test
    void legaisDeDevolveVazioParaCasaVaziaOuPecaAdversaria() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("a7", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("h8"))).isEmpty();
        assertThat(GeradorDeMovimentos.legaisDe(tabuleiro, Cor.BRANCA, Posicao.de("a7"))).isEmpty();
    }

    @Test
    void existeCapturaRespondeSemAlterarOTabuleiro() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir();

        assertThat(GeradorDeMovimentos.existeCaptura(tabuleiro, Cor.BRANCA)).isTrue();
        assertThat(GeradorDeMovimentos.existeCaptura(tabuleiro, Cor.PRETA)).isTrue();
        assertThat(tabuleiro.contar(Cor.PRETA)).isEqualTo(1);
    }

    @Test
    void posicaoInicialNaoTemCapturaDisponivel() {
        assertThat(GeradorDeMovimentos.existeCaptura(Tabuleiro.inicial(), Cor.BRANCA)).isFalse();
        assertThat(GeradorDeMovimentos.legais(Tabuleiro.inicial(), Cor.BRANCA)).hasSize(7);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=MovimentosLegaisTest`
Expected: FAIL na compilação — `cannot find symbol: method legais(Tabuleiro,Cor)`.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/tabuleirodamas/domain/regras/LeiDaMaioria.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Movimento;

import java.util.List;

/**
 * Lei da maioria: havendo mais de uma sequencia de captura, o jogador e obrigado a
 * escolher uma das que capturam o maior numero de pecas.
 */
public final class LeiDaMaioria {

    private LeiDaMaioria() {
    }

    public static List<Movimento> filtrar(List<Movimento> capturas) {
        int maximo = capturas.stream()
                .mapToInt(Movimento::quantidadeCapturada)
                .max()
                .orElse(0);
        return capturas.stream()
                .filter(movimento -> movimento.quantidadeCapturada() == maximo)
                .toList();
    }
}
```

Acrescente a `GeradorDeMovimentos.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=MovimentosLegaisTest`
Expected: PASS — 8 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain/regras src/test/java/com/tabuleirodamas/domain/regras
git commit -m "Aplica captura obrigatoria e lei da maioria

Havendo captura disponivel o movimento simples deixa de ser legal, e
entre as capturas so valem as de quantidade maxima. Ambas as regras
estavam ausentes no prototipo."
```

---

### Task 9: Avaliação de fim de jogo

Substitui a contagem de 12 pontos do protótipo, que testava o jogador errado e ignorava afogamento e empate.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/EstadoPartida.java`
- Create: `src/main/java/com/tabuleirodamas/domain/regras/AvaliadorDeFimDeJogo.java`
- Test: `src/test/java/com/tabuleirodamas/domain/regras/AvaliadorDeFimDeJogoTest.java`

**Interfaces:**
- Consumes: `Tabuleiro`, `Cor`, `GeradorDeMovimentos.legais` (Tasks 3, 8)
- Produces:
  - `enum EstadoPartida { EM_ANDAMENTO, VITORIA_BRANCA, VITORIA_PRETA, EMPATE }` com `boolean encerrada()` e `Optional<Cor> vencedor()`
  - `final class AvaliadorDeFimDeJogo` com `static final int LIMITE_LANCES_SEM_PROGRESSO = 40` e `static EstadoPartida avaliar(Tabuleiro, Cor proximoAJogar, int lancesSemProgresso)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/regras/AvaliadorDeFimDeJogoTest.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Tabuleiro;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliadorDeFimDeJogoTest {

    @Test
    void partidaSegueEmAndamentoQuandoHaLancesDisponiveis() {
        assertThat(AvaliadorDeFimDeJogo.avaliar(Tabuleiro.inicial(), Cor.BRANCA, 0))
                .isEqualTo(EstadoPartida.EM_ANDAMENTO);
    }

    @Test
    void vencePorAusenciaDePecasDoAdversario() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().pedra("d4", Cor.BRANCA).construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA, 0))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void vencePorAfogamentoQuandoOAdversarioNaoTemLanceLegal() {
        // A preta em a7 avanca para a linha 6: b6 esta ocupada por branca e a casa de
        // pouso da captura (c5) tambem, entao ela nao tem lance legal.
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .pedra("a7", Cor.PRETA)
                .pedra("b6", Cor.BRANCA)
                .pedra("c5", Cor.BRANCA)
                .dama("h8", Cor.BRANCA)
                .construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA, 0))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void empataAoAtingirOLimiteDeLancesSemProgresso() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .dama("h8", Cor.PRETA)
                .construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.BRANCA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO))
                .isEqualTo(EstadoPartida.EMPATE);
        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.BRANCA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO - 1))
                .isEqualTo(EstadoPartida.EM_ANDAMENTO);
    }

    @Test
    void vitoriaTemPrecedenciaSobreEmpate() {
        Tabuleiro tabuleiro = TabuleiroBuilder.vazio().dama("a1", Cor.BRANCA).construir();

        assertThat(AvaliadorDeFimDeJogo.avaliar(tabuleiro, Cor.PRETA,
                AvaliadorDeFimDeJogo.LIMITE_LANCES_SEM_PROGRESSO))
                .isEqualTo(EstadoPartida.VITORIA_BRANCA);
    }

    @Test
    void estadoConheceOVencedorESeEstaEncerrado() {
        assertThat(EstadoPartida.EM_ANDAMENTO.encerrada()).isFalse();
        assertThat(EstadoPartida.EM_ANDAMENTO.vencedor()).isEmpty();
        assertThat(EstadoPartida.VITORIA_BRANCA.encerrada()).isTrue();
        assertThat(EstadoPartida.VITORIA_BRANCA.vencedor()).contains(Cor.BRANCA);
        assertThat(EstadoPartida.VITORIA_PRETA.vencedor()).contains(Cor.PRETA);
        assertThat(EstadoPartida.EMPATE.encerrada()).isTrue();
        assertThat(EstadoPartida.EMPATE.vencedor()).isEqualTo(Optional.<Cor>empty());
    }
}
```

Conferência do afogamento: `a7` é `(1,0)`. A preta avança no sentido `+1`; os destinos seriam `(2,-1)` (fora) e `(2,1)` = `b6`, ocupada. Como captura, `b6` é adversária e o pouso seria `c5` = `(3,2)`, também ocupada. Para trás, `(0,1)` = `b8` está vazia mas pedra não anda para trás, e não há peça em `(0,1)` para capturar. Logo a preta não tem lance legal.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=AvaliadorDeFimDeJogoTest`
Expected: FAIL na compilação — `cannot find symbol: class EstadoPartida`.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/tabuleirodamas/domain/EstadoPartida.java`:

```java
package com.tabuleirodamas.domain;

import java.util.Optional;

public enum EstadoPartida {

    EM_ANDAMENTO(null),
    VITORIA_BRANCA(Cor.BRANCA),
    VITORIA_PRETA(Cor.PRETA),
    EMPATE(null);

    private final Cor vencedor;

    EstadoPartida(Cor vencedor) {
        this.vencedor = vencedor;
    }

    public static EstadoPartida vitoriaDe(Cor cor) {
        return cor == Cor.BRANCA ? VITORIA_BRANCA : VITORIA_PRETA;
    }

    public boolean encerrada() {
        return this != EM_ANDAMENTO;
    }

    public Optional<Cor> vencedor() {
        return Optional.ofNullable(vencedor);
    }
}
```

`src/main/java/com/tabuleirodamas/domain/regras/AvaliadorDeFimDeJogo.java`:

```java
package com.tabuleirodamas.domain.regras;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Tabuleiro;

/**
 * Decide se a partida terminou. Substitui a contagem de pontos do prototipo, que
 * media o jogador errado e nao enxergava afogamento nem empate.
 */
public final class AvaliadorDeFimDeJogo {

    /** 20 lances de cada lado sem captura e sem movimento de pedra. */
    public static final int LIMITE_LANCES_SEM_PROGRESSO = 40;

    private AvaliadorDeFimDeJogo() {
    }

    public static EstadoPartida avaliar(Tabuleiro tabuleiro, Cor proximoAJogar,
                                        int lancesSemProgresso) {
        boolean semPecas = tabuleiro.contar(proximoAJogar) == 0;
        boolean afogado = GeradorDeMovimentos.legais(tabuleiro, proximoAJogar).isEmpty();

        if (semPecas || afogado) {
            return EstadoPartida.vitoriaDe(proximoAJogar.oposta());
        }
        if (lancesSemProgresso >= LIMITE_LANCES_SEM_PROGRESSO) {
            return EstadoPartida.EMPATE;
        }
        return EstadoPartida.EM_ANDAMENTO;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=AvaliadorDeFimDeJogoTest`
Expected: PASS — 6 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain src/test/java/com/tabuleirodamas/domain
git commit -m "Avalia fim de jogo por ausencia de pecas, afogamento e empate

Vitoria quando o adversario fica sem pecas ou sem lance legal; empate
apos 20 lances de cada lado sem captura e sem movimento de pedra."
```

---

### Task 10: Agregado `Partida`

Junta tabuleiro, turno, histórico e estado. Uma sequência de captura inteira é **um** lance, o que elimina o turno extra que o protótipo concedia.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/domain/Partida.java`
- Create: `src/main/java/com/tabuleirodamas/domain/regras/DiagnosticoDeIlegalidade.java`
- Test: `src/test/java/com/tabuleirodamas/domain/PartidaTest.java`

**Interfaces:**
- Consumes: `Tabuleiro`, `Movimento`, `EstadoPartida`, `GeradorDeMovimentos`, `AvaliadorDeFimDeJogo` (Tasks 3-9)
- Produces:
  - `final class Partida` com `static Partida nova()`, `UUID id()`, `Tabuleiro tabuleiro()`, `Cor vezDe()`, `EstadoPartida estado()`, `List<Movimento> historico()`, `int lancesSemProgresso()`, `Map<Cor, Integer> placar()`, `List<Movimento> movimentosLegais()`, `List<Movimento> movimentosLegaisDe(Posicao)`, `boolean capturaObrigatoria()`, `Partida jogar(List<Posicao>)`
  - `final class DiagnosticoDeIlegalidade` com `static MotivoIlegalidade motivo(Tabuleiro, Cor, List<Posicao>)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/domain/PartidaTest.java`:

```java
package com.tabuleirodamas.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaTest {

    private static List<Posicao> caminho(String... notacoes) {
        return java.util.Arrays.stream(notacoes).map(Posicao::de).toList();
    }

    @Test
    void partidaNovaComecaComAsBrancasEDozePecasDeCadaLado() {
        Partida partida = Partida.nova();

        assertThat(partida.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(partida.estado()).isEqualTo(EstadoPartida.EM_ANDAMENTO);
        assertThat(partida.tabuleiro().contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(partida.historico()).isEmpty();
        assertThat(partida.id()).isNotNull();
    }

    @Test
    void jogarDevolveNovaPartidaSemAlterarAAnterior() {
        Partida inicial = Partida.nova();

        Partida depois = inicial.jogar(caminho("c3", "d4"));

        assertThat(inicial.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(inicial.tabuleiro().pecaEm(Posicao.de("c3"))).isPresent();
        assertThat(depois.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(depois.tabuleiro().pecaEm(Posicao.de("d4"))).isPresent();
        assertThat(depois.historico()).hasSize(1);
    }

    @Test
    void recusaMoverPecaDoAdversario() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("b6", "c5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PECA_DO_ADVERSARIO);
    }

    @Test
    void recusaMoverDeCasaVazia() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("d4", "e5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.SEM_PECA_NA_ORIGEM);
    }

    @Test
    void recusaMovimentoSimplesHavendoCapturaDisponivel() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .pedra("a1", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("a1", "b2")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAPTURA_OBRIGATORIA);
    }

    @Test
    void recusaCapturaQueNaoTomaOMaximo() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("f6", Cor.PRETA)
                .pedra("h2", Cor.BRANCA)
                .pedra("g3", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("h2", "f4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.NAO_CAPTURA_O_MAXIMO);
    }

    @Test
    void recusaCaminhoQueNaoEDiagonal() {
        Partida partida = Partida.nova();

        assertThatThrownBy(() -> partida.jogar(caminho("c3", "c5")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAMINHO_NAO_DIAGONAL);
    }

    @Test
    void recusaDestinoOcupado() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .dama("d4", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("c3", "d4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.DESTINO_OCUPADO);
    }

    @Test
    void aSequenciaDeCapturaInteiraEUmUnicoLanceEOTurnoPassaUmaVezSo() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(caminho("a1", "c3", "e5"));

        assertThat(depois.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(depois.historico()).hasSize(1);
        assertThat(depois.tabuleiro().contar(Cor.PRETA)).isEqualTo(1);
        assertThat(depois.placar()).containsEntry(Cor.BRANCA, 11);
    }

    @Test
    void capturaObrigatoriaEExpostaParaOCliente() {
        Partida semCaptura = Partida.nova();
        Partida comCaptura = Partida.de(TabuleiroBuilder.vazio()
                .pedra("d4", Cor.BRANCA)
                .pedra("e5", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThat(semCaptura.capturaObrigatoria()).isFalse();
        assertThat(comCaptura.capturaObrigatoria()).isTrue();
    }

    @Test
    void movimentosLegaisDeNaoAlteraAPartida() {
        Partida partida = Partida.nova();

        partida.movimentosLegaisDe(Posicao.de("c3"));

        assertThat(partida.tabuleiro().contar(Cor.BRANCA)).isEqualTo(12);
        assertThat(partida.tabuleiro().contar(Cor.PRETA)).isEqualTo(12);
        assertThat(partida.historico()).isEmpty();
    }

    @Test
    void contadorDeLancesSemProgressoZeraEmCapturaOuMovimentoDePedra() {
        // A dama preta fica em h6, fora da diagonal a1-h8, para que nenhum dos lances
        // abaixo abra uma captura e torne o movimento simples ilegal.
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .dama("a1", Cor.BRANCA)
                .pedra("a3", Cor.BRANCA)
                .dama("h6", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida aposDama = partida.jogar(caminho("a1", "b2"));
        assertThat(aposDama.lancesSemProgresso()).isEqualTo(1);

        Partida aposDuasDamas = aposDama.jogar(caminho("h6", "g5"));
        assertThat(aposDuasDamas.lancesSemProgresso()).isEqualTo(2);

        Partida aposPedra = aposDuasDamas.jogar(caminho("a3", "b4"));
        assertThat(aposPedra.lancesSemProgresso()).isZero();
    }

    @Test
    void partidaEncerradaRecusaNovoLance() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida encerrada = partida.jogar(caminho("c3", "e5"));

        assertThat(encerrada.estado()).isEqualTo(EstadoPartida.VITORIA_BRANCA);
        assertThatThrownBy(() -> encerrada.jogar(caminho("e5", "d6")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PARTIDA_ENCERRADA);
    }

    @Test
    void recusaDamaMovidaParaAPropriaCasaSemEstourarOTabuleiro() {
        // Regressao: no prototipo, origem igual ao destino fazia a varredura da dama
        // caminhar para fora do tabuleiro e lancar ArrayIndexOutOfBoundsException.
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .dama("d4", Cor.BRANCA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        assertThatThrownBy(() -> partida.jogar(caminho("d4", "d4")))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.CAMINHO_NAO_DIAGONAL);
    }

    @Test
    void promoveAPedraQueTerminaNaUltimaLinha() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a7", Cor.BRANCA)
                .pedra("h2", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(caminho("a7", "b8"));

        assertThat(depois.tabuleiro().pecaEm(Posicao.de("b8"))).contains(Peca.dama(Cor.BRANCA));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PartidaTest`
Expected: FAIL na compilação — `cannot find symbol: class Partida`.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/tabuleirodamas/domain/regras/DiagnosticoDeIlegalidade.java`:

```java
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
```

`src/main/java/com/tabuleirodamas/domain/Partida.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PartidaTest`
Expected: PASS — 14 testes verdes.

Rode a suíte completa: `mvn -q test` → PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/domain src/test/java/com/tabuleirodamas/domain
git commit -m "Adiciona o agregado Partida

Imutavel: cada lance devolve uma nova Partida. A sequencia de captura
inteira e um unico lance, de modo que o turno passa exatamente uma vez -
o prototipo concedia jogada extra por falso positivo na deteccao de
captura encadeada. O placar e derivado do tabuleiro, sem contador
paralelo. Recusas carregam motivo tipado via DiagnosticoDeIlegalidade."
```

---


### Task 11: Repositório em memória e serviço de aplicação

Camada que guarda partidas por `id` e orquestra o domínio. Sem `System.out` e sem `Scanner` — o protótipo tinha os dois dentro do serviço.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/application/PartidaRepository.java`
- Create: `src/main/java/com/tabuleirodamas/application/PartidaRepositoryEmMemoria.java`
- Create: `src/main/java/com/tabuleirodamas/application/PartidaNaoEncontradaException.java`
- Create: `src/main/java/com/tabuleirodamas/application/PartidaService.java`
- Test: `src/test/java/com/tabuleirodamas/application/PartidaServiceTest.java`

**Interfaces:**
- Consumes: `Partida`, `Posicao`, `Movimento` (Task 10)
- Produces:
  - `interface PartidaRepository` com `Partida salvar(Partida)`, `Optional<Partida> buscar(UUID)`, `void remover(UUID)`
  - `class PartidaNaoEncontradaException extends RuntimeException` com `UUID id()`
  - `@Service class PartidaService` com `Partida criar()`, `Partida buscar(UUID)`, `List<Movimento> movimentosLegais(UUID)`, `List<Movimento> movimentosLegaisDe(UUID, Posicao)`, `Partida jogar(UUID, List<Posicao>)`, `void remover(UUID)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/application/PartidaServiceTest.java`:

```java
package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.MotivoIlegalidade;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.MovimentoIlegalException;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaServiceTest {

    private PartidaService servico;

    @BeforeEach
    void preparar() {
        servico = new PartidaService(new PartidaRepositoryEmMemoria());
    }

    @Test
    void criaPartidaERecuperaPeloId() {
        Partida criada = servico.criar();

        assertThat(servico.buscar(criada.id()).id()).isEqualTo(criada.id());
        assertThat(criada.vezDe()).isEqualTo(Cor.BRANCA);
    }

    @Test
    void buscarPartidaInexistenteFalha() {
        UUID inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> servico.buscar(inexistente))
                .isInstanceOf(PartidaNaoEncontradaException.class);
    }

    @Test
    void jogarPersisteONovoEstado() {
        Partida criada = servico.criar();

        servico.jogar(criada.id(), List.of(Posicao.de("c3"), Posicao.de("d4")));

        Partida recuperada = servico.buscar(criada.id());
        assertThat(recuperada.vezDe()).isEqualTo(Cor.PRETA);
        assertThat(recuperada.historico()).hasSize(1);
    }

    @Test
    void jogadaIlegalNaoAlteraOEstadoGuardado() {
        Partida criada = servico.criar();

        assertThatThrownBy(() -> servico.jogar(criada.id(),
                List.of(Posicao.de("b6"), Posicao.de("c5"))))
                .isInstanceOf(MovimentoIlegalException.class)
                .extracting(e -> ((MovimentoIlegalException) e).motivo())
                .isEqualTo(MotivoIlegalidade.PECA_DO_ADVERSARIO);

        assertThat(servico.buscar(criada.id()).vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(servico.buscar(criada.id()).historico()).isEmpty();
    }

    @Test
    void listaMovimentosLegaisDaVezEDeUmaOrigem() {
        Partida criada = servico.criar();

        assertThat(servico.movimentosLegais(criada.id())).hasSize(7);
        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("a3")))
                .extracting(Movimento::destino).containsExactly(Posicao.de("b4"));
    }

    @Test
    void consultarMovimentosDeCasaVaziaDevolveListaVaziaSemFalhar() {
        Partida criada = servico.criar();

        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("d4"))).isEmpty();
        assertThat(servico.movimentosLegaisDe(criada.id(), Posicao.de("b6"))).isEmpty();
    }

    @Test
    void removePartida() {
        Partida criada = servico.criar();

        servico.remover(criada.id());

        assertThatThrownBy(() -> servico.buscar(criada.id()))
                .isInstanceOf(PartidaNaoEncontradaException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PartidaServiceTest`
Expected: FAIL na compilação — `cannot find symbol: class PartidaService`.

- [ ] **Step 3: Write minimal implementation**

`PartidaRepository.java`:

```java
package com.tabuleirodamas.application;

import com.tabuleirodamas.domain.Partida;

import java.util.Optional;
import java.util.UUID;

/** Guarda partidas. A implementacao em memoria basta para a demonstracao; trocar
 *  por JPA nao exige tocar no dominio. */
public interface PartidaRepository {

    Partida salvar(Partida partida);

    Optional<Partida> buscar(UUID id);

    void remover(UUID id);
}
```

`PartidaRepositoryEmMemoria.java`:

```java
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
```

`PartidaNaoEncontradaException.java`:

```java
package com.tabuleirodamas.application;

import java.util.UUID;

public class PartidaNaoEncontradaException extends RuntimeException {

    private final transient UUID id;

    public PartidaNaoEncontradaException(UUID id) {
        super("Partida nao encontrada: " + id);
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
```

`PartidaService.java`:

```java
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

    /** Só grava quando o lance é legal: uma jogada recusada não altera o estado. */
    public Partida jogar(UUID id, List<Posicao> caminho) {
        return repositorio.salvar(buscar(id).jogar(caminho));
    }

    public void remover(UUID id) {
        repositorio.remover(id);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PartidaServiceTest`
Expected: PASS — 7 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/application src/test/java/com/tabuleirodamas/application
git commit -m "Adiciona repositorio em memoria e servico de aplicacao

O servico orquestra dominio e persistencia sem conter regra de jogo.
Como o dominio e imutavel, uma jogada recusada nunca chega a ser
gravada."
```

---

### Task 12: DTOs e mapeamento para JSON

Contrato de fio para o Angular. Notação algébrica como identificador de casa, com `linha`/`coluna` acompanhando para facilitar a renderização do grid.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/api/dto/CasaDTO.java`
- Create: `src/main/java/com/tabuleirodamas/api/dto/MovimentoDTO.java`
- Create: `src/main/java/com/tabuleirodamas/api/dto/PartidaDTO.java`
- Create: `src/main/java/com/tabuleirodamas/api/dto/JogadaRequest.java`
- Create: `src/main/java/com/tabuleirodamas/api/PartidaMapper.java`
- Test: `src/test/java/com/tabuleirodamas/api/PartidaMapperTest.java`

**Interfaces:**
- Consumes: `Partida`, `Movimento`, `Tabuleiro`, `Posicao` (Tasks 3-10)
- Produces:
  - `record CasaDTO(String notacao, int linha, int coluna, Cor cor, TipoPeca tipo)`
  - `record MovimentoDTO(List<String> caminho, List<String> capturas, boolean promove)`
  - `record PartidaDTO(String id, EstadoPartida estado, Cor vezDe, Cor vencedor, boolean capturaObrigatoria, Map<Cor, Integer> placar, int lancesSemProgresso, List<CasaDTO> casas, List<MovimentoDTO> historico)`
  - `record JogadaRequest(List<String> caminho)` com `List<Posicao> paraPosicoes()`
  - `final class PartidaMapper` com `static PartidaDTO dePartida(Partida)` e `static MovimentoDTO deMovimento(Movimento)`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/api/PartidaMapperTest.java`:

```java
package com.tabuleirodamas.api;

import com.tabuleirodamas.api.dto.CasaDTO;
import com.tabuleirodamas.api.dto.JogadaRequest;
import com.tabuleirodamas.api.dto.PartidaDTO;
import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.TabuleiroBuilder;
import com.tabuleirodamas.domain.TipoPeca;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartidaMapperTest {

    @Test
    void mapeiaPartidaNovaComVinteEQuatroCasasOcupadas() {
        PartidaDTO dto = PartidaMapper.dePartida(Partida.nova());

        assertThat(dto.casas()).hasSize(24);
        assertThat(dto.estado()).isEqualTo(EstadoPartida.EM_ANDAMENTO);
        assertThat(dto.vezDe()).isEqualTo(Cor.BRANCA);
        assertThat(dto.vencedor()).isNull();
        assertThat(dto.capturaObrigatoria()).isFalse();
        assertThat(dto.historico()).isEmpty();
        assertThat(dto.placar()).containsEntry(Cor.BRANCA, 0).containsEntry(Cor.PRETA, 0);
    }

    @Test
    void casaCarregaNotacaoEIndicesJuntos() {
        PartidaDTO dto = PartidaMapper.dePartida(Partida.nova());

        assertThat(dto.casas()).contains(new CasaDTO("b8", 0, 1, Cor.PRETA, TipoPeca.PEDRA));
        assertThat(dto.casas()).contains(new CasaDTO("a1", 7, 0, Cor.BRANCA, TipoPeca.PEDRA));
    }

    @Test
    void mapeiaSequenciaDeCapturaComCaminhoECapturasEmNotacao() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("a1", Cor.BRANCA)
                .pedra("b2", Cor.PRETA)
                .pedra("d4", Cor.PRETA)
                .pedra("h8", Cor.PRETA)
                .construir(), Cor.BRANCA);

        Partida depois = partida.jogar(List.of(
                Posicao.de("a1"), Posicao.de("c3"), Posicao.de("e5")));
        PartidaDTO dto = PartidaMapper.dePartida(depois);

        assertThat(dto.historico()).singleElement().satisfies(movimento -> {
            assertThat(movimento.caminho()).containsExactly("a1", "c3", "e5");
            assertThat(movimento.capturas()).containsExactlyInAnyOrder("b2", "d4");
        });
    }

    @Test
    void exponeVencedorQuandoAPartidaTermina() {
        Partida partida = Partida.de(TabuleiroBuilder.vazio()
                .pedra("c3", Cor.BRANCA)
                .pedra("d4", Cor.PRETA)
                .construir(), Cor.BRANCA);

        PartidaDTO dto = PartidaMapper.dePartida(
                partida.jogar(List.of(Posicao.de("c3"), Posicao.de("e5"))));

        assertThat(dto.estado()).isEqualTo(EstadoPartida.VITORIA_BRANCA);
        assertThat(dto.vencedor()).isEqualTo(Cor.BRANCA);
    }

    @Test
    void requisicaoDeJogadaConverteNotacaoEmPosicoes() {
        JogadaRequest requisicao = new JogadaRequest(List.of("b6", "d4", "f2"));

        assertThat(requisicao.paraPosicoes())
                .containsExactly(Posicao.de("b6"), Posicao.de("d4"), Posicao.de("f2"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PartidaMapperTest`
Expected: FAIL na compilação — `cannot find symbol: class PartidaDTO`.

- [ ] **Step 3: Write minimal implementation**

`CasaDTO.java`:

```java
package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.TipoPeca;

/** Casa ocupada. Traz a notacao e os indices para o cliente escolher o que usar. */
public record CasaDTO(String notacao, int linha, int coluna, Cor cor, TipoPeca tipo) {
}
```

`MovimentoDTO.java`:

```java
package com.tabuleirodamas.api.dto;

import java.util.List;

public record MovimentoDTO(List<String> caminho, List<String> capturas, boolean promove) {
}
```

`PartidaDTO.java`:

```java
package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.EstadoPartida;

import java.util.List;
import java.util.Map;

public record PartidaDTO(
        String id,
        EstadoPartida estado,
        Cor vezDe,
        Cor vencedor,
        boolean capturaObrigatoria,
        Map<Cor, Integer> placar,
        int lancesSemProgresso,
        List<CasaDTO> casas,
        List<MovimentoDTO> historico) {
}
```

`JogadaRequest.java`:

```java
package com.tabuleirodamas.api.dto;

import com.tabuleirodamas.domain.Posicao;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Uma jogada e sempre o caminho completo: ["b6","d4","f2"]. Guardar so origem e
 * destino seria ambiguo, porque duas sequencias distintas podem terminar na mesma casa.
 */
public record JogadaRequest(
        @NotEmpty(message = "Informe o caminho da jogada.")
        @Size(min = 2, message = "O caminho precisa de ao menos duas casas.")
        List<String> caminho) {

    public List<Posicao> paraPosicoes() {
        return caminho.stream().map(Posicao::de).toList();
    }
}
```

`PartidaMapper.java`:

```java
package com.tabuleirodamas.api;

import com.tabuleirodamas.api.dto.CasaDTO;
import com.tabuleirodamas.api.dto.MovimentoDTO;
import com.tabuleirodamas.api.dto.PartidaDTO;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Posicao;

import java.util.Comparator;
import java.util.List;

public final class PartidaMapper {

    private PartidaMapper() {
    }

    public static PartidaDTO dePartida(Partida partida) {
        List<CasaDTO> casas = partida.tabuleiro().casas().entrySet().stream()
                .sorted(Comparator
                        .comparingInt((java.util.Map.Entry<Posicao, ?> e) -> e.getKey().linha())
                        .thenComparingInt(e -> e.getKey().coluna()))
                .map(entrada -> new CasaDTO(
                        entrada.getKey().notacao(),
                        entrada.getKey().linha(),
                        entrada.getKey().coluna(),
                        ((com.tabuleirodamas.domain.Peca) entrada.getValue()).cor(),
                        ((com.tabuleirodamas.domain.Peca) entrada.getValue()).tipo()))
                .toList();

        return new PartidaDTO(
                partida.id().toString(),
                partida.estado(),
                partida.vezDe(),
                partida.estado().vencedor().orElse(null),
                partida.capturaObrigatoria(),
                partida.placar(),
                partida.lancesSemProgresso(),
                casas,
                partida.historico().stream().map(PartidaMapper::deMovimento).toList());
    }

    public static MovimentoDTO deMovimento(Movimento movimento) {
        return new MovimentoDTO(
                movimento.caminhoEmNotacao(),
                movimento.capturas().stream().map(Posicao::notacao).sorted().toList(),
                movimento.promove());
    }
}
```

Se o cast explícito em `dePartida` incomodar, declare o stream como `Map.Entry<Posicao, Peca>` importando `com.tabuleirodamas.domain.Peca` e `java.util.Map` — o comportamento é o mesmo.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PartidaMapperTest`
Expected: PASS — 5 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/api src/test/java/com/tabuleirodamas/api
git commit -m "Adiciona DTOs e mapeamento para o contrato REST

Casas trazem notacao algebrica e indices juntos para o cliente escolher.
A jogada viaja como caminho completo, sem ambiguidade entre sequencias
que terminam na mesma casa."
```

---

### Task 13: Controller REST e tradução de erros

Expõe a API e converte cada `MotivoIlegalidade` em `ProblemDetail` (RFC 7807). Aqui morre o último resquício do `System.out.println` como canal de erro.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/api/PartidaController.java`
- Create: `src/main/java/com/tabuleirodamas/api/TratadorGlobalDeErros.java`
- Test: `src/test/java/com/tabuleirodamas/api/PartidaControllerTest.java`

**Interfaces:**
- Consumes: `PartidaService`, `PartidaMapper`, `JogadaRequest`, `MovimentoIlegalException`, `PartidaNaoEncontradaException` (Tasks 11-12)
- Produces: os seis endpoints da especificação

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/api/PartidaControllerTest.java`:

```java
package com.tabuleirodamas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PartidaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String criarPartida() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/partidas"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String corpoDeJogada(String... casas) throws Exception {
        return objectMapper.writeValueAsString(Map.of("caminho", casas));
    }

    @Test
    void criaPartidaComTabuleiroCompletoEVezDasBrancas() throws Exception {
        mockMvc.perform(post("/api/partidas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vezDe").value("BRANCA"))
                .andExpect(jsonPath("$.estado").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.casas.length()").value(24))
                .andExpect(jsonPath("$.capturaObrigatoria").value(false));
    }

    @Test
    void consultaPartidaPeloId() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void listaTodosOsLancesLegaisDaVez() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void filtraLancesLegaisPelaCasaDeOrigem() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id).param("origem", "a3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].caminho[1]").value("b4"));
    }

    @Test
    void consultaDeOrigemVaziaDevolveListaVaziaComStatusOk() throws Exception {
        String id = criarPartida();

        mockMvc.perform(get("/api/partidas/{id}/movimentos", id).param("origem", "d4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void executaJogadaValidaEPassaAVez() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3", "d4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vezDe").value("PRETA"))
                .andExpect(jsonPath("$.historico.length()").value(1));
    }

    @Test
    void jogadaIlegalRespondeQuatrocentosEVinteEDoisComMotivoTipado() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("b6", "c5")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.motivo").value("PECA_DO_ADVERSARIO"))
                .andExpect(jsonPath("$.title").value("Movimento ilegal"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void casaForaDoTabuleiroRespondeQuatrocentosEVinteEDois() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3", "z9")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.motivo").value("FORA_DO_TABULEIRO"));
    }

    @Test
    void caminhoCurtoDemaisRespondeQuatrocentos() throws Exception {
        String id = criarPartida();

        mockMvc.perform(post("/api/partidas/{id}/jogadas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeJogada("c3")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void partidaInexistenteRespondeQuatrocentosEQuatro() throws Exception {
        mockMvc.perform(get("/api/partidas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Partida não encontrada"));
    }

    @Test
    void removePartida() throws Exception {
        String id = criarPartida();

        mockMvc.perform(delete("/api/partidas/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/partidas/{id}", id)).andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=PartidaControllerTest`
Expected: FAIL — todos os endpoints respondem `404` porque `PartidaController` ainda não existe.

- [ ] **Step 3: Write minimal implementation**

`PartidaController.java`:

```java
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

    /** Sem "origem", devolve todos os lances legais da vez. Consulta nunca recusa. */
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
```

`TratadorGlobalDeErros.java`:

```java
package com.tabuleirodamas.api;

import com.tabuleirodamas.application.PartidaNaoEncontradaException;
import com.tabuleirodamas.domain.MovimentoIlegalException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as excecoes do dominio em RFC 7807. Substitui os System.out.println do
 * prototipo: o motivo da recusa agora chega ao cliente em formato legivel por maquina.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    @ExceptionHandler(MovimentoIlegalException.class)
    public ProblemDetail movimentoIlegal(MovimentoIlegalException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, excecao.motivo().mensagem());
        problema.setTitle("Movimento ilegal");
        problema.setProperty("motivo", excecao.motivo().name());
        return problema;
    }

    @ExceptionHandler(PartidaNaoEncontradaException.class)
    public ProblemDetail partidaNaoEncontrada(PartidaNaoEncontradaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Partida não encontrada");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail requisicaoInvalida(MethodArgumentNotValidException excecao) {
        String detalhe = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida.");
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Requisição inválida");
        return problema;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=PartidaControllerTest`
Expected: PASS — 11 testes verdes.

Se `casaForaDoTabuleiroRespondeQuatrocentosEVinteEDois` retornar `400` em vez de `422`, confirme que `JogadaRequest.paraPosicoes()` só é chamado dentro do controller (depois da validação do Bean Validation), e não durante a desserialização.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tabuleirodamas/api src/test/java/com/tabuleirodamas/api
git commit -m "Expoe a API REST e traduz erros em RFC 7807

Seis endpoints cobrindo criacao, consulta, lances legais por origem,
jogada e remocao. Cada recusa carrega o motivo tipado no corpo da
resposta, no lugar das mensagens que o prototipo escrevia no console."
```

---

### Task 14: CORS, OpenAPI, README e runner de console

Fecha o ambiente para o Angular e devolve a demonstração de terminal sobre o domínio novo.

**Files:**
- Create: `src/main/java/com/tabuleirodamas/config/ConfiguracaoCors.java`
- Create: `src/main/java/com/tabuleirodamas/config/ConfiguracaoOpenApi.java`
- Create: `src/main/java/com/tabuleirodamas/console/RunnerConsole.java`
- Modify: `README.md`
- Test: `src/test/java/com/tabuleirodamas/config/ConfiguracaoCorsTest.java`

**Interfaces:**
- Consumes: `PartidaService`, `PartidaController` (Tasks 11-13)
- Produces: `ConfiguracaoCors` (bean `WebMvcConfigurer`), `ConfiguracaoOpenApi` (bean `OpenAPI`), `RunnerConsole` (`CommandLineRunner` sob o profile `console`)

- [ ] **Step 1: Write the failing test**

`src/test/java/com/tabuleirodamas/config/ConfiguracaoCorsTest.java`:

```java
package com.tabuleirodamas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfiguracaoCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void liberaOOrigemDoAngularEmDesenvolvimento() throws Exception {
        mockMvc.perform(options("/api/partidas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void naoLiberaOrigemDesconhecida() throws Exception {
        mockMvc.perform(options("/api/partidas")
                        .header("Origin", "http://exemplo-nao-autorizado.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ConfiguracaoCorsTest`
Expected: FAIL — a requisição preflight responde `403` porque nenhuma origem está liberada.

- [ ] **Step 3: Write minimal implementation**

`ConfiguracaoCors.java`:

```java
package com.tabuleirodamas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Libera o front Angular em desenvolvimento. Ajuste damas.cors.origens em producao. */
@Configuration
public class ConfiguracaoCors implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public ConfiguracaoCors(
            @Value("${damas.cors.origens:http://localhost:4200}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas.clone();
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
```

`ConfiguracaoOpenApi.java`:

```java
package com.tabuleirodamas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    public OpenAPI documentacaoDaApi() {
        return new OpenAPI().info(new Info()
                .title("API de Damas Brasileiras")
                .version("1.0.0")
                .description("Backend do jogo de damas: captura obrigatória, lei da maioria "
                        + "e dama voadora. Contrato pronto para geração de client TypeScript."));
    }
}
```

`RunnerConsole.java`:

```java
package com.tabuleirodamas.console;

import com.tabuleirodamas.domain.Cor;
import com.tabuleirodamas.domain.Movimento;
import com.tabuleirodamas.domain.MovimentoIlegalException;
import com.tabuleirodamas.domain.Partida;
import com.tabuleirodamas.domain.Peca;
import com.tabuleirodamas.domain.Posicao;
import com.tabuleirodamas.domain.Tabuleiro;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Demonstracao em terminal, preservada do prototipo e reescrita sobre o dominio novo.
 * Ativar com: mvn spring-boot:run -Dspring-boot.run.profiles=console
 */
@Profile("console")
@Component
public class RunnerConsole implements CommandLineRunner {

    @Override
    public void run(String... args) {
        Scanner entrada = new Scanner(System.in);
        Partida partida = Partida.nova();

        while (!partida.estado().encerrada()) {
            imprimir(partida);
            System.out.printf("Vez das %s. Informe o caminho (ex: c3 d4): ", partida.vezDe());

            String linha = entrada.nextLine().trim();
            if (linha.equalsIgnoreCase("sair")) {
                return;
            }
            try {
                partida = partida.jogar(paraCaminho(linha));
            } catch (MovimentoIlegalException excecao) {
                System.out.println("Recusado: " + excecao.motivo().mensagem());
            } catch (RuntimeException excecao) {
                System.out.println("Entrada invalida. Use casas como: c3 d4");
            }
        }

        imprimir(partida);
        System.out.println(partida.estado().vencedor()
                .map(cor -> "Fim de jogo. Venceram as " + cor + ".")
                .orElse("Fim de jogo. Empate."));
    }

    private List<Posicao> paraCaminho(String linha) {
        return Arrays.stream(linha.split("\\s+")).map(Posicao::de).toList();
    }

    private void imprimir(Partida partida) {
        Tabuleiro tabuleiro = partida.tabuleiro();
        System.out.println("   a b c d e f g h");
        for (int linha = 0; linha < Tabuleiro.TAMANHO; linha++) {
            StringBuilder texto = new StringBuilder(" " + (Tabuleiro.TAMANHO - linha) + " ");
            for (int coluna = 0; coluna < Tabuleiro.TAMANHO; coluna++) {
                texto.append(simbolo(tabuleiro.pecaEm(new Posicao(linha, coluna)).orElse(null)))
                        .append(' ');
            }
            System.out.println(texto);
        }
        System.out.println("Placar: " + partida.placar()
                + (partida.capturaObrigatoria() ? "  [captura obrigatoria]" : ""));
    }

    private char simbolo(Peca peca) {
        if (peca == null) {
            return '.';
        }
        char letra = peca.cor() == Cor.BRANCA ? 'b' : 'p';
        return peca.ehDama() ? Character.toUpperCase(letra) : letra;
    }
}
```

Substitua `README.md` inteiro por:

````markdown
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
  "vencedor": null,
  "capturaObrigatoria": true,
  "placar": { "BRANCA": 3, "PRETA": 1 },
  "lancesSemProgresso": 4,
  "casas": [
    { "notacao": "b8", "linha": 0, "coluna": 1, "cor": "PRETA", "tipo": "PEDRA" }
  ],
  "historico": [ { "caminho": ["c3", "d4"], "capturas": [], "promove": false } ]
}
```

`casas` lista somente as casas ocupadas.

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
`GET /api/partidas/{id}/movimentos?origem=<casa>` e destaque os `caminho[último]` de
cada lance devolvido; ao clicar no destino, envie o `caminho` correspondente para
`POST /api/partidas/{id}/jogadas`.

## Arquitetura

```
com.tabuleirodamas
├── domain/       regras puras, imutáveis, sem Spring e sem I/O
│   └── regras/   geração de lances, lei da maioria, fim de jogo
├── application/  serviço e repositório
├── api/          controller, DTOs e tradução de erros
└── config/       CORS e OpenAPI
```

As dependências apontam numa direção só: `api → application → domain`. O tabuleiro é
imutável, então gerar os lances legais nunca altera a partida.

## Persistência

As partidas ficam em memória (`ConcurrentHashMap`) e se perdem ao reiniciar. A
interface `PartidaRepository` isola essa escolha: trocar por JPA não exige tocar no
domínio.
````

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=ConfiguracaoCorsTest`
Expected: PASS — 2 testes verdes.

Rode a suíte completa e suba a aplicação para conferência final:

```bash
mvn -q test
mvn -q spring-boot:run
```

Com a aplicação no ar, em outro terminal:

```bash
curl -s -X POST http://localhost:8080/api/partidas
curl -s http://localhost:8080/v3/api-docs | head -c 200
```

Expected: JSON da partida com 24 casas, e o contrato OpenAPI respondendo.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Configura CORS, OpenAPI, README e runner de console

Libera o front Angular em localhost:4200, publica o contrato em
/v3/api-docs para geracao do client TypeScript, e devolve a
demonstracao de terminal reescrita sobre o dominio novo."
```

---

## Verificação final

Depois da Task 14, rode a suíte inteira e confira o resultado antes de declarar concluído:

```bash
mvn clean test
```

Confira também, um a um, que os doze itens do diagnóstico da especificação foram fechados:

| # | Problema do protótipo | Fechado em |
|---|---|---|
| 1 | Dependência circular `Models ↔ Controllers` | Task 3 — domínio sem dependência de serviço |
| 2 | Limites validados depois do acesso ao array | Task 2 — `Posicao` valida na construção |
| 3 | Dama pulava turno, destino ocupado e limites | Task 10 — todo lance passa por `movimentosLegais` |
| 4 | Captura de dama não pontuava | Task 10 — placar derivado do tabuleiro |
| 5 | Dama para a própria casa saía do tabuleiro | Task 2 + Task 10 — caminho precisa constar nos legais |
| 6 | Validar alterava o tabuleiro | Tasks 3-7 — tabuleiro imutável, geração pura |
| 7 | Varredura de dama aplicada a pedra | Task 6 — `saltoDePedra` limitado a distância 2 |
| 8 | Vitória testava o jogador errado | Task 9 — `AvaliadorDeFimDeJogo` |
| 9 | Motivo da recusa só em `System.out` | Tasks 2, 10, 13 — `MotivoIlegalidade` até o `ProblemDetail` |
| 10 | Posição duplicada entre array e `Peca` | Task 3 — `Peca` sem posição |
| 11 | Sem captura obrigatória, lei da maioria e empate | Tasks 8-9 |
| 12 | Sem testes, `target/` versionado | Task 1 + testes de todas as tasks |
