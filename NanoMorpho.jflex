import java.io.*;

%%

%public
%class NanoMorphoLexer
%implements NanoMorphoParser.Lexer
%unicode
%byaccj
%line
%column

%{

public String getLVal() { return yytext(); }
public void yyerror( String error )
{
	System.err.println("Error:  "+error);
	System.err.println("Lexeme: "+yytext());
	System.err.println("Line:   "+(yyline+1));
	System.err.println("Column: "+(yycolumn+1));
	System.exit(1);
}

%}

  /* Reglulegar skilgreiningar */

  /* Regular definitions */

_DIGIT=[0-9]
_FLOAT={_DIGIT}+\.{_DIGIT}+([eE][+-]?{_DIGIT}+)?
_INT={_DIGIT}+
_STRING=\"([^\"\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|\\[0-7][0-7]|\\[0-7])*\"
_CHAR=\'([^\'\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|(\\[0-7][0-7])|(\\[0-7]))\'
_DELIM=[(){},;=]
_NAME=(_|[:jletter:])(_|[:jletter:]|{_DIGIT})*
_OPNAME=[\+\-*/!%&=><\:\^\~&|?]+

%%

{_DELIM} {
    return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
    return LITERAL;
}

"if" {
    return IF;
}

"else" {
    return ELSE;
}

"while" {
    return WHILE;
}

"var" {
    return VAR;
}

"fun" {
	return FUN;
}

"return" {
    return RETURN;
}

"and" | "&" {
	return AND;
}

"or" | "|" {
	return OR;
}

"not" | "!" {
	return NOT;
}

{_NAME} {
    return NAME;
}

{_OPNAME} {
    switch( yycharat(0) )
    {
    case '?':
    case '~':
    case '^':
        return OP1;
    case ':':
        return OP2;
    case '|':
        return OP3;
    case '&':
        return OP4;
    case '<':
    case '>':
    case '=':
    case '!':
        return OP5;
    case '+':
    case '-':
        return OP6;
    case '*':
    case '/':
    case '%':
        return OP7;
    }
    throw new Error("This can't happen");
}

";;;".*$ {
}

[ \t\r\n\f] {
}

. {
    return YYerror;
}
