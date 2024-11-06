package Models;

import Enumeration.Cor;

public class Partida {
    private Tabuleiro tabuleiro;
    private Jogador jogador1;
    private Jogador jogador2;

    public Partida() {
        tabuleiro = new Tabuleiro();
        jogador1 = new Jogador(true, Cor.BRANCA);
        jogador2 = new Jogador(false, Cor.PRETA);
    }

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    public Jogador getJogadorAtual() {
        return jogador1.isSuaVez() ? jogador1 : jogador2;
    }

    public Jogador getJogador1() {
        return jogador1;
    }

    public Jogador getJogador2() {
        return jogador2;
    }
}
