package com.tabuleirodamas.console;

import com.tabuleirodamas.domain.Cor;
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
        try (Scanner entrada = new Scanner(System.in)) {
            Partida partida = Partida.nova();

            while (!partida.estado().encerrada()) {
                imprimir(partida);
                System.out.printf("Vez das %s. Informe o caminho (ex: c3 d4), ou 'sair': ",
                        partida.vezDe());

                String linha = entrada.nextLine().trim();
                if (linha.equalsIgnoreCase("sair")) {
                    return;
                }
                partida = tentarJogar(partida, linha);
            }

            imprimir(partida);
            System.out.println(partida.estado().vencedor()
                    .map(cor -> "Fim de jogo. Venceram as " + cor + ".")
                    .orElse("Fim de jogo. Empate."));
        }
    }

    private Partida tentarJogar(Partida partida, String linha) {
        try {
            return partida.jogar(paraCaminho(linha));
        } catch (MovimentoIlegalException excecao) {
            System.out.println("Recusado: " + excecao.motivo().mensagem());
        } catch (RuntimeException excecao) {
            System.out.println("Entrada invalida. Use casas como: c3 d4");
        }
        return partida;
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
