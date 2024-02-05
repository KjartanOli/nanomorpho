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

%token <String> LITERAL VAR WHILE RETURN NAME OPNAME IF ELSEIF ELSE '(' ')' '{''}' '=' ',' ';'
%token YYERRCODE
%type <String> token

%%

program
	:	token program
	|	%empty
	;

token
	:	LITERAL { l.show("LITERAL",$LITERAL); 	}
		|	NAME { l.show("NAME",$NAME); 		}
		| 	VAR { l.show("VAR", $VAR); }
		|	RETURN { l.show("RETURN", $RETURN); }
		|	IF { l.show("IF",$IF); 			}
		|	ELSEIF { l.show("ELSEIF", $ELSEIF); }
		|	ELSE { l.show("ELSE", $ELSE); }
		|	OPNAME { l.show("OPNAME",$OPNAME); 	}
		|	WHILE { l.show("WHILE", $WHILE); }
		|	',' { l.show("','", $1); }
		|	';' {l.show("';'", $1); }
		|	'=' {l.show("'='", $1); }
		|	'('		{l.show("'('",$1); 			}
		|	')'		{l.show("')'",$1); 			}
		|	'{'		{l.show("'{'",$1); 			}
		|	'}'		{l.show("'}'",$1); 			}
	;
