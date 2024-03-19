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

%type <String> op
%type <Vector<Function>> program
%type <Function> function
%type <Body> body decl
%type <Expr> stmt expr binop unop initialiser
%type <Expr[]> optexprs
%type <Vector<Expr>> stmt_list optexprsp
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
	| { st.pushScope(); }'{' stmt_list '}' {
        st.popScope();
    	$$ = new Body($stmt_list.toArray(new Expr[]{}));
    }

stmt_list
	: stmt ';' stmt_list
    {
        var res = new Vector<Expr>();
    	res.add($stmt);
        res.addAll($3);
        $$ = res;
    }
    | %empty { $$ = new Vector<Expr>(); }

stmt
    : RETURN expr { $$ = new Return($2); }
    | expr
    | decl { $$ = $decl; }

expr
    : LITERAL { $$ = new Literal($LITERAL); }
    | NAME { $$ = new Fetch(st.findVar($NAME)); }
    | NAME '=' expr { $$ = new Store(st.findVar($NAME), $3); }
    | NAME '(' optexprs ')' { $$ = new Call($NAME, $optexprs); }
    | unop
    | binop
    ;

optexprs
   : %empty { $$ = new Expr[]{}; }
   | expr optexprsp {
       var res = new Vector<Expr>();
       res.add($expr);
       res.addAll($optexprsp);
       $$ = res.toArray(new Expr[]{});
   }
   ;

optexprsp
    : %empty { $$ = new Vector<Expr>(); }
    | ',' expr optexprsp {
       var res = new Vector<Expr>();
       res.add($expr);
       res.addAll($3);
       $$ = res;
    }
    ;

unop
    : op expr %prec UNOP { $$ = new Call($1, new Expr[]{$2}); }
    | NOT expr { $$ = new Not($2); }
    ;

binop
    : expr AND expr { $$ = new And($1, $3); }
    | expr OR expr { $$ = new Or($1, $3); }
    | expr OP1 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP2 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP3 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP4 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP5 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP6 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    | expr OP7 expr { $$ = new Call($2, new Expr[]{$1, $3}); }
    ;

op: OP1 | OP2 | OP3 | OP4 | OP5 | OP6 | OP7 ;


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
    : '=' expr { $$ = $expr; }
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
