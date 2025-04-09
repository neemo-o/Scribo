package application.lexer;

public class Token {
    public final TokenType tipo;
    public final String lexema;
    public final int linha;

    public Token(TokenType tipo, String lexema, int linha) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
    }

    @Override
    public String toString() {
        return tipo + " '" + lexema + "' (linha " + linha + ")";
    }
}
