%code imports {
	import java.util.*;
}

%language "Java"
%define parse.error verbose
%define api.parser.class {NanoMorphoParser}
%define api.parser.extends {Compiler}

%token WHILE IF ELSE VAR FUN AND OR RETURN
%token<String> NAME LITERAL OP1 OP2 OP3 OP4 OP5 OP6 OP7

%right RETURN '='
%right OR
%right AND
%right NOT
%left OP1
%right OP2
%left OP3
%left OP4
%left OP5
%left OP6
%left OP7
%right UNOP

%type <Vector<Function>> program
%type <Function> function
%type <Body> body decl
%type <Expr> expr expr_or_decl initialiser
%type <Vector<Expr>> expr_list
%type <Variable> variable
%type <Vector<Variable>> variable_list variable_list_p
%type <Integer> varcount

%%

start
	:	program
		{ generateProgram($program); }
	;

program
	: program function { $1.add($function); $$ = $1; }
	| function { $$ = new Vector<Function>(); ((Vector<Function>)$$).add($function); }

function
	: { st.pushScope(); } FUN NAME '(' parameter_list varcount ')' '=' body
    { $$ = new Function($NAME, $varcount, $body); st.popScope(); }

parameter_list
	: %empty
    | NAME parameter_list_p { st.addVar($NAME); }

parameter_list_p
	: ',' NAME { st.addVar($NAME); } parameter_list_p
	| %empty
	;

varcount : %empty { $$ = st.varCount(); };

body
	: expr { $$ = new Body(new Expr[]{$expr}); }
	| '{' expr_list '}' {
    	$$ = new Body($expr_list.toArray(new Expr[]{}));
    }

expr_list
	: expr_or_decl ';' expr_list
    {
        var res = new Vector<Expr>();
    	res.add($expr_or_decl);
        res.addAll($3);
        $$ = res;
    }
    | %empty { $$ = new Vector<Expr>(); }

expr_or_decl
	: decl { $$ = $decl; }
   | expr { $$ = $expr; }

expr
    : LITERAL { $$ = new Literal($LITERAL); }
    | NAME '=' expr { $$ = new Store(st.findVar($NAME), $3); }
    | RETURN expr { $$ = new Return($2); }

decl
	: VAR variable variable_list
	{
	    var res = new Vector<Variable>();
    	res.add($variable);
        res.addAll($variable_list);
        $$ = new Body(res.toArray(new Expr[]{}));
    }

variable: NAME initialiser { st.addVar($NAME); $$ = new Variable($initialiser); }

initialiser
    : expr { $$ = $expr; }
    | %empty { $$ = null; }

variable_list
    : variable variable_list_p
    {
    	var res = new Vector<Variable>();
        res.add($variable);
        res.addAll($variable_list_p);
        $$ = res;
    }

variable_list_p
	: %empty { $$ = new Vector<Variable>(); }
    | ',' variable variable_list_p
    {
    	var res = new Vector<Variable>();
        res.add($variable);
        res.addAll($3);
        $$ = res;
    }
