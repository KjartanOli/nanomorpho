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
%type <String> token

%%

program
	:	decl program
	|	expr program
	|	%empty
	;

token
	:	LITERAL	{ l.show("LITERAL",$LITERAL); 	}
	|	NAME	{ l.show("NAME",$NAME); 		}
	|	OPNAME { l.show("OPNAME", $OPNAME); }
	|	IF		{ l.show("IF",$IF); 			}
	|	DEFINE	{ l.show("DEFINE",$DEFINE); 	}
	|	'('		{ l.show("'('",$1); 			}
	|	')'		{ l.show("')'",$1); 			}
	;

expr : NAME { l.show("EXPRESSION", $1); } ;

namelist : NAME namelistP ;
namelistP
	: ',' NAME namelistP
	| %empty
	;

decl
	: VAR namelist { l.show("DECL", $1); };

ifexpr
	: IF '(' expr ')' body ifexprP
	;

ifexprP
	: ElSIF '(' expr ')' body
	| ELSE body
	| %empty
	;

decl
	: VAR namelist { l.show("DECL", $1); };

func
	: NAME '(' namelist ')';

body
	: '{' EXPR ';' EXPR '}';
%%
