%%

%public
%class NanoMorphoLexer
%implements NanoMorpho.Lexer
%unicode
%line
%column
%byaccj

%{

	private static NanoMorphoLexer lexer;
	String lexeme1, lexeme2;
	private static int token1, token2;
	private int line1, colum1, line2, colum2;

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


	public static String advance(){
		String res = lexeme1;
		token1 = token2;
		lexem1 = lexeme2
		if(token1 == EOF) return; // Tíma bundið nafn fyrir end of file
		token2 =lexer.yylex;
	}

	public String getLVal()
	{
		return yylval;
	}

	public void yyerror( String error )
	{
		System.err.println("Error:  "+error);
		System.err.println("Lexeme: "+yylval);
		System.err.println("Line:   "+(yyline+1));
		System.err.println("Column: "+(yycolumn+1));
		System.exit(1);
	}

	public void show( String token, String lexeme )
	{
		System.out.print("Token: "+token);
		System.out.print(", Lexeme: "+lexeme);
		System.out.print(", Line: "+(yyline+1));
		System.out.println(", Column: "+(yycolumn+1));
	}

	public String getTokenName( int token )
	{
		switch( token )
		{
		case LITERAL:   return "LITERAL";
		case NAME:      return "NAME";
		case IF:        return "IF";
		case ELSEIF:    return "ELSEIF";
		case ELSE:      return "ELSE";
		case WHILE:     return "WHILE";
		case YYERRCODE: return "YYERRCODE";
		case '(':       return "'('";
		case ')':       return "')'";
		case '{':       return "'{'";
		case '}':       return "'}'";
		}
		return "unknown";
	}

	public int getLine() { return yyline+1; }
	public int getColumn() { return yycolumn+1; }

	public String over(int token){
	       if(token1!=token) expcted(token);
	       String res = lexeme1;
	       advance();
	       return res;
	}

	public String over(char token){
	       if(token1!=token) expcted(token);
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
