package application.interpreter;

import application.parser.Comando;
import application.parser.Expressao;

interface Visitor<R> {
    
    R visitarExpressaoBinaria(Expressao.Binaria expr); 
    R visitarExpressaoLiteral(Expressao.Literal expr); 
    R visitarExpressaoVariavel(Expressao.Variavel expr);
    
  
    R visitarComandoExpressao(Comando.ExpressaoCmd cmd);
    R visitarComandoMostrar(Comando.MostrarCmd cmd);    
    R visitarComandoVar(Comando.VarCmd cmd);
}