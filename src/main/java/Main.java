import Controllers.PartidaService;
import Models.Jogador;
import Models.Partida;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Partida partida = new Partida();
        PartidaService partidaService = new PartidaService();

        do {
            partidaService.imprimeTabuleiro(partida);

            Jogador jogadorAtual = partida.getJogadorAtual();
            int qtdPontosAtual = jogadorAtual.getQtdePontos();

            System.out.println("Vez do jogador com peças " + (jogadorAtual.getCorPecaJogador().toString()));
            System.out.println("Digite o número da linha inicial: ");
            int posicaoInicialLinha = scanner.nextInt();
            System.out.println("Digite o número da coluna inicial: ");
            int posicaoInicialColuna = scanner.nextInt();
            System.out.println("Digite o número da linha de destino: ");
            int posicaoFinalLinha = scanner.nextInt();
            System.out.println("Digite o número da coluna de destino: ");
            int posicaoFinalColuna = scanner.nextInt();

            partidaService.movePeca(partida, posicaoInicialLinha, posicaoInicialColuna, posicaoFinalLinha, posicaoFinalColuna, qtdPontosAtual);

        } while (partida.getJogadorAtual().getQtdePontos() < 12);

        partidaService.encerrarPartida(partida.getJogadorAtual());
    }
}
