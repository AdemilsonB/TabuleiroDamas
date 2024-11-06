package Controllers;

import Enumeration.Cor;
import Models.Jogador;
import Models.Partida;
import Models.Peca;
import Models.Tabuleiro;

import java.util.Scanner;

public class PartidaService {

    private TabuleiroService tabuleiroService;
    private Scanner scanner;

    public PartidaService() {
        this.tabuleiroService = new TabuleiroService();
        this.scanner = new Scanner(System.in);
    }

    public void imprimeTabuleiro(Partida partida) {
        Tabuleiro tabuleiro = partida.getTabuleiro();
        Peca[][] tabuleiroPeca = tabuleiro.getTabuleiro();

        System.out.print("  ");
        for (int x = 0; x < 8; x++) {
            System.out.print(x + " ");
        }
        System.out.println();

        for (int linha = 0; linha < 8; linha++) {
            System.out.print((linha) + " ");
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = tabuleiroPeca[linha][coluna];
                if (peca != null) {
                    System.out.print(peca.getCor().toString().charAt(0) + " ");
                } else {
                    System.out.print("- ");
                }
            }
            System.out.println();
        }
    }

    public void movePeca(Partida partida, int posicaoInicialLinha, int posicaoInicialColuna, int posicaoFinalLinha, int posicaoFinalColuna, int qtdPontosAtual) {
        if (!tabuleiroService.movimentoValido(partida, posicaoInicialLinha, posicaoInicialColuna, posicaoFinalLinha, posicaoFinalColuna)) {
            return;
        }

        tabuleiroService.movePeca(partida, posicaoInicialLinha, posicaoInicialColuna, posicaoFinalLinha, posicaoFinalColuna);

        Jogador jogadorAtual = partida.getJogadorAtual();
        Boolean possibilidadeCaptura = tabuleiroService.verificarCapturaPossivel(partida, jogadorAtual, posicaoFinalLinha, posicaoFinalColuna);

        if(qtdPontosAtual == jogadorAtual.getQtdePontos() || !possibilidadeCaptura) {
            mudarVezJogador(partida);
        }
    }

    public void mudarVezJogador(Partida partida) {
        Jogador jogador1 = partida.getJogador1();
        Jogador jogador2 = partida.getJogador2();

        jogador1.setSuaVez(!jogador1.isSuaVez());
        jogador2.setSuaVez(!jogador2.isSuaVez());
    }

    public void encerrarPartida(Jogador jogadorVencedor) {
        System.out.println("Fim de jogo!");
        System.out.println("O jogador com peças " + jogadorVencedor.getCorPecaJogador().toString() + " venceu a partida!");
    }
}
