package application.interpreter;

import application.parser.Comando;
import application.parser.Expressao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class Interpretador implements Expressao.Visitor<Object>, Comando.Visitor<Void> {

    private final Scanner scanner = new Scanner(System.in);
    private final Consumer<String> saida;
    private final Ambiente ambiente = new Ambiente();

    public Interpretador() {
        this(System.out::println); 
    }

    public Interpretador(Consumer<String> saida) {
        this.saida = saida; 
    }

    public void mostrar(String texto) {
        saida.accept(texto);
    }

    private static class Ambiente {
        private final Map<String, Object> valores = new HashMap<>();

        public void definir(String nome, Object valor) {
            valores.put(nome, valor);
        }

        public Object obter(String nome) {
            if (valores.containsKey(nome)) {
                return valores.get(nome);
            }
            throw new RuntimeException("Variável não definida: " + nome);
        }
    }

    public void interpretar(List<Comando> comandos) {
        try {
            for (Comando comando : comandos) {
                executar(comando);
            }
        } catch (RuntimeException erro) {
            mostrar("Erro de execução: " + erro.getMessage());
        }
    }

    private void executar(Comando comando) {
        comando.aceitar(this);
    }

    private Object avaliar(Expressao expr) {
        return expr.aceitar(this);
    }

    @Override
    public Void visitarComandoExpressao(Comando.ExpressaoCmd cmd) {
        avaliar(cmd.expressao);
        return null;
    }

    @Override
    public Void visitarComandoMostrar(Comando.MostrarCmd cmd) {
        Object valor = avaliar(cmd.expressao);
        mostrar(String.valueOf(valor));
        return null;
    }

    @Override
    public Void visitarComandoVar(Comando.VarCmd cmd) {
        Object valor = null;
        if (cmd.inicializador != null) {
            valor = avaliar(cmd.inicializador);
        }
        ambiente.definir(cmd.nome.lexema, valor);
        return null;
    }

    @Override
    public Void visitarComandoBloco(Comando.BlocoCmd cmd) {
        for (Comando comando : cmd.comandos) {
            executar(comando);
        }
        return null;
    }

    @Override
    public Void visitarComandoSe(Comando.SeCmd cmd) {
        Object condicao = avaliar(cmd.condicao);
        if (converterParaBooleano(condicao)) {
            executar(cmd.entaoRamo);
        } else if (cmd.senaoRamo != null) {
            executar(cmd.senaoRamo);
        }
        return null;
    }

    @Override
    public Void visitarComandoEnquanto(Comando.EnquantoCmd cmd) {
        while (converterParaBooleano(avaliar(cmd.condicao))) {
            executar(cmd.corpo);
        }
        return null;
    }

    @Override
    public Void visitarComandoPara(Comando.ParaCmd cmd) {
        Object valorInicial = avaliar(cmd.inicio);
        ambiente.definir(cmd.variavel.lexema, valorInicial);

        double incremento = 1.0;
        if (cmd.incremento != null) {
            Object valorIncremento = avaliar(cmd.incremento);
            if (valorIncremento instanceof Double) {
                incremento = (double) valorIncremento;
            }
        }

        while (true) {
            Object atual = ambiente.obter(cmd.variavel.lexema);
            Object alvo = avaliar(cmd.fim);

            if (atual instanceof Double && alvo instanceof Double) {
                double vAtual = (double) atual;
                double vAlvo = (double) alvo;

                if (incremento > 0 && vAtual >= vAlvo) break;
                if (incremento < 0 && vAtual <= vAlvo) break;
            } else break;

            executar(cmd.corpo);
            double novoValor = (double) ambiente.obter(cmd.variavel.lexema) + incremento;
            ambiente.definir(cmd.variavel.lexema, novoValor);
        }

        return null;
    }

    @Override
    public Void visitarComandoLer(Comando.LerCmd cmd) {
        mostrar("Entrada (" + cmd.variavel.lexema + "): ");
        String entrada = scanner.nextLine();

        try {
            double valor = Double.parseDouble(entrada);
            ambiente.definir(cmd.variavel.lexema, valor);
            return null;
        } catch (NumberFormatException ignored) {}

        if (entrada.equalsIgnoreCase("sim")) {
            ambiente.definir(cmd.variavel.lexema, true);
        } else if (entrada.equalsIgnoreCase("nao")) {
            ambiente.definir(cmd.variavel.lexema, false);
        } else {
            ambiente.definir(cmd.variavel.lexema, entrada);
        }

        return null;
    }

    @Override
    public Object visitarExpressaoBinaria(Expressao.Binaria expr) {
        if (expr.esquerda == null) {
            Object direita = avaliar(expr.direita);
            return switch (expr.operador.tipo) {
                case SUBTRACAO -> -(double) direita;
                case NAO_LOGICO -> !converterParaBooleano(direita);
                default -> throw new RuntimeException("Operador unário não suportado: " + expr.operador.lexema);
            };
        }

        Object esquerda = avaliar(expr.esquerda);
        Object direita = avaliar(expr.direita);

        return switch (expr.operador.tipo) {
            case SOMA -> (esquerda instanceof String || direita instanceof String)
                    ? String.valueOf(esquerda) + direita
                    : (double) esquerda + (double) direita;
            case SUBTRACAO -> (double) esquerda - (double) direita;
            case MULTIPLICACAO -> (double) esquerda * (double) direita;
            case DIVISAO -> {
                if ((double) direita == 0) throw new RuntimeException("Divisão por zero.");
                yield (double) esquerda / (double) direita;
            }
            case IGUAL -> iguais(esquerda, direita);
            case DIFERENTE -> !iguais(esquerda, direita);
            case MAIOR -> (double) esquerda > (double) direita;
            case MAIOR_IGUAL -> (double) esquerda >= (double) direita;
            case MENOR -> (double) esquerda < (double) direita;
            case MENOR_IGUAL -> (double) esquerda <= (double) direita;
            case E -> converterParaBooleano(esquerda) && converterParaBooleano(direita);
            case OU -> converterParaBooleano(esquerda) || converterParaBooleano(direita);
            default -> throw new RuntimeException("Operador não suportado: " + expr.operador.lexema);
        };
    }

    @Override
    public Object visitarExpressaoLiteral(Expressao.Literal expr) {
        String valor = expr.valor;

        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException ignored) {}

        if (valor.equals("sim")) return true;
        if (valor.equals("nao")) return false;
        if (valor.startsWith("\"") && valor.endsWith("\"")) {
            return valor.substring(1, valor.length() - 1);
        }

        return valor;
    }

    @Override
    public Object visitarExpressaoVariavel(Expressao.Variavel expr) {
        return ambiente.obter(expr.nome);
    }

    private boolean iguais(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean converterParaBooleano(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (boolean) obj;
        if (obj instanceof Double) return (double) obj != 0;
        if (obj instanceof String) return !((String) obj).isEmpty();
        return true;
    }
}
