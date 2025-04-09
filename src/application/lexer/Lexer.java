package application.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
	private final String fonte;
	private final List<Token> tokens = new ArrayList<>();

	private int inicio = 0;
	private int atual = 0;
	private int linha = 1;

	// PALAVRAS-CHAVES
	private static final Map<String, TokenType> palavrasReservadas = new HashMap<>();

	static {
		palavrasReservadas.put("mostrar", TokenType.MOSTRAR);
		palavrasReservadas.put("formatar", TokenType.FORMATAR);
		palavrasReservadas.put("ler", TokenType.LER);
		palavrasReservadas.put("se", TokenType.SE);
		palavrasReservadas.put("senao", TokenType.SENAO);
		palavrasReservadas.put("enquanto", TokenType.ENQUANTO);
		palavrasReservadas.put("para", TokenType.PARA);
		palavrasReservadas.put("func", TokenType.FUNC);
		palavrasReservadas.put("sim", TokenType.LITERAL_LOGICO);
		palavrasReservadas.put("nao", TokenType.LITERAL_LOGICO);
		palavrasReservadas.put("e", TokenType.E);
		palavrasReservadas.put("ou", TokenType.OU);
		palavrasReservadas.put("nao_logico", TokenType.NAO_LOGICO);
		palavrasReservadas.put("numero", TokenType.NUMERO);
		palavrasReservadas.put("texto", TokenType.TEXTO);
		palavrasReservadas.put("logico", TokenType.LOGICO);
		palavrasReservadas.put("lista", TokenType.LISTA);
	}

	public Lexer(String fonte) {
		this.fonte = fonte;
	}

	public List<Token> escanearTokens() {
		while (!isFim()) {
			inicio = atual;
			escanearToken();
		}

		tokens.add(new Token(TokenType.EOF, "", linha));
		return tokens;
	}

	private void escanearToken() {
		char c = avancar();

		switch (c) {
		case ' ':
		case '\r':
		case '\t':
			break;
		case '\n':
			linha++;
			break;
		case '(':
			adicionarToken(TokenType.ABRE_PAREN);
			break;
		case ')':
			adicionarToken(TokenType.FECHA_PAREN);
			break;
		case '{':
			adicionarToken(TokenType.ABRE_CHAVE);
			break;
		case '}':
			adicionarToken(TokenType.FECHA_CHAVE);
			break;
		case ',':
			adicionarToken(TokenType.VIRGULA);
			break;
		case '+':
			adicionarToken(TokenType.SOMA);
			break;
		case '-':
			if (corresponde('>')) {
				adicionarToken(TokenType.ATRIBUICAO);
			} else {
				adicionarToken(TokenType.SUBTRACAO);
			}
			break;
		case '*':
			adicionarToken(TokenType.MULTIPLICACAO);
			break;
		case '/':
			if (peek() == '#') {
				while (peek() != '\n' && !isFim())
					avancar();
			} else {
				adicionarToken(TokenType.DIVISAO);
			}
			break;
		case '=':
			adicionarToken(corresponde('=') ? TokenType.IGUAL : TokenType.ERRO);
			break;
		case '!':
			adicionarToken(corresponde('=') ? TokenType.DIFERENTE : TokenType.ERRO);
			break;
		case '>':
			adicionarToken(corresponde('=') ? TokenType.MAIOR_IGUAL : TokenType.MAIOR);
			break;
		case '<':
			adicionarToken(corresponde('=') ? TokenType.MENOR_IGUAL : TokenType.MENOR);
			break;
		case '"':
			string();
			break;
		case '#':
			while (peek() != '\n' && !isFim())
				avancar();
			break;
		default:
			if (ehDigito(c)) {
				numero();
			} else if (ehLetra(c)) {
				identificador();
			} else {
				adicionarToken(TokenType.ERRO);
			}
			break;
		}
	}

	private void string() {
		while (peek() != '"' && !isFim()) {
			if (peek() == '\n')
				linha++;
			avancar();
		}

		if (isFim()) {
			adicionarToken(TokenType.ERRO);
			return;
		}

		avancar(); // fecha aspas
		adicionarToken(TokenType.LITERAL_TEXTO);
	}

	private void numero() {
		while (ehDigito(peek()))
			avancar();

		if (peek() == '.' && ehDigito(peekProximo())) {
			avancar();
			while (ehDigito(peek()))
				avancar();
		}

		adicionarToken(TokenType.LITERAL_NUMERO);
	}

	private void identificador() {
		while (ehLetraOuDigito(peek()))
			avancar();
		String texto = fonte.substring(inicio, atual);
		TokenType tipo = palavrasReservadas.getOrDefault(texto, TokenType.IDENTIFICADOR);
		adicionarToken(tipo);
	}

	private boolean corresponde(char esperado) {
		if (isFim())
			return false;
		if (fonte.charAt(atual) != esperado)
			return false;
		atual++;
		return true;
	}

	private char avancar() {
		return fonte.charAt(atual++);
	}

	private char peek() {
		if (isFim())
			return '\0';
		return fonte.charAt(atual);
	}

	private char peekProximo() {
		if (atual + 1 >= fonte.length())
			return '\0';
		return fonte.charAt(atual + 1);
	}

	private boolean ehDigito(char c) {
		return c >= '0' && c <= '9';
	}

	private boolean ehLetra(char c) {
		return Character.isLetter(c) || c == '_';
	}

	private boolean ehLetraOuDigito(char c) {
		return ehLetra(c) || ehDigito(c);
	}

	private boolean isFim() {
		return atual >= fonte.length();
	}

	private void adicionarToken(TokenType tipo) {
		String texto = fonte.substring(inicio, atual);
		tokens.add(new Token(tipo, texto, linha));
	}
}
