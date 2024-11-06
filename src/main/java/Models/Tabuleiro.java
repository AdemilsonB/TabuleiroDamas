package Models;

import Controllers.TabuleiroService;

public class Tabuleiro {
    private Peca[][] tabuleiro;
    private TabuleiroService tabuleiroService;

    public Tabuleiro() {
        tabuleiro = new Peca[8][8];

        tabuleiroService = new TabuleiroService();
        tabuleiroService.iniciarTabuleiro(tabuleiro);
    }

    public Peca getPeca(int linha, int coluna) {
        return tabuleiro[linha][coluna];
    }

    public void removerPeca(int linha, int coluna) {
        tabuleiro[linha][coluna] = null;
    }

    public Peca[][] getTabuleiro() {
        return tabuleiro;
    }
}
