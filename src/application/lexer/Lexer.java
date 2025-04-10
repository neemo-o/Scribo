package application.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lexer {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private int column = 1;
	private boolean hadError = false;
	private boolean inStatement = false;
	private boolean expectingSemicolon = false;
	private TokenType currentType = null;
	private Object currentLiteral = null;
	private final Set<String> declaredIdentifiers = new HashSet<>();

	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		// Palavras-chave
		keywords.put("mostrar", TokenType.MOSTRAR);
		keywords.put("formatar", TokenType.FORMATAR);
		keywords.put("ler", TokenType.LER);
		keywords.put("se", TokenType.SE);
		keywords.put("senao", TokenType.SENAO);
		keywords.put("enquanto", TokenType.ENQUANTO);
		keywords.put("para", TokenType.PARA);
		keywords.put("func", TokenType.FUNC);
		keywords.put("retornar", TokenType.RETORNAR);
		
		// Tipos primitivos
		keywords.put("int", TokenType.NUMERO);
		keywords.put("float", TokenType.NUMERO);
		keywords.put("double", TokenType.DOUBLE);
		keywords.put("char", TokenType.TEXTO);
		keywords.put("string", TokenType.TEXTO);
		keywords.put("bool", TokenType.LOGICO);
		keywords.put("lista", TokenType.LISTA);
		keywords.put("long", TokenType.LONG);
		keywords.put("short", TokenType.SHORT);
		keywords.put("unsigned", TokenType.UNSIGNED);
		keywords.put("void", TokenType.VOID);
		
		// Valores booleanos
		keywords.put("verdadeiro", TokenType.LITERAL_LOGICO);
		keywords.put("falso", TokenType.LITERAL_LOGICO);
		
		// Operadores lógicos
		keywords.put("e", TokenType.E);
		keywords.put("ou", TokenType.OU);
		keywords.put("nao", TokenType.NAO_LOGICO);
	}

	public Lexer(String source) {
		this.source = source;
	}

	public List<Token> scanTokens() {
		reset();
		while (!isAtEnd() && !hadError) {
			start = current;
			scanToken();
		}

		// Verifica se há uma declaração não finalizada
		if (inStatement && !hadError) {
			error("Declaração não finalizada");
		}

		if (!hadError) {
			tokens.add(new Token(TokenType.EOF, "", null, line));
		}
		return tokens;
	}

	public void reset() {
		tokens.clear();
		declaredIdentifiers.clear();
		start = 0;
		current = 0;
		line = 1;
		column = 1;
		hadError = false;
		inStatement = false;
		expectingSemicolon = false;
		currentType = null;
		currentLiteral = null;
	}

	public boolean hadError() {
		return hadError;
	}

	private void error(String message) {
		hadError = true;
		System.err.println("Erro na linha " + line + ", coluna " + column + ": " + message);
	}

	private void scanToken() {
		if (hadError) {
			return; // Para de gerar tokens se houver erro
		}
		
		char c = advance();
		switch (c) {
			case '(': 
				addToken(TokenType.ABRE_PAREN);
				inStatement = true;
				break;
			case ')': 
				addToken(TokenType.FECHA_PAREN);
				break;
			case '{': 
				addToken(TokenType.ABRE_CHAVE);
				inStatement = true;
				break;
			case '}': 
				addToken(TokenType.FECHA_CHAVE);
				break;
			case ',': 
				addToken(TokenType.VIRGULA);
				break;
			case ';': 
				addToken(TokenType.PONTO_VIRGULA);
				inStatement = false;
				expectingSemicolon = false;
				checkTypeCompatibility();
				currentType = null;
				currentLiteral = null;
				break;
			case '+':
				if (match('+')) {
					addToken(TokenType.INCREMENTO);
					inStatement = true;
				} else if (match('=')) {
					addToken(TokenType.SOMA_ATRIBUICAO);
					inStatement = true;
				} else {
					addToken(TokenType.SOMA);
					inStatement = true;
				}
				break;
			case '-':
				if (match('-')) {
					addToken(TokenType.DECREMENTO);
					inStatement = true;
				} else if (match('=')) {
					addToken(TokenType.SUBTRACAO_ATRIBUICAO);
					inStatement = true;
				} else {
					addToken(TokenType.SUBTRACAO);
					inStatement = true;
				}
				break;
			case '*':
				if (match('=')) {
					addToken(TokenType.MULTIPLICACAO_ATRIBUICAO);
					inStatement = true;
				} else {
					addToken(TokenType.MULTIPLICACAO);
					inStatement = true;
				}
				break;
			case '/':
				if (match('=')) {
					addToken(TokenType.DIVISAO_ATRIBUICAO);
					inStatement = true;
				} else {
					addToken(TokenType.DIVISAO);
					inStatement = true;
				}
				break;
			case '%':
				if (match('=')) {
					addToken(TokenType.MODULO_ATRIBUICAO);
					inStatement = true;
				} else {
					addToken(TokenType.MODULO);
					inStatement = true;
				}
				break;
			case '=': 
				error("Operador '=' não é válido. Use ':=' para atribuição.");
				addToken(TokenType.ERRO);
				break;
			case '!': 
				addToken(match('=') ? TokenType.DIFERENTE : TokenType.ERRO);
				break;
			case '>': 
				addToken(match('=') ? TokenType.MAIOR_IGUAL : TokenType.MAIOR);
				break;
			case '<': 
				addToken(match('=') ? TokenType.MENOR_IGUAL : TokenType.MENOR);
				break;
			case ':': 
				if (match('=')) {
					addToken(TokenType.ATRIBUICAO);
					// Adiciona o identificador ao conjunto de identificadores declarados
					if (!tokens.isEmpty()) {
						Token lastToken = tokens.get(tokens.size() - 1);
						if (lastToken.getType() == TokenType.IDENTIFICADOR) {
							declaredIdentifiers.add(lastToken.getLexeme());
						}
					}
				} else {
					error("Operador ':=' esperado");
				}
				break;
			case ' ':
			case '\r':
			case '\t':
				column++;
				break;
			case '\n':
				line++;
				column = 1;
				if (inStatement && expectingSemicolon) {
					error("Ponto e vírgula esperado no final da linha");
				}
				break;
			case '"': 
				string();
				break;
			case '#':
				if (match('[')) {
					// Comentário de bloco
					while (!(peek() == ']' && peekNext() == '#') && !isAtEnd()) {
						if (peek() == '\n') line++;
						advance();
					}
					if (isAtEnd()) {
						error("Comentário de bloco não fechado");
						return;
					}
					advance(); // Consome o ]
					advance(); // Consome o #
				} else {
					// Comentário de linha
					while (peek() != '\n' && !isAtEnd()) advance();
				}
				break;
			case '[':
				if (currentType == TokenType.LISTA) {
					addToken(TokenType.ABRE_COLCHETE);
					inStatement = true;
					currentType = null;
					// Inicia contagem de colchetes abertos
					int colchetesAbertos = 1;
					while (!isAtEnd() && colchetesAbertos > 0) {
						if (peek() == '[') {
							colchetesAbertos++;
							advance();
						} else if (peek() == ']') {
							colchetesAbertos--;
							advance();
						} else if (peek() == '#' && peekNext() == '[') {
							// Ignora comentários de bloco dentro de listas
							advance(); // Consome o #
							advance(); // Consome o [
							while (!(peek() == ']' && peekNext() == '#') && !isAtEnd()) {
								if (peek() == '\n') line++;
								advance();
							}
							if (isAtEnd()) {
								error("Comentário de bloco não fechado dentro de lista");
								return;
							}
							advance(); // Consome o ]
							advance(); // Consome o #
						} else {
							advance();
						}
					}
					if (colchetesAbertos > 0) {
						error("Lista não fechada corretamente");
					}
				} else {
					error("Uso incorreto de colchetes");
				}
				break;
			case ']':
				if (inStatement) {
					addToken(TokenType.FECHA_COLCHETE);
				} else {
					error("Uso incorreto de colchetes");
				}
				break;
			case '\'': 
				processChar();
				break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					error("Caractere inesperado: '" + c + "'");
					addToken(TokenType.ERRO);
				}
				break;
		}
	}

	private void identifier() {
		while (isAlphaNumeric(peek())) advance();

		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		
		if (type == null) {
			// É um identificador
			type = TokenType.IDENTIFICADOR;
			
			// Verifica se é uma palavra solta
			if (!isValidIdentifierContext()) {
				// Lista de palavras-chave que podem aparecer soltas
				if (!text.equals("func") && !text.equals("se") && 
					!text.equals("senao") && !text.equals("enquanto") && 
					!text.equals("para") && !text.equals("retornar") &&
					!text.equals("mostrar") && !text.equals("ler") &&
					!text.equals("formatar")) {
					error("Palavra solta encontrada: '" + text + "'");
					return;
				}
			}
		} else if (type == TokenType.NUMERO || type == TokenType.TEXTO || 
				   type == TokenType.LOGICO || type == TokenType.LISTA ||
				   type == TokenType.LONG || type == TokenType.SHORT ||
				   type == TokenType.UNSIGNED || type == TokenType.DOUBLE ||
				   type == TokenType.VOID) {
			// É um tipo primitivo
			inStatement = true;
			currentType = type;
		}
		
		addToken(type);
	}

	private boolean isValidIdentifierContext() {
		// Verifica se o identificador está em um contexto válido
		if (tokens.isEmpty()) return false;
		
		Token lastToken = tokens.get(tokens.size() - 1);
		TokenType lastType = lastToken.getType();
		
		// Contextos válidos para um identificador:
		// 1. Após um operador
		// 2. Após uma atribuição
		// 3. Após um ponto e vírgula
		// 4. Após um tipo primitivo
		// 5. Após um identificador declarado anteriormente
		// 6. Após operadores lógicos
		return isAfterOperator() || 
			   lastType == TokenType.ATRIBUICAO ||
			   lastType == TokenType.PONTO_VIRGULA ||
			   lastType == TokenType.NUMERO ||
			   lastType == TokenType.TEXTO ||
			   lastType == TokenType.LOGICO ||
			   lastType == TokenType.LISTA ||
			   lastType == TokenType.LONG ||
			   lastType == TokenType.SHORT ||
			   lastType == TokenType.UNSIGNED ||
			   lastType == TokenType.DOUBLE ||
			   lastType == TokenType.E ||
			   lastType == TokenType.OU ||
			   lastType == TokenType.NAO_LOGICO ||
			   (lastType == TokenType.IDENTIFICADOR && declaredIdentifiers.contains(lastToken.getLexeme()));
	}

	private boolean isInExpression() {
		// Verifica se estamos dentro de uma expressão (após um operador ou atribuição)
		if (tokens.isEmpty()) return false;
		Token lastToken = tokens.get(tokens.size() - 1);
		TokenType lastTokenType = lastToken.getType();
		return lastTokenType == TokenType.ATRIBUICAO ||
			   lastTokenType == TokenType.SOMA ||
			   lastTokenType == TokenType.SUBTRACAO ||
			   lastTokenType == TokenType.MULTIPLICACAO ||
			   lastTokenType == TokenType.DIVISAO ||
			   lastTokenType == TokenType.MODULO ||
			   lastTokenType == TokenType.INCREMENTO ||
			   lastTokenType == TokenType.DECREMENTO ||
			   lastTokenType == TokenType.SOMA_ATRIBUICAO ||
			   lastTokenType == TokenType.SUBTRACAO_ATRIBUICAO ||
			   lastTokenType == TokenType.MULTIPLICACAO_ATRIBUICAO ||
			   lastTokenType == TokenType.DIVISAO_ATRIBUICAO ||
			   lastTokenType == TokenType.MODULO_ATRIBUICAO;
	}

	private boolean isAfterOperator() {
		if (tokens.isEmpty()) return false;
		Token lastToken = tokens.get(tokens.size() - 1);
		TokenType lastTokenType = lastToken.getType();
		return lastTokenType == TokenType.SOMA ||
			   lastTokenType == TokenType.SUBTRACAO ||
			   lastTokenType == TokenType.MULTIPLICACAO ||
			   lastTokenType == TokenType.DIVISAO ||
			   lastTokenType == TokenType.MODULO ||
			   lastTokenType == TokenType.INCREMENTO ||
			   lastTokenType == TokenType.DECREMENTO ||
			   lastTokenType == TokenType.SOMA_ATRIBUICAO ||
			   lastTokenType == TokenType.SUBTRACAO_ATRIBUICAO ||
			   lastTokenType == TokenType.MULTIPLICACAO_ATRIBUICAO ||
			   lastTokenType == TokenType.DIVISAO_ATRIBUICAO ||
			   lastTokenType == TokenType.MODULO_ATRIBUICAO ||
			   lastTokenType == TokenType.ATRIBUICAO;
	}

	private boolean isAfterIdentifier() {
		if (tokens.isEmpty()) return false;
		Token lastToken = tokens.get(tokens.size() - 1);
		TokenType lastTokenType = lastToken.getType();
		return lastTokenType == TokenType.IDENTIFICADOR;
	}

	private void number() {
		boolean isFloat = false;
		while (isDigit(peek())) advance();

		if (peek() == '.' && isDigit(peekNext())) {
			isFloat = true;
			advance();
			while (isDigit(peek())) advance();
		}

		String numberStr = source.substring(start, current);
		try {
			if (isFloat) {
				currentLiteral = Double.parseDouble(numberStr);
			} else {
				currentLiteral = Integer.parseInt(numberStr);
			}
			addToken(TokenType.LITERAL_NUMERO, currentLiteral);
		} catch (NumberFormatException e) {
			error("Número inválido: " + numberStr);
		}
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') {
				line++;
				column = 1;
			}
			advance();
		}

		if (isAtEnd()) {
			error("String não fechada");
			addToken(TokenType.ERRO);
			return;
		}

		advance();
		String value = source.substring(start + 1, current - 1);
		currentLiteral = value;
		addToken(TokenType.LITERAL_TEXTO, value);
	}

	private void processChar() {
		if (isAtEnd()) {
			error("Caractere não fechado");
			return;
		}

		char value = advance();
		
		// Verifica se é um caractere de escape
		if (value == '\\') {
			if (isAtEnd()) {
				error("Sequência de escape incompleta");
				return;
			}
			value = advance();
			switch (value) {
				case 'n': value = '\n'; break;
				case 't': value = '\t'; break;
				case 'r': value = '\r'; break;
				case '\\': value = '\\'; break;
				case '\'': value = '\''; break;
				default:
					error("Sequência de escape inválida: \\" + value);
					return;
			}
		}

		if (peek() != '\'') {
			error("Caractere deve conter exatamente um caractere");
			return;
		}

		advance(); // Consome a aspas simples de fechamento
		currentLiteral = String.valueOf(value);
		addToken(TokenType.LITERAL_TEXTO, currentLiteral);
	}

	private void checkTypeCompatibility() {
		if (currentType != null && currentLiteral != null) {
			switch (currentType) {
				case NUMERO:
				case LONG:
				case SHORT:
				case UNSIGNED:
				case DOUBLE:
					if (!(currentLiteral instanceof Number)) {
						error("Tipo incompatível: esperado número, encontrado " + 
							(currentLiteral instanceof String ? "string" : "booleano"));
						hadError = true;
					}
					break;
				case TEXTO:
					if (!(currentLiteral instanceof String)) {
						error("Tipo incompatível: esperado string, encontrado " + 
							(currentLiteral instanceof Number ? "número" : "booleano"));
						hadError = true;
					}
					break;
				case LOGICO:
					if (!(currentLiteral instanceof Boolean)) {
						error("Tipo incompatível: esperado booleano, encontrado " + 
							(currentLiteral instanceof String ? "string" : "número"));
						hadError = true;
					}
					break;
				case LISTA:
					// Listas podem conter qualquer tipo, então não verificamos
					break;
				case VOID:
					error("Tipo void não pode ser usado em declarações");
					hadError = true;
					break;
				default:
					break;
			}
		}
	}

	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;
		current++;
		column++;
		return true;
	}

	private char peek() {
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}

	private char peekNext() {
		if (current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
			   (c >= 'A' && c <= 'Z') ||
			   c == '_';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private char advance() {
		column++;
		return source.charAt(current++);
	}

	private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
}
