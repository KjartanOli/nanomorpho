/*
 * Bison driver for NanoMorpho lexer.
 */

%language "Java"
%define parse.error verbose
%define api.parser.class {NanoMorpho}
%define api.parser.public

%code imports {
	import java.util.*;
	import java.io.*;
}

%code {
	static private NanoMorphoLexer l;
	
	public static void main( String args[] )
	  	throws IOException
	{
		l = new NanoMorphoLexer(new FileReader(args[0]));
		NanoMorpho p = new NanoMorpho(l);
		p.parse();
	}	
}

%token <String> LITERAL VAR WHILE RETURN NAME OPNAME IF ELSEIF ELSE DEFINE '(' ')'
%token YYERRCODE

%%

program
	:	func program
	|	%empty
	;

expr : NAME { l.show("EXPRESSION", $1); }
		| 		RETURN expr { l.show("Return EXPRESSION", $1); }
		|		NAME '=' expr
		|		NAME '(' exprList')'
		| 		OPNAME expr { l.show("Unary OP expression", $1); }
		| 		expr OPNAME expr { l.show("Binop expr", $2); }
		| 		LITERAL { l.show("literal expr", $LITERAL); }
		| '(' 	expr ')' { l.show("parenth expr", $1); }
		| 		ifexpr { l.show("if expr", ""); }
		|		"while" '(' expr ')' body { l.show("while", ""); }
	;

namelist : NAME namelistP { l.show("NAME", $NAME); };
namelistP : ',' NAME namelistP { l.show("NAME", $NAME); } | %empty;

decl : VAR namelist { l.show("DECL", $1); };

ifexpr : IF '(' expr ')' body elsif { l.show("IF", $IF); };

elsif : ELSEIF '(' expr ')' body elsif { l.show("ELSEIF", $ELSEIF); } | else;

 else : ELSE body { l.show("ELSE", $ELSE); } | %empty;

decllist : decl ';' decllist | %empty;

exprlist : expr ';' exprlist { l.show("Expression", ""); } | %empty;

func : NAME '(' paramlist ')' funcBody { l.show("Function", ""); };

paramlist : NAME paramlistP { l.show("PARAM", $NAME); }
		| %empty { l.show("Empty param list", ""); };

paramlistP : ',' NAME paramlistP { l.show("PARAM", $NAME); } | %empty;

funcBody : '{' decllist exprlist '}';

body : '{' expr ';' exprlist '}';
