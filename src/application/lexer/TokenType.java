package application.lexer;

public enum TokenType {
    // Símbolos
    ABRE_PAREN, FECHA_PAREN,
    ABRE_CHAVE, FECHA_CHAVE,
    VIRGULA, ATRIBUICAO,
    SOMA, SUBTRACAO, MULTIPLICACAO, DIVISAO,

    // Comparação
    IGUAL, DIFERENTE,
    MAIOR, MAIOR_IGUAL,
    MENOR, MENOR_IGUAL, PONTO_VIRGULA,

    // Literais
    IDENTIFICADOR,
    LITERAL_NUMERO,
    LITERAL_TEXTO,
    LITERAL_LOGICO,

    // Palavras-chave
    MOSTRAR, FORMATAR, LER,
    SE, SENAO, ENQUANTO, PARA,
    FUNC, 

    // Tipos primitivos
    NUMERO, TEXTO, LOGICO, LISTA,

    // Operadores lógicos
    E, OU, NAO_LOGICO,

    // Outros
    ERRO,
    EOF
}