package application;

import java.util.ArrayList;
import java.util.List;

public class ErroReporter {
    private static final List<Erro> erros = new ArrayList<>();
    private static boolean temErro = false;

    public static void relatar(int linha, String mensagem) {
        temErro = true;
        erros.add(new Erro(linha, mensagem));
    }

    public static void relatar(int linha, String onde, String mensagem) {
        temErro = true;
        erros.add(new Erro(linha, onde, mensagem));
    }

    public static void mostrarErros() {
        for (Erro erro : erros) {
            System.err.println(erro);
        }
    }

    public static boolean temErro() {
        return temErro;
    }

    public static void limparErros() {
        erros.clear();
        temErro = false;
    }

    private static class Erro {
        final int linha;
        final String onde;
        final String mensagem;

        Erro(int linha, String mensagem) {
            this(linha, "", mensagem);
        }

        Erro(int linha, String onde, String mensagem) {
            this.linha = linha;
            this.onde = onde;
            this.mensagem = mensagem;
        }

        @Override
        public String toString() {
            if (onde.isEmpty()) {
                return "[Linha " + linha + "] Erro: " + mensagem;
            } else {
                return "[Linha " + linha + "] Erro em '" + onde + "': " + mensagem;
            }
        }
    }
}