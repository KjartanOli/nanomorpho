%code imports {
	import java.util.*;
}

%language "Java"
%define parse.error verbose
%define api.parser.class {NanoMorphoParser}
%define api.parser.extends {Compiler}

%token WHILE FOR IF ELSE COND MATCH FAT_ARROW VAR FUN AND OR RETURN
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

%type <String> op name_or_op
%type <Vector<Function>> program
%type <Function> function
%type <Body> body optbody decl optdecl ifrest
%type <If> cond conds
%type <Match> match
%type <Deque<Match>> matchs
%type <Expr> stmt expr binop unop initialiser ifexpr condexpr matchexpr whileexpr for_loop optexpr
%type <Expr[]> optexprs
%type <Deque<Expr>> optexprsp
%type <Vector<Expr>> stmt_list
%type <Variable> variable
%type <Vector<Variable>> variable_list
%type <Integer> varcount

%%

start: program { generateProgram($program); } ;

program
	: program function { $1.add($function); $$ = $1; }
	| function { $$ = new Vector<Function>(); ((Vector<Function>)$$).add($function); }

function
	: { st.pushScope(); } FUN name_or_op '(' parameter_list varcount ')' '=' body {
	$$ = new Function($name_or_op, $varcount, $body); st.popScope();
}

varcount: %empty { $$ = st.varCount(); }

name_or_op: NAME | op ;

parameter_list
	: %empty
    | NAME { st.addVar($NAME); }
    | parameter_list ',' NAME { st.addVar($NAME); }

/*
body
	: expr { $$ = new Body(new Expr[]{$expr}); }
	| { st.pushScope(); }'{' stmt_list '}' {
        st.popScope();
    	$$ = new Body($stmt_list.toArray(new Expr[]{}));
    }
*/

body: { st.pushScope(); }'{' stmt_list ';' '}' {
	st.popScope();
	$$ = new Body($stmt_list.toArray(new Expr[]{}));
}

stmt_list
    : stmt { $$ = new Vector<Expr>(); ((Vector<Expr>)$$).add($stmt); }
    | stmt_list ';' stmt {
	((Vector<Expr>)$1).add($stmt);
	$$ = $1;
}
    | %empty { $$ = new Vector<Expr>(); }
    ;

stmt
    : RETURN expr { $$ = new Return($2); }
    | expr
    | decl { $$ = $decl; }
    | whileexpr
    | for_loop
    ;

expr
    : LITERAL { $$ = new Literal($LITERAL); }
    | NAME { $$ = new Fetch(st.findVar($NAME)); }
    | NAME '=' expr { $$ = new Store(st.findVar($NAME), $3); }
    | NAME '(' optexprs ')' { $$ = new Call($NAME, $optexprs); }
    | '(' expr ')' %prec UNOP { $$ = $2; }
    | ifexpr
    | condexpr
    | matchexpr
    | unop
    | binop
    ;

condexpr: COND '{' conds optbody '}' {
	$$ = $optbody == null ? $conds : new Body(new Expr[]{$conds, $optbody});
 };
conds: cond conds { $$ = new If($cond.cond, $cond.thenpart, $2 == null ? null : new Body($2)); } | %empty { $$ = null; };
cond: expr FAT_ARROW body { $$ = new If($expr, $body); }

matchexpr: MATCH expr '{' matchs optbody '}' {
	var match_var = "__match_var";
	st.addVar(match_var);
	var matches = (Deque<Match>) $matchs;
	If res = null;
	do {
		var match = matches.removeFirst();
		var test = new Call("==", new Expr[]{
			new Fetch(st.findVar(match_var)),
			match.value
		});
		res = new If(test, match.body, res == null ? $optbody : new Body(res));
	} while (matches.size() > 0);

	$$ = new Body(new Expr[]{new Variable($expr), res});
}
matchs: match matchs {
	$$ = $2;
	((Deque<Match>)$$).addLast($match);
}
      | %empty { $$ = new ArrayDeque<Match>(); }
match: expr FAT_ARROW body { $$ = new Match($expr, $body); }

optexpr: %empty { $$ = null; } | expr ;
optbody: %empty { $$ = null; } | body ;
optdecl: %empty { $$ = null; } | decl ;

optexprs
   : %empty { $$ = new Expr[]{}; }
   | expr optexprsp {
       ((Deque<Expr>)$2).add($expr);
       $$ = ((Deque<Expr>)$2).toArray(new Expr[]{});
   }
   ;

optexprsp
    : %empty { $$ = new ArrayDeque<Expr>(); }
    | ',' expr optexprsp {
       ((Deque<Expr>)$3).add($expr);
       $$ = $3;
    }
    ;

op: OP1 | OP2 | OP3 | OP4 | OP5 | OP6 | OP7 ;
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

ifexpr
    : IF expr body { $$ = new If($expr, $body); }
    | IF expr body ifrest { $$ = new If($expr, $3, $4); }
    ;

ifrest
    : ELSE body { $$ = $body; }
    | ELSE ifexpr { $$ = new Body(new Expr[]{$ifexpr}); }
    ;

whileexpr: WHILE expr body { $$ = new While($expr, $body); };
for_loop: FOR '(' optdecl ';' expr ';' optexpr ')' body {
	var t = new Vector<Expr>(Arrays.asList($body.exprs));
	if ($7 != null)
		t.add($7);
	if ($optdecl != null)
		$$ = new Body(new Expr[]{
			$optdecl,
			new While($5, new Body(t.toArray(new Expr[]{})))
		});
	else
		$$ = new While($5, new Body(t.toArray(new Expr[]{})));
};

decl: VAR variable_list {
	$$ = new Body($variable_list.toArray(new Expr[]{}));
};

variable: NAME initialiser { st.addVar($NAME); $$ = new Variable($initialiser); }

initialiser
    : '=' expr { $$ = $expr; }
    | %empty { $$ = null; }
    ;

variable_list
    : variable { $$ = new Vector<Variable>(); ((Vector<Variable>)$$).add($variable); }
    | variable_list ',' variable { $$ = $1; ((Vector<Variable>)$$).add($variable); }
    ;
