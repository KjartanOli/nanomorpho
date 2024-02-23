import java.util.Vector;
import java.util.HashMap;
import java.util.Arrays;

public class NanoMorphoParser
{
	static Token advance() throws Exception {
		return NanoMorphoLexer.advance();
	}

	static Token over( int tok ) throws Exception {
		return NanoMorphoLexer.over(tok);
	}

	static Token over( char tok ) throws Exception {
		return NanoMorphoLexer.over(tok);
	}

	static Token getToken() {
		return NanoMorphoLexer.getToken();
	}

	static Token getToken2() {
		return NanoMorphoLexer.getToken2();
	}

	static int getLine() {
		return NanoMorphoLexer.getLine();
	}

	static int getColumn() {
		return NanoMorphoLexer.getColumn();
	}

	static void expected( String str ) {
		NanoMorphoLexer.expected(str);
	}

	enum ExprType {
		LITERAL,
		RETURN,
		CALL,
		STORE,
		FETCH,
		IF1,
		IF2,
		BODY,
		WHILE,
		OR,
		AND,
		NOT
	}

	interface Expr {
		public ExprType type();
	}

	static class Literal implements Expr {
		public final String value;

		public Literal(String value) { this.value = value; }
		public ExprType type() { return ExprType.LITERAL; }
		public String toString() { return this.value; }
	}

	static class Call implements Expr {
		public final String function;
		public final Expr[] args;

		public Call(String function, Expr[] args) {
			this.function = function;
			this.args = args;
		}

		public ExprType type() { return ExprType.CALL; }
		public String toString() {
			return String.format("%s%s", this.function, Arrays.toString(this.args));
		}
	}

	static class Return implements Expr {
		public final Expr value;

		public Return(Expr value) { this.value = value; }
		public ExprType type() { return ExprType.RETURN; }
		public String toString() {
			return String.format("Return [%s]", value);
		}
	}

	static class If implements Expr {
		public final Expr cond;
		public final Body thenpart;
		public final Body elsepart;

		public If(Expr cond, Body thenpart) {
			this(cond, thenpart, null);
		}

		public If(Expr cond, Body thenpart, Body elsepart) {
			this.cond = cond;
			this.thenpart = thenpart;
			this.elsepart = elsepart;
		}

		public ExprType type () {
			return (this.elsepart == null) ? ExprType.IF1 : ExprType.IF2;
		}
		public String toString() {
			if (this.elsepart == null)
				return String.format("If [%s] {%s}", this.cond, this.thenpart);
			else
				return String.format("If [%s] {%s} else {%s}", this.cond, this.thenpart, this.elsepart);
		}
	}

	static class Body implements Expr {
		public final Expr[] exprs;

		public Body(Expr[] exprs) { this.exprs = exprs; }
		public ExprType type() { return ExprType.BODY; }
		public String toString() {
			return Arrays.toString(this.exprs);
		}
	}

	static class Fetch implements Expr {
		public final int pos;

		public Fetch(int pos) { this.pos = pos; }
		public ExprType type() { return ExprType.FETCH; }
		public String toString() {
			return String.format("Fetch %d", this.pos);
		}
	}

	static class Store implements Expr {
		public final int pos;
		public final Expr value;

		public Store(int pos, Expr value) {
			this.pos = pos;
			this.value = value;
		}

		public ExprType type() { return ExprType.STORE; }
		public String toString() {
			return String.format("Store %d [%s]", this.pos, this.value);
		}
	}

	static class While implements Expr {
		public final Expr cond;
		public final Body body;

		public While(Expr cond, Body body) {
			this.cond = cond;
			this.body = body;
		}

		public ExprType type() { return ExprType.WHILE; }
		public String toString() {
			return String.format("While [%s] {%s}", this.cond, this.body);
		}
	}

	static class Or implements Expr {
		public final Expr left;
		public final Expr right;

		public Or(Expr left, Expr right) {
			this.left = left;
			this.right = right;
		}

		public ExprType type() { return ExprType.OR; }
		public String toString() {
			return String.format("Or [%s] [%s]", this.left, this.right);
		}
	}

	static class And implements Expr {
		public final Expr left;
		public final Expr right;

		public And(Expr left, Expr right) {
			this.left = left;
			this.right = right;
		}

		public ExprType type() { return ExprType.AND; }
		public String toString() {
			return String.format("And [%s] [%s]", this.left, this.right);
		}
	}

	static class Not implements Expr {
		public final Expr value;

		public Not(Expr value) { this.value = value; }
		public ExprType type() { return ExprType.NOT; }
		public String toString() {
			return String.format("Not [%s]", this.value);
		}
	}

	private static class FunctionBody {
		public final int declc;
		public final Expr[] body;

		public FunctionBody(int declc, Expr[] body) {
			this.declc = declc;
			this.body = body;
		}
	}

	static class Function {
		public final String name;
		public final int argc;
		public final int localc;
		public final Expr[] body;
		public Function(String name, int argc, int localc, Expr[] body) {
			this.name = name;
			this.argc = argc;
			this.localc = localc;
			this.body = body;
		}
	}

    // You will not need the symbol table until you do the compiler proper.
    // The symbol table consists of the following two variables.
    private static int varCount;
    private static HashMap<String,Integer> varTable;

    // Adds a new variable to the symbol table.
    // Throws Error if the variable already exists.
	private static void addVar( String name ) {
		if( varTable.get(name) != null )
			expected("undeclared variable name");
		varTable.put(name,varCount++);
	}

    // Finds the location of an existing variable.
    // Throws Error if the variable does not exist.
	private static int findVar( String name ) {
		Integer res = varTable.get(name);
		if( res == null )
			expected("declared variable name");
		return res;
	}

    static Vector<Function> program() throws Exception {
		var res = new Vector<Function>();
		while (getToken().type() != Token.EOF)
			res.add(function());
		return res;
	}

	static Function function() throws Exception {
		varCount = 0;
		varTable = new HashMap<String,Integer>();

		over(Token.FUN);
		var name = over(Token.NAME);
		over('(');
		var params = parameter_list();
		over(')');
		var body = function_body();

		// We're done with this symbol table so we'll erase it
		varTable = null;
		return new Function(name.lexeme(), params, (Integer) body.declc, body.body);
	}

	static int parameter_list() throws Exception {
		int argc = 0;
		if (getToken().type() != Token.NAME)
			return argc;

		var params = new Vector<Object>();
		var name = over(Token.NAME);
		++argc;
		addVar(name.lexeme());
		while (getToken().type() == ',') {
			over(',');
			name = over(Token.NAME);
			addVar(name.lexeme());
			++argc;
		}
		return argc;
	}

	static Integer declaration_list() throws Exception {
		int count = 0;
		while (getToken().type() == Token.VAR) {
			count += decl();
			over(';');
		}
		return count;
	}

	static FunctionBody function_body() throws Exception {
		over('{');
		var locals = declaration_list();
		var exprs = new Vector<Expr>();
		while (getToken().type() != '}') {
			exprs.add(expr());
			over(';');
		}
		over('}');
		return new FunctionBody(locals, exprs.toArray(new Expr[]{}));
	}

    // decl = 'var', Token.NAME, { ',' Token.NAME } ;
    static int decl() throws Exception {
		int count = 1;
		over(Token.VAR);
		addVar(over(Token.NAME).lexeme());
		while( getToken().type() == ',' ) {
			over(',');
			addVar(over(Token.NAME).lexeme());
			++count;
		}

		return count;
	}

    static Expr expr() throws Exception {
		if (getToken().type() == Token.RETURN) {
			over(Token.RETURN);
			return new Return(expr());
		}
		else if (getToken().type() == Token.NAME && getToken2().type() == '=') {
			var name = over(Token.NAME);
			var pos = findVar(name.lexeme());
			over('=');
			return new Store(pos, expr());
		}
		else {
			return orexpr();
		}
	}

	static Expr orexpr() throws Exception {
		var left = andexpr();
		if (getToken().type() == Token.OR) {
			over(Token.OR);
			var right = orexpr();
			left = new Or(left, right);
		}

		return left;
	}

	static Expr andexpr() throws Exception {
		var left = notexpr();
		if (getToken().type() == Token.AND) {
			var right = notexpr();
			left = new And(left, right);
		}
		return left;
	}

	static Expr notexpr() throws Exception {
		if (getToken().type() == Token.NOT) {
			over(Token.NOT);
			return new Not(notexpr());
		}
		return binopexpr1();
	}

	static Expr binopexpr1() throws Exception {
		var left = binopexpr2();
		while (getToken().type() == Token.OP1) {
			Token op = over(Token.OP1);
			Expr right = binopexpr2();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr binopexpr2() throws Exception {
		Expr left = binopexpr3();
		if(getToken().type() == Token.OP2) {
			var op = over(Token.OP2);
			var right = binopexpr2();
			return new Call(op.lexeme(), new Expr[]{left,right});
		}
		return left;
	}

	static Expr binopexpr3() throws Exception {
		var left = binopexpr4();
		while (getToken().type() == Token.OP3) {
			Token op = over(Token.OP3);
			Expr right = binopexpr4();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr binopexpr4() throws Exception {
		var left = binopexpr5();
		while (getToken().type() == Token.OP4) {
			Token op = over(Token.OP4);
			Expr right = binopexpr5();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr binopexpr5() throws Exception {
		var left = binopexpr6();
		while (getToken().type() == Token.OP5) {
			Token op = over(Token.OP5);
			Expr right = binopexpr6();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr binopexpr6() throws Exception {
		var left = binopexpr7();
		while (getToken().type() == Token.OP6) {
			Token op = over(Token.OP6);
			Expr right = binopexpr7();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr binopexpr7() throws Exception {
		var left = smallexpr();
		while (getToken().type() == Token.OP7) {
			Token op = over(Token.OP7);
			Expr right = smallexpr();

			left = new Call(op.lexeme(), new Expr[]{left, right});
		}
		return left;
	}

	static Expr smallexpr() throws Exception {
		switch (getToken().type()) {
			case Token.NAME:
				var name = over(Token.NAME);
				if (getToken().type() == '(') {
					over('(');
					var args = new Vector<Expr>();
					if (getToken().type() != ')') {
						args.add(expr());
						while (getToken().type() == ',') {
							over(',');
							args.add(expr());
						}
					}
					over(')');
					return new Call(name.lexeme(), args.toArray(new Expr[]{}));
				}
				return new Fetch(findVar(name.lexeme()));
			case Token.LITERAL:
				var token = over(Token.LITERAL);
				return new Literal(token.lexeme());
			case '(':
				over('(');
				var res = expr();
				over(')');
				return res;
			case Token.OP1:
			case Token.OP2:
			case Token.OP3:
			case Token.OP4:
			case Token.OP5:
			case Token.OP6:
			case Token.OP7:
				var op = advance();
				var expr = smallexpr();
				return new Call(op.lexeme(), new Expr[]{expr});
			case Token.IF:
				return ifexpr();
			case Token.ELSEIF:
				expected("'if' before 'elseif'");
				break;
			case Token.ELSE:
				expected("'if' before 'else'");
				break;
			case Token.WHILE:
				over(Token.WHILE);
				var test = expr();
				var body = body();
				return new While(test, body);
		}
		expected("something went wrong");
		return null;
	}

    // Alternative syntax with 'elsif'.
    // Slightly more complicated and requires that
    // the caller verifies beforehand that 'if'
    // and 'elsif' are used in their proper places.

    // ifexpr = 'if', expr, body, [ ifrest ] ;
    // ifrest = 'else', body | 'elsif', expr, body, [ ifrest ] ;
    static Expr ifexpr() throws Exception {
		if( getToken().type() == Token.ELSEIF )
			over(Token.ELSEIF);
		else
			over(Token.IF);

		var cond = expr();
		var thenpart = body();
		if( getToken().type() != Token.ELSE && getToken().type() != Token.ELSEIF )
			return new If(cond,thenpart);
		over(Token.ELSE);
		var t = new If(cond,thenpart,body());
		return t;
    }

    static Body body() throws Exception {
		var res = new Vector<Expr>();
		if (getToken().type() != '{')
			return new Body(new Expr[]{expr()});

		over('{');
		res.add(expr());
		over(';');
		while (getToken().type() != '}') {
			res.add(expr());
			over(';');
		}
		over('}');
		return new Body(res.toArray(new Expr[]{}));
	}

    // None of the following is needed in the parser
    static void generateProgram( String filename, Vector<Function> funs ) {
		String programname = filename.substring(0,filename.lastIndexOf('.'));
		emit("\"%s.mexe\" = main in",programname);
		emit("!");
		emit("{{");
		for( var f: funs ) generateFunction(f);
		emit("}}");
		emit("*");
		emit("BASIS;");
	}

    static void emit( String fmt, Object... args ) {
		System.out.format(fmt,args);
		System.out.println();
	}

    static void generateFunction( Function fun ) {
		emit("#\"%s[f%d]\" = ", fun.name, fun.argc);
		emit("[");
		if (fun.localc > 0) {
			emit("(MakeVal null)");
			for (var i = 0; i < fun.localc; ++i)
				emit("(Push)");
		}
		for (var i = 0; i < fun.body.length - 1; ++i)
			generateExpr(fun.body[i]);

		 generateExpr(fun.body[fun.body.length - 1], true);
		emit("];");
	}

    // All existing labels, i.e. labels the generated
    // code that we have already produced, should be
    // of form
    //    _xxxx
    // where xxxx corresponds to an integer n
    // such that 0 <= n < nextLab.
    // So we should update nextLab as we generate
    // new labels.
    // The first generated label would be _0, the
    // next would be _1, and so on.
    private static int nextLab = 0;

	// Returns a new, previously unused, label.
	// Useful for control-flow expressions.
	static String newLabel() {
		return "_"+(nextLab++);
	}

	static void generateLiteral(Literal l, boolean tailpos) {
		if (tailpos)
			emit("(MakeValP %s)", l.value);
		else
			emit("(MakeVal %s)", l.value);
	}

	static void generateFetch(Fetch f, boolean tailpos) {
		if (tailpos)
			emit("(FetchR %d)", f.pos);
		else
			emit("(Fetch %d)", f.pos);
	}

	static void generateStore(Store s, boolean tailpos) {
		generateExpr(s.value);
		if (tailpos)
			emit("(StoreR %d)", s.pos);
		else
			emit("(Store %d)", s.pos);
	}

	static void generateExpr(Expr e) {
		generateExpr(e, false);
	}

	static void generateExpr(Expr e, boolean tailpos) {
		int pos;
		switch (e.type()) {
			case ExprType.RETURN:
				generateReturn((Return) e);
				break;
			case ExprType.STORE:
				generateStore((Store) e, tailpos);
				break;
			case ExprType.FETCH:
				generateFetch((Fetch) e, tailpos);
				break;
			case ExprType.LITERAL:
				generateLiteral((Literal) e, tailpos);
				break;
			case ExprType.CALL:
				generateFuncall((Call) e, tailpos);
				break;
			case ExprType.IF1:
				generateIF1((If) e);
				break;
			case ExprType.IF2:
				generateIF2((If) e);
				break;
			case ExprType.BODY:
				generateBody((Body) e);
				break;
			case ExprType.WHILE:
				generateWhile((While) e);
				break;
			case ExprType.OR:
				generateOR((Or) e);
				break;
			case ExprType.AND:
				generateAND((And) e);
				break;
			case ExprType.NOT:
				generateNOT((Not) e);
				break;
		}
	}

	static void generateBody(Body body) {
		for (var expr : body.exprs)
			generateExpr(expr);
	}

	static void generateWhile(While w) {
		var startLab = newLabel();
		var endLab = newLabel();

		emit("%s:", startLab);
		generateExpr(w.cond);
		emit("(GoFalse %s)", endLab);
		generateBody(w.body);
		emit("(Go %s)", startLab);
		emit("%s:", endLab);
	}

	static void generateIF1(If i) {
		var lab = newLabel();
		generateExpr(i.cond);
		emit("(GoFalse %s)", lab);
		generateExpr(i.thenpart);
		emit("%s:", lab);
	}

	static void generateIF2(If i) {
		var elseLab = newLabel();
		var endLab = newLabel();

		generateExpr(i.cond);
		emit("(GoFalse %s)", elseLab);
		generateExpr(i.thenpart);
		emit("(Go %s)", endLab);
		emit("%s:", elseLab);
		generateExpr(i.elsepart);
		emit("%s:", endLab);
	}

	static void generateReturn(Return r) {
		switch (r.value.type()) {
			case ExprType.FETCH:
				generateFetch((Fetch) r.value, true);
				break;
			case ExprType.CALL:
				generateFuncall((Call) r.value, true);
				break;
			case ExprType.LITERAL:
				generateLiteral((Literal) r.value, true);
				break;
			default:
				generateExpr(r.value);
				emit("(Return)");
				break;
		}
	}

	static void generateOR(Or o) {
		var endlab = newLabel();
		generateExpr(o.left);
		emit("(GoTrue %s)", endlab);
		generateExpr(o.right);
		emit("%s:", endlab);
	}

	static void generateAND(And a) {
		var endlab = newLabel();
		generateExpr(a.left);
		emit("(GoFalse %s)", endlab);
		generateExpr(a.right);
		emit("%s:", endlab);
	}

	static void generateNOT(Not n) {
		generateExpr(n.value);
		emit("(Not)");
	}

	static void generateFuncall(Call fun) {
		generateFuncall(fun, false);
	}

	static void generateFuncall(Call c, boolean tailCall) {
		var argc = c.args.length;
		if (argc > 0) {
			generateExpr(c.args[0]);
			for (var i = 1; i < argc; ++i) {
				var arg = c.args[i];
				if (arg.type() == ExprType.LITERAL) {
					emit("(MakeValP %s)", ((Literal) arg).value);
				}
				else {
					generateExpr(arg);
					emit("(Push)");
				}
			}
		}
		if (tailCall)
			emit("(CallR #\"%s[f%d]\" %d)", c.function, argc, argc);
		else
			emit("(Call #\"%s[f%d]\" %d)", c.function, argc, argc);
	}

	static public void main( String[] args ) throws Exception {
		Vector<Function> code = null;
		try {
			NanoMorphoLexer.startLexer(args[0]);
			code = program();
			if(getToken().type() != Token.EOF) expected("end of file");
		}
		catch(Throwable e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		// For the parser you do not need this line:
		generateProgram(args[0],code);
	}
}
