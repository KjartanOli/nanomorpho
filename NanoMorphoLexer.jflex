%%

%public
%class NanoMorphoLexer
%unicode
%line
%column
%byaccj

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
private static int token1, token2;
private static String lexeme1, lexeme2;
private static int line1, column1, line2, column2;

public static void startLexer( String filename ) throws Exception
{
    startLexer(new FileReader(filename));
}

public static void startLexer( Reader r ) throws Exception
{
    lexer = new NanoMorphoLexer(r);
    token1 = lexer.yylex(); // changes lexeme2
    lexeme1 = lexeme2;
    line1 = lexer.yyline;
    column1 = lexer.yycolumn;
    if( token1 == NanoMorphoParser.EOF ) return;
    token2 = lexer.yylex(); // changes lexeme2
    line2 = lexer.yyline;
    column2 = lexer.yycolumn;
}

public static String advance() throws Exception
{
    String res = lexeme1;
    token1 = token2;
    lexeme1 = lexeme2;
    line1 = line2;
    column1 = column2;
    if( token2 == NanoMorphoParser.EOF ) return res;
    token2 = lexer.yylex();
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

public static int getToken()
{
    return token1;
}

public static String getTokenName()
{
    return tokname(token1);
}

public static int getToken2()
{
    return token2;
}

public static String getLexeme()
{
    return lexeme1;
}

public static void expected( int tok )
{
    expected(tokname(tok));
}

public static void expected( char tok )
{
    expected("'"+tok+"'");
}

public static void expected( String tok )
{
    throw new Error("Expected "+tok+", found '"+lexeme1+"' near line "+(line1+1)+", column "+(column1+1));
}

private static String tokname( int tok )
{
    if( tok<1000 ) return "'"+(char)tok+"'";
    switch( tok )
    {
    case NanoMorphoParser.IF:
        return "IF";
    case NanoMorphoParser.ELSE:
        return "ELSE";
    case NanoMorphoParser.ELSIF:
        return "ELSIF";
    case NanoMorphoParser.WHILE:
        return "WHILE";
    case NanoMorphoParser.VAR:
        return "VAR";
    case NanoMorphoParser.RETURN:
        return "RETURN";
    case NanoMorphoParser.NAME:
        return "NAME";
    case NanoMorphoParser.LITERAL:
        return "LITERAL";
    case NanoMorphoParser.OP1:
        return "OP1";
    case NanoMorphoParser.OP2:
        return "OP2";
    case NanoMorphoParser.OP3:
        return "OP3";
    case NanoMorphoParser.OP4:
        return "OP4";
    case NanoMorphoParser.OP5:
        return "OP5";
    case NanoMorphoParser.OP6:
        return "OP6";
    case NanoMorphoParser.OP7:
        return "OP7";
    }
    throw new Error();
}

public static String over( int tok ) throws Exception
{
    if( token1!=tok ) expected(tok);
    String res = lexeme1;
    advance();
    return res;
}

public static String over( char tok ) throws Exception
{
    if( token1!=tok ) expected(tok);
    String res = lexeme1;
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
_NAME=([:letter:]|_)([:letter:]|_DIGIT|_)*
_OPNAME=[:&|<>=+\-*/%!?~\^]+

%%

{_DELIM} {
	yylval = yytext();
	return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
	yylval = yytext();
	return LITERAL;
}

"if" {
	yylval = yytext();
	return IF;
}

"elseif" {
	yylval = yytext();
	return ELSEIF;
}

"else" {
	yylval = yytext();
	return ELSE;
}

"fun" {
	yylval = yytext();
	return FUN;
}

"var" {
	yylval = yytext();
	return VAR;
}

"return" {
	yylval = yytext();
	return RETURN;
}

"while" {
	yylval = yytext();
	return WHILE;
}

{_NAME} {
	yylval = yytext();
	return NAME;
}

{_OPNAME} {
	yylval = yytext();
	return OPNAME;
}

";;;".*$ {
}

[ \t\r\n\f] {
}

. {
	yylval = yytext();
	return YYERRCODE;
}
