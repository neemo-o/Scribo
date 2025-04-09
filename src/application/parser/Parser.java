package application.parser;

import application.ErroReporter;
import application.lexer.Token;
import application.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int atual = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Comando> parse() {
        List<Comando> comandos = new ArrayList<>();
        while (!isFim()) {
            try {
                comandos.add(declaracao());
            } catch (ParseException e) {
                sincronizar();
            }
        }
        return comandos;
    }

    // Sincroniza após erro para continuar analisando
    private void sincronizar() {
        avancar();

        while (!isFim()) {
            if (anterior().tipo == TokenType.PONTO_VIRGULA) return;

            switch (peek().tipo) {
                case SE:
                case PARA:
                case ENQUANTO:
                case MOSTRAR:
                case LER:
                    return;
			default:
				break;
            }

            avancar();
        }
    }

    private Comando declaracao() {
        if (verificarConsumir(TokenType.NUMERO, TokenType.TEXTO, TokenType.LOGICO, TokenType.LISTA) || verificar(TokenType.IDENTIFICADOR)) {
            return declaracaoVariavel();
        }
        return comando();
    }

    private Comando comando() {
        if (verificarConsumir(TokenType.SE)) return comandoSe();
        if (verificarConsumir(TokenType.ENQUANTO)) return comandoEnquanto();
        if (verificarConsumir(TokenType.PARA)) return comandoPara();
        if (verificarConsumir(TokenType.MOSTRAR)) return comandoMostrar();
        if (verificarConsumir(TokenType.LER)) return comandoLer();
        if (verificarConsumir(TokenType.ABRE_CHAVE)) return new Comando.BlocoCmd(bloco());

        return comandoExpressao();
    }

    private List<Comando> bloco() {
        List<Comando> comandos = new ArrayList<>();

        while (!verificar(TokenType.FECHA_CHAVE) && !isFim()) {
            comandos.add(declaracao());
        }

        consumir(TokenType.FECHA_CHAVE, "Esperado '}' após bloco.");
        return comandos;
    }

    private Comando comandoSe() {
        consumir(TokenType.ABRE_PAREN, "Esperado '(' após 'se'.");
        Expressao condicao = expressao();
        consumir(TokenType.FECHA_PAREN, "Esperado ')' após condição.");

        Comando entaoRamo = comando();
        Comando senaoRamo = null;
        if (verificarConsumir(TokenType.SENAO)) {
            senaoRamo = comando();
        }

        return new Comando.SeCmd(condicao, entaoRamo, senaoRamo);
    }

    private Comando comandoEnquanto() {
        consumir(TokenType.ABRE_PAREN, "Esperado '(' após 'enquanto'.");
        Expressao condicao = expressao();
        consumir(TokenType.FECHA_PAREN, "Esperado ')' após condição.");
        Comando corpo = comando();

        return new Comando.EnquantoCmd(condicao, corpo);
    }

    private Comando comandoPara() {
        consumir(TokenType.ABRE_PAREN, "Esperado '(' após 'para'.");
        
        // Inicialização
        Token variavel = consumir(TokenType.IDENTIFICADOR, "Esperado nome de variável.");
        consumir(TokenType.ATRIBUICAO, "Esperado '->' após nome da variável.");
        Expressao inicio = expressao();
        
        consumir(TokenType.VIRGULA, "Esperado ',' após inicialização.");
        
        // Condição
        Expressao fim = expressao();
        
        // Incremento (opcional)
        Expressao incremento = null;
        if (verificarConsumir(TokenType.VIRGULA)) {
            incremento = expressao();
        }
        
        consumir(TokenType.FECHA_PAREN, "Esperado ')' após condição do para.");
        
        Comando corpo = comando();
        
        return new Comando.ParaCmd(variavel, inicio, fim, incremento, corpo);
    }

    private Comando comandoMostrar() {
        Expressao valor = expressao();
        return new Comando.MostrarCmd(valor);
    }

    private Comando comandoLer() {
        Token nome = consumir(TokenType.IDENTIFICADOR, "Esperado nome de variável após 'ler'.");
        return new Comando.LerCmd(nome);
    }

    private Comando declaracaoVariavel() {
        // Consome o tipo se estiver presente (opcional)
        if (verificar(TokenType.NUMERO, TokenType.TEXTO, TokenType.LOGICO, TokenType.LISTA)) {
            avancar(); // Consome o tipo
        }
        
        Token nome = consumir(TokenType.IDENTIFICADOR, "Esperado nome de variável.");
        consumir(TokenType.ATRIBUICAO, "Esperado '->' após nome da variável.");
        Expressao inicializador = expressao();
        return new Comando.VarCmd(nome, inicializador);
    }

    private Comando comandoExpressao() {
        Expressao expr = expressao();
        return new Comando.ExpressaoCmd(expr);
    }

    private Expressao expressao() {
        return logico();
    }

    private Expressao logico() {
        Expressao expr = igualdade();

        while (verificarConsumir(TokenType.E, TokenType.OU)) {
            Token operador = anterior();
            Expressao direita = igualdade();
            expr = new Expressao.Binaria(expr, operador, direita);
        }

        return expr;
    }

    private Expressao igualdade() {
        Expressao expr = comparacao();

        while (verificarConsumir(TokenType.IGUAL, TokenType.DIFERENTE)) {
            Token operador = anterior();
            Expressao direita = comparacao();
            expr = new Expressao.Binaria(expr, operador, direita);
        }

        return expr;
    }

    private Expressao comparacao() {
        Expressao expr = termo();

        while (verificarConsumir(TokenType.MAIOR, TokenType.MAIOR_IGUAL, TokenType.MENOR, TokenType.MENOR_IGUAL)) {
            Token operador = anterior();
            Expressao direita = termo();
            expr = new Expressao.Binaria(expr, operador, direita);
        }

        return expr;
    }

    private Expressao termo() {
        Expressao expr = fator();

        while (verificarConsumir(TokenType.SOMA, TokenType.SUBTRACAO)) {
            Token operador = anterior();
            Expressao direita = fator();
            expr = new Expressao.Binaria(expr, operador, direita);
        }

        return expr;
    }

    private Expressao fator() {
        Expressao expr = unario();

        while (verificarConsumir(TokenType.MULTIPLICACAO, TokenType.DIVISAO)) {
            Token operador = anterior();
            Expressao direita = unario();
            expr = new Expressao.Binaria(expr, operador, direita);
        }

        return expr;
    }

    private Expressao unario() {
        if (verificarConsumir(TokenType.SUBTRACAO, TokenType.NAO_LOGICO)) {
            Token operador = anterior();
            Expressao direita = unario();
            return new Expressao.Binaria(null, operador, direita);
        }
        return primario();
    }

    private Expressao primario() {
        if (verificarConsumir(TokenType.LITERAL_NUMERO, TokenType.LITERAL_TEXTO, TokenType.LITERAL_LOGICO)) {
            return new Expressao.Literal(anterior().lexema);
        }

        if (verificarConsumir(TokenType.IDENTIFICADOR)) {
            return new Expressao.Variavel(anterior().lexema);
        }

        if (verificarConsumir(TokenType.ABRE_PAREN)) {
            Expressao expr = expressao();
            consumir(TokenType.FECHA_PAREN, "Esperado ')' após expressão.");
            return expr;
        }

        throw erro(peek(), "Esperado expressão válida.");
    }

    private Token consumir(TokenType tipo, String mensagem) {
        if (verificar(tipo)) return avancar();
        
        Token token = peek();
        ErroReporter.relatar(token.linha, token.lexema, mensagem);
        throw new ParseException(mensagem);
    }

    private boolean verificar(TokenType... tipos) {
        for (TokenType tipo : tipos) {
            if (!isFim() && peek().tipo == tipo) {
                return true;
            }
        }
        return false;
    }

    private boolean verificarConsumir(TokenType... tipos) {
        for (TokenType tipo : tipos) {
            if (verificar(tipo)) {
                avancar();
                return true;
            }
        }
        return false;
    }

    private Token avancar() {
        if (!isFim()) atual++;
        return anterior();
    }

    private boolean isFim() {
        return peek().tipo == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(atual);
    }

    private Token anterior() {
        return tokens.get(atual - 1);
    }

    private ParseException erro(Token token, String mensagem) {
        ErroReporter.relatar(token.linha, token.lexema, mensagem);
        return new ParseException(mensagem);
    }

    public static class ParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ParseException(String mensagem) {
            super(mensagem);
        }
    }
}