package Models;

import Enumeration.Cor;

public class Peca {
    private int posicaoLinha;
    private int posicaoColuna;
    private Cor cor;
    private boolean dama;

    public Peca(Cor cor, int posicaoLinha, int posicaoColuna) {
        this.posicaoLinha = posicaoLinha;
        this.posicaoColuna = posicaoColuna;
        this.cor = cor;
        this.dama = false;
    }

    public Cor getCor() {
        return cor;
    }

    public void setCor(Cor cor) {
        this.cor = cor;
    }

    public int getPosicaoLinha() {
        return posicaoLinha;
    }

    public void setPosicaoLinha(int posicaoLinha) {
        this.posicaoLinha = posicaoLinha;
    }

    public int getPosicaoColuna() {
        return posicaoColuna;
    }

    public void setPosicaoColuna(int posicaoColuna) {
        this.posicaoColuna = posicaoColuna;
    }

    public boolean isDama() {
        return dama;
    }

    public void setDama(boolean dama) {
        this.dama = dama;
    }
}
