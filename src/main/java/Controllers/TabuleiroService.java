package Controllers;

import Enumeration.Cor;
import Models.Jogador;
import Models.Partida;
import Models.Peca;
import Models.Tabuleiro;

public class TabuleiroService {

    public void iniciarTabuleiro(Peca[][] tabuleiro) {
        for (int linha = 0; linha < 3; linha++) {
            for (int coluna = 0; coluna < 8; coluna += 2) {
                if (linha % 2 == 0) {
                    tabuleiro[linha][coluna + 1] = new Peca(Cor.PRETA, linha, coluna + 1);
                } else {
                    tabuleiro[linha][coluna] = new Peca(Cor.PRETA, linha, coluna);
                }
            }
        }

        for (int linha = 5; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna += 2) {
                if (linha % 2 == 0) {
                    tabuleiro[linha][coluna + 1] = new Peca(Cor.BRANCA, linha, coluna + 1);
                } else {
                    tabuleiro[linha][coluna] = new Peca(Cor.BRANCA, linha, coluna);
                }
            }
        }
    }

    public void movePeca(Partida partida, int posicaoLinhaInicial, int posicaoColunaInicial, int posicaoLinhaFinal, int posicaoColunaFinal) {
        Peca[][] tabuleiroPeca = partida.getTabuleiro().getTabuleiro();
        Peca peca = tabuleiroPeca[posicaoLinhaInicial][posicaoColunaInicial];

        tabuleiroPeca[posicaoLinhaInicial][posicaoColunaInicial] = null;
        tabuleiroPeca[posicaoLinhaFinal][posicaoColunaFinal] = peca;

        peca.setPosicaoLinha(posicaoLinhaFinal);
        peca.setPosicaoColuna(posicaoColunaFinal);

        verificarPromocaoDama(peca);

    }

    private void verificarPromocaoDama(Peca peca) {
        if ((Cor.BRANCA.equals(peca.getCor()) && peca.getPosicaoLinha() == 0) ||
                (Cor.PRETA.equals(peca.getCor()) && peca.getPosicaoLinha() == 7)) {
            peca.setDama(true);
        }
    }

    public Boolean verificarAcaoCaptura(Partida partida, Peca pecaPosicaoAtual, int linhaDestino, int colunaDestino) {
        int linhaAtual = pecaPosicaoAtual.getPosicaoLinha();
        int colunaAtual = pecaPosicaoAtual.getPosicaoColuna();

        if (Math.abs(linhaDestino - linhaAtual) == 2 && Math.abs(colunaDestino - colunaAtual) == 2) {
            int linhaMeio = (linhaAtual + linhaDestino) / 2;
            int colunaMeio = (colunaAtual + colunaDestino) / 2;

            Peca pecaIntermediaria = partida.getTabuleiro().getPeca(linhaMeio, colunaMeio);

            if (pecaIntermediaria != null && !pecaIntermediaria.getCor().equals(pecaPosicaoAtual.getCor())) {
                efetuarCapturaPeca(partida, pecaIntermediaria);
                return true;
            }
        }

        return false;
    }

    public void efetuarCapturaPeca(Partida partida, Peca pecaCapturada) {
        Jogador jogador = partida.getCurrentJogador();
        Tabuleiro tabuleiro = partida.getTabuleiro();

        tabuleiro.removerPeca(pecaCapturada.getPosicaoLinha(), pecaCapturada.getPosicaoColuna());

        int qtdePontosJogador = jogador.getQtdePontos() + 1;
        jogador.setQtdePontos(qtdePontosJogador);
        System.out.println("Peça capturada");
    }


    public boolean movimentoValido(Partida partida, int posicaoLinhaInicial, int posicaoColunaInicial, int posicaoLinhaFinal, int posicaoColunaFinal) {
        Peca pecaPosicaoAtual = partida.getTabuleiro().getPeca(posicaoLinhaInicial, posicaoColunaInicial);
        Peca pecaPosicaoDestino = partida.getTabuleiro().getPeca(posicaoLinhaFinal, posicaoColunaFinal);
        boolean pecaPosicaoAtualIsDama = false;
        Cor pecaAtuaCor = Cor.BRANCA;

        if(pecaPosicaoAtual != null) {
            pecaPosicaoAtualIsDama = pecaPosicaoAtual.isDama();
            pecaAtuaCor = pecaPosicaoAtual.getCor();
        }

        Cor corPecaJogador1 = partida.getJogador1().getCorPecaJogador();
        Cor corPecaJogador2 = partida.getJogador2().getCorPecaJogador();

        //Todo válida se o jogador esta movimento sua peça e não a peça do adversário
        if(pecaAtuaCor.equals(corPecaJogador1) && !partida.getJogador1().isSuaVez()) {
            System.out.println("Peça inválida, pertence ao outro jogador.");
            return false;
        } else if(pecaAtuaCor.equals(corPecaJogador2) && !partida.getJogador2().isSuaVez())  {
            System.out.println("Peça inválida, pertence ao outro jogador.");
            return false;
        }

        if (pecaPosicaoAtual == null) {
            System.out.println("Não há peça na posição inicial.");
            return false;
        } else if(pecaPosicaoDestino != null) {
            System.out.println("Movimento Inválido! A posição já esta ocupada por outra peça.");
            return false;
        } else if (posicaoLinhaFinal < 0 || posicaoLinhaFinal >= 8 || posicaoColunaFinal < 0 || posicaoColunaFinal >= 8) {
            System.out.println("Posição final fora dos limites do tabuleiro.");
            return false;
        }

        Boolean movimentoCaptura = verificarAcaoCaptura(partida, pecaPosicaoAtual, posicaoLinhaFinal, posicaoColunaFinal);
        //Todo Verifica se não está voltando casas, sem ser damas ou sem ter movimento de captura válido
        if(!pecaPosicaoAtualIsDama && Cor.PRETA.equals(pecaAtuaCor)){
            if((posicaoLinhaInicial > posicaoLinhaFinal) && !movimentoCaptura) {
                System.out.println("Movimento inválido! Para efetuar esta ação é necessário ser Dama ou ter ação de captura.");
                return false;
            }
        } else if(!pecaPosicaoAtualIsDama && Cor.BRANCA.equals(pecaAtuaCor)){
            if((posicaoLinhaInicial < posicaoLinhaFinal) && !movimentoCaptura) {
                System.out.println("Movimento inválido! Para efetuar esta ação é necessário ser Dama ou ter ação de captura.");
                return false;
            }
        }

        if (Math.abs(posicaoLinhaFinal - posicaoLinhaInicial) != Math.abs(posicaoColunaFinal - posicaoColunaInicial) && !movimentoCaptura) {
            System.out.println("Movimento inválido! O movimento deve ser na diagonal!");
            return false;
        }

        if (!pecaPosicaoAtual.isDama() && !movimentoCaptura && (Math.abs(posicaoLinhaFinal - posicaoLinhaInicial) > 1 || Math.abs(posicaoColunaFinal - posicaoColunaInicial) > 1)) {
            System.out.println("Movimento inválido! Não pode pular mais de uma casa sem captura!");
            return false;
        }

        return true;
    }
}
