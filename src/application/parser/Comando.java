package application.parser;

import application.lexer.Token;
import java.util.List;

public abstract class Comando {
    public interface Visitor<R> {
        R visitarComandoExpressao(ExpressaoCmd cmd);
        R visitarComandoMostrar(MostrarCmd cmd);
        R visitarComandoVar(VarCmd cmd);
        R visitarComandoBloco(BlocoCmd cmd);
        R visitarComandoSe(SeCmd cmd);
        R visitarComandoEnquanto(EnquantoCmd cmd);
        R visitarComandoPara(ParaCmd cmd);
        R visitarComandoLer(LerCmd cmd);
    }
    
    public abstract <R> R aceitar(Visitor<R> visitor);
    
    public static class ExpressaoCmd extends Comando {
        public final Expressao expressao;
        
        public ExpressaoCmd(Expressao expressao) {
            this.expressao = expressao;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoExpressao(this);
        }
    }
    
    public static class MostrarCmd extends Comando {
        public final Expressao expressao;
        
        public MostrarCmd(Expressao expressao) {
            this.expressao = expressao;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoMostrar(this);
        }
    }
    
    public static class VarCmd extends Comando {
        public final Token nome;             
        public final Expressao inicializador; 
        
        public VarCmd(Token nome, Expressao inicializador) {
            this.nome = nome;
            this.inicializador = inicializador;
        }
        
        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoVar(this);
        }
    }

    public static class BlocoCmd extends Comando {
        public final List<Comando> comandos;

        public BlocoCmd(List<Comando> comandos) {
            this.comandos = comandos;
        }

        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoBloco(this);
        }
    }

    public static class SeCmd extends Comando {
        public final Expressao condicao;
        public final Comando entaoRamo;
        public final Comando senaoRamo;

        public SeCmd(Expressao condicao, Comando entaoRamo, Comando senaoRamo) {
            this.condicao = condicao;
            this.entaoRamo = entaoRamo;
            this.senaoRamo = senaoRamo;
        }

        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoSe(this);
        }
    }

    public static class EnquantoCmd extends Comando {
        public final Expressao condicao;
        public final Comando corpo;

        public EnquantoCmd(Expressao condicao, Comando corpo) {
            this.condicao = condicao;
            this.corpo = corpo;
        }

        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoEnquanto(this);
        }
    }

    public static class ParaCmd extends Comando {
        public final Token variavel;
        public final Expressao inicio;
        public final Expressao fim;
        public final Expressao incremento;
        public final Comando corpo;

        public ParaCmd(Token variavel, Expressao inicio, Expressao fim, 
                       Expressao incremento, Comando corpo) {
            this.variavel = variavel;
            this.inicio = inicio;
            this.fim = fim;
            this.incremento = incremento;
            this.corpo = corpo;
        }

        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoPara(this);
        }
    }

    public static class LerCmd extends Comando {
        public final Token variavel;

        public LerCmd(Token variavel) {
            this.variavel = variavel;
        }

        @Override
        public <R> R aceitar(Visitor<R> visitor) {
            return visitor.visitarComandoLer(this);
        }
    }
}