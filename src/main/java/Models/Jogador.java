package Models;

import Enumeration.Cor;

public class Jogador {
    private Cor corPecaJogador;
    private boolean suaVez;
    private int qtdePontos;

    public Jogador(boolean suaVez, Cor corPecasJogador) {
        this.suaVez = suaVez;
        this.corPecaJogador = corPecasJogador;
        this.qtdePontos = 0;
    }

    public boolean isSuaVez() {
        return suaVez;
    }

    public void setSuaVez(boolean suaVez) {
        this.suaVez = suaVez;
    }

    public int getQtdePontos() {
        return qtdePontos;
    }

    public void setQtdePontos(int qtdePontos) {
        this.qtdePontos = qtdePontos;
    }

    public Cor getCorPecaJogador() {
        return corPecaJogador;
    }
}
