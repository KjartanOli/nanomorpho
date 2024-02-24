/**
    Höfundur: Snorri Agnarsson, 2017-2024

    Þennan lesgreini má þýða og keyra með skipununum
        java -jar JFlex-full-1.9.1.jar nanomorpholexer.jflex
        javac NanoMorphoLexer.java NanoMorphoParser.java
        java NanoMorphoParser inntaksskrá
    Einnig má nota forritið 'make', ef viðeigandi 'makefile'
    er til staðar:
        make test
 */

import java.io.*;

%%

%public
%class NanoMorphoLexer
%unicode
%byaccj
%line
%column

%{

// We will have one and only one lexer
private static NanoMorphoLexer lexer;

// We will maintain a data invariant
// that we have information about the
// next token and the following token.
// After initialization and before and
// after each public operation we have:
//  * token1 is the next unprocessed token
//    and lexeme1, line1, and column1 are
//    corresponding to that token.
//  * token2 is the following unprocessed token
//    and lexeme2, line2, and column2 are
//    corresponding to that token.
// This data invariant is established in the
// static initialization method startLexer
// and is maintained each time we use the
// lexer operations to advance in the input.
// A special case is at end of file, in which
// case token2, lexeme2, line2, and column2
// are irrelevant and should be ignored.
// Each lexer action, i.e. a call to lexer.yylex()
// returns the appropriate token and also updates
// the variable lexeme2 to contain the underlying
// lexeme.
private static Token token1, token2;
private static String lexeme2;
private static int line1, column1, line2, column2;

public static void startLexer( String filename ) throws Exception
{
    startLexer(new FileReader(filename));
}

public static void startLexer( Reader r ) throws Exception
{
    lexer = new NanoMorphoLexer(r);
    int type = lexer.yylex();
    String lexeme = lexeme2;
    token1 = new Token(type, lexeme);
    line1 = lexer.yyline;
    column1 = lexer.yycolumn;
    if(type == Token.EOF) return;
    type = lexer.yylex(); // changes lexeme2
    token2 = new Token(type, lexeme2);
    line2 = lexer.yyline;
    column2 = lexer.yycolumn;
}

public static Token advance() throws Exception
{
    var res = token1;
    token1 = token2;
    line1 = line2;
    column1 = column2;
    if(token2.type() == Token.EOF) return res;
    int type = lexer.yylex();
    token2 = new Token(type, lexeme2);
    line2 = lexer.yyline;
    column2 = lexer.yycolumn;
    return res;
}

public static int getLine()
{
    return line1+1;
}

public static int getColumn()
{
    return column1+1;
}

public static Token getToken()
{
    return token1;
}

public static Token getToken2()
{
    return token2;
}

public static void expected( int tok )
{
    expected(Token.name(tok));
}

public static void expected( char tok )
{
    expected(String.format("'%c'", tok));
}

public static void expected( String tok )
{
    throw new Error(String.format("Expected %s, found '%s' near line %d, column %d%n", tok, token1.lexeme(), line1 + 1, column1 + 1));
}

public static Token over( int tok ) throws Exception
{
    if(token1.type() != tok) expected(tok);
    var res = token1;
    advance();
    return res;
}

public static Token over( char tok ) throws Exception
{
    if(token1.type() != tok) expected(tok);
    var res = token1;
    advance();
    return res;
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

  /* Lesgreiningarreglur */

  /* Scanner rules */

{_DELIM} {
    lexeme2 = yytext();
    return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
    lexeme2 = yytext();
    return Token.LITERAL;
}

"if" {
    lexeme2 = yytext();
    return Token.IF;
}

"else" {
    lexeme2 = yytext();
    return Token.ELSE;
}

"while" {
    lexeme2 = yytext();
    return Token.WHILE;
}

"var" {
    lexeme2 = yytext();
    return Token.VAR;
}

"fun" {
	lexeme2 = yytext();
	return Token.FUN;
}

"return" {
    lexeme2 = yytext();
    return Token.RETURN;
}

"and" | "&" {
	lexeme2 = yytext();
	return Token.AND;
}

"or" | "|" {
	lexeme2 = yytext();
	return Token.OR;
}

"not" | "!" {
	lexeme2 = yytext();
	return Token.NOT;
}

{_NAME} {
    lexeme2 = yytext();
    return Token.NAME;
}

{_OPNAME} {
    lexeme2 = yytext();
    switch( yycharat(0) )
    {
    case '?':
    case '~':
    case '^':
        return Token.OP1;
    case ':':
        return Token.OP2;
    case '|':
        return Token.OP3;
    case '&':
        return Token.OP4;
    case '<':
    case '>':
    case '=':
    case '!':
        return Token.OP5;
    case '+':
    case '-':
        return Token.OP6;
    case '*':
    case '/':
    case '%':
        return Token.OP7;
    }
    throw new Error("This can't happen");
}

";;;".*$ {
}

[ \t\r\n\f] {
}

. {
    lexeme2 = yytext();
    return Token.YYERRCODE;
}
