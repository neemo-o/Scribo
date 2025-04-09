package application.parser;

import application.lexer.Token;


public abstract class Expressao {
    
   
    public interface Visitor<R> {
        R visitarExpressaoBinaria(Binaria expr);
        R visitarExpressaoLiteral(Literal expr);
        R visitarExpressaoVariavel(Variavel expr);
    }
    
    
    public abstract <R> R aceitar(Visitor<R> visitor);
    
   
    public static class Binaria extends Expressao {
        public final Expressao esquerda;  
        public final Token operador;      
        public final Expressao direita;   
        public Binaria(Expressao esquerda, Token operador, Expressao direita) {
            this.esquerda = esquerda;
            this.operador = operador;
            this.direita = direita;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarExpressaoBinaria(this);
        }
    }
   
    public static class Literal extends Expressao {
        public final String valor;  // Valor literal como string
        
        public Literal(String valor) {
            this.valor = valor;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarExpressaoLiteral(this);
        }
    }
    
   
    public static class Variavel extends Expressao {
        public final String nome;  
        
        public Variavel(String nome) {
            this.nome = nome;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarExpressaoVariavel(this);
        }
    }
}