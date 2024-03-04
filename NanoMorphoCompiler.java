import java.util.Vector;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;

public class NanoMorphoCompiler
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
		NOT,
		VARIABLE
	}

	static class GenerationContext {
		public final boolean tailpos;

		GenerationContext(boolean tailpos) {
			this.tailpos = tailpos;
		}
	}

	interface Expr {
		public ExprType type();
		public void generate(GenerationContext ctx);
		default public void generate() {
			this.generate(new GenerationContext(false));
		}
	}

	static class Literal implements Expr {
		public final String value;

		public Literal(String value) { this.value = value; }
		public ExprType type() { return ExprType.LITERAL; }
		public void generate(GenerationContext ctx) {
			if (ctx.tailpos)
				emit("(MakeValR %s)", this.value);
			else
				emit("(MakeVal %s)", this.value);
		}
		public String toString() { return this.value; }
	}

	static class Variable implements Expr {
		public final Expr initialiser;

		public Variable(Expr initialiser) {
			this.initialiser = initialiser;
		}
		public Variable() {
			this(new Literal("null"));
		}
		public ExprType type() { return ExprType.VARIABLE; }
		public void generate(GenerationContext ctx) {
			this.initialiser.generate();
			emit("(Push)");
		}
	}

	static class Call implements Expr {
		public final String function;
		public final Expr[] args;

		public Call(String function, Expr[] args) {
			this.function = function;
			this.args = args;
		}

		public ExprType type() { return ExprType.CALL; }
		public void generate(GenerationContext ctx) {
			var argc = this.args.length;
			if (argc > 0) {
				this.args[0].generate(new GenerationContext(false));
				for (var i = 1; i < argc; ++i) {
					var arg = this.args[i];
					if (arg.type() == ExprType.LITERAL) {
						emit("(MakeValP %s)", ((Literal) arg).value);
					}
					else {
						emit("(Push)");
						arg.generate(new GenerationContext(false));
					}
				}
			}
			if (ctx.tailpos)
				emit("(CallR #\"%s[f%d]\" %d)", this.function, argc, argc);
			else
				emit("(Call #\"%s[f%d]\" %d)", this.function, argc, argc);
		}
		public String toString() {
			return String.format("%s%s", this.function, Arrays.toString(this.args));
		}
	}

	static class Return implements Expr {
		public final Expr value;

		public Return(Expr value) { this.value = value; }
		public ExprType type() { return ExprType.RETURN; }
		public void generate(GenerationContext ctx) {
			switch (this.value.type()) {
				case FETCH:
				case LITERAL:
				case CALL:
					this.value.generate(new GenerationContext(true));
					break;
				default:
					this.value.generate(ctx);
					emit("(Return)");
					break;
			}
		}
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
		public void generate(GenerationContext ctx) {
			if (this.type() == ExprType.IF1)
				this.generate1(ctx);
			else
				this.generate2(ctx);
		}
		private void generate1(GenerationContext ctx) {
			var lab = newLabel();
			this.cond.generate(new GenerationContext(false));
			emit("(GoFalse %s)", lab);
			this.thenpart.generate(ctx);
			emit("%s:", lab);
		}
		private void generate2(GenerationContext ctx) {
			var elseLab = newLabel();
			var endLab = newLabel();

			this.cond.generate(new GenerationContext(false));
			emit("(GoFalse %s)", elseLab);
			this.thenpart.generate(ctx);
			emit("(Go %s)", endLab);
			emit("%s:", elseLab);
			this.elsepart.generate(ctx);
			emit("%s:", endLab);
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
		public void generate(GenerationContext ctx) {
			for (var i = 0; i < this.exprs.length - 1; ++i)
				this.exprs[i].generate(new GenerationContext(false));

			this.exprs[this.exprs.length - 1].generate(ctx);
		}
		public String toString() {
			return Arrays.toString(this.exprs);
		}
	}

	static class Fetch implements Expr {
		public final int pos;

		public Fetch(int pos) { this.pos = pos; }
		public ExprType type() { return ExprType.FETCH; }
		public void generate(GenerationContext ctx) {
			if (ctx.tailpos)
				emit("(FetchR %d)", this.pos);
			else
				emit("(Fetch %d)", this.pos);
		}
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
		public void generate(GenerationContext ctx) {
			this.value.generate(new GenerationContext(false));
			if (ctx.tailpos)
				emit("(StoreR %d)", this.pos);
			else
				emit("(Store %d)", this.pos);
		}
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
		public void generate(GenerationContext ctx) {
			var startLab = newLabel();
			var endLab = newLabel();

			emit("%s:", startLab);
			this.cond.generate(new GenerationContext(false));
			emit("(GoFalse %s)", endLab);
			this.body.generate(new GenerationContext(false));
			emit("(Go %s)", startLab);
			emit("%s:", endLab);
		}
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
		public void generate(GenerationContext ctx) {
			var endlab = newLabel();
			this.left.generate(ctx);
			emit("(GoTrue %s)", endlab);
			this.right.generate(ctx);
			emit("%s:", endlab);
		}
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
		public void generate(GenerationContext ctx) {
			var endlab = newLabel();
			this.left.generate(ctx);
			emit("(GoFalse %s)", endlab);
			this.right.generate(ctx);
			emit("%s:", endlab);
		}
		public String toString() {
			return String.format("And [%s] [%s]", this.left, this.right);
		}
	}

	static class Not implements Expr {
		public final Expr value;

		public Not(Expr value) { this.value = value; }
		public ExprType type() { return ExprType.NOT; }
		public void generate(GenerationContext ctx) {
			value.generate(new GenerationContext(false));
			if (ctx.tailpos)
				emit("(NotR)");
			else
				emit("(Not)");
		}
		public String toString() {
			return String.format("Not [%s]", this.value);
		}
	}

	static class Function {
		public final String name;
		public final int argc;
		public final Body body;
		public Function(String name, int argc, Body body) {
			this.name = name;
			this.argc = argc;
			this.body = body;
		}
		public void generate() {
			emit("#\"%s[f%d]\" = ", this.name, this.argc);
			emit("[");
			this.body.generate(new GenerationContext(true));
			emit("];");
		}
	}

	static class SymbolTable {
		private Deque<HashMap<String, Integer>> scopes = new ArrayDeque<HashMap<String, Integer>>();

		public int varCount() {
			var count = 0;
			for (var scope : scopes)
				count += scope.size();

			return count;
		}

		public void pushScope() {
			scopes.push(new HashMap<String, Integer>());
		}

		public void popScope() {
			scopes.pop();
		}

		public void addVar(String name) {
			if (scopes.peek().get(name) != null)
				expected("undeclared variable name");
			scopes.peek().put(name, this.varCount());
		}

		public int findVar(String name) {
			for (var scope : scopes) {
				var pos = scope.get(name);
				if (pos != null)
					return pos;
			}
			expected("declared variable name");
			// unreachable
			return 0;
		}
	}

	private static SymbolTable st = new SymbolTable();

    static Vector<Function> program() throws Exception {
		var res = new Vector<Function>();
		while (getToken().type() != Token.EOF)
			res.add(function());
		return res;
	}

	static Function function() throws Exception {
		st.pushScope();
		over(Token.FUN);
		var name = over(Token.NAME);
		over('(');
		var params = parameter_list();
		over(')');
		over('=');
		var body = body();

		// We're done with this symbol table so we'll erase it
		st.popScope();
		return new Function(name.lexeme(), params, body);
	}

	static int parameter_list() throws Exception {
		int argc = 0;
		if (getToken().type() != Token.NAME)
			return argc;

		var params = new Vector<Object>();
		var name = over(Token.NAME);
		++argc;
		st.addVar(name.lexeme());
		while (getToken().type() == ',') {
			over(',');
			name = over(Token.NAME);
			st.addVar(name.lexeme());
			++argc;
		}
		return argc;
	}

    // decl = 'var', Token.NAME, { ',' Token.NAME } ;
    static Body decl() throws Exception {
		var res = new Vector<Variable>();
		over(Token.VAR);
		var name = over(Token.NAME);
		if (getToken().type() == '=') {
			over('=');
			res.add(new Variable(expr()));
		}
		else {
			res.add(new Variable());
		}
		// By adding the new variable to the symbol table after
		// parsing its initialiser we allow the initialiser to refer
		// to variables of the same name in outer scopes.
		st.addVar(name.lexeme());

		while( getToken().type() == ',' ) {
			over(',');
			name = over(Token.NAME);
			if (getToken().type() == '=') {
				over('=');
				res.add(new Variable(expr()));
			}
			else {
				res.add(new Variable());
			}
			// By adding the new variable to the symbol table after
			// parsing its initialiser we allow the initialiser to refer
			// to variables of the same name in outer scopes.
			st.addVar(name.lexeme());
		}

		return new Body(res.toArray(new Expr[]{}));
	}

    static Expr expr() throws Exception {
		if (getToken().type() == Token.RETURN) {
			over(Token.RETURN);
			return new Return(expr());
		}
		else if (getToken().type() == Token.VAR) {
			return decl();
		}
		else if (getToken().type() == Token.NAME && getToken2().type() == '=') {
			var name = over(Token.NAME);
			var pos = st.findVar(name.lexeme());
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
			over(Token.AND);
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
				return new Fetch(st.findVar(name.lexeme()));
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

    // ifexpr = 'if', expr, body, [ ifrest ] ;
    // ifrest = 'else', body ;
    static Expr ifexpr() throws Exception {
		over(Token.IF);

		var cond = expr();
		var thenpart = body();
		if(getToken().type() != Token.ELSE)
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
		st.pushScope();
		res.add(expr());
		over(';');
		while (getToken().type() != '}') {
			res.add(expr());
			over(';');
		}
		over('}');
		st.popScope();
		return new Body(res.toArray(new Expr[]{}));
	}

    // None of the following is needed in the parser
    static void generateProgram( String filename, Vector<Function> funs ) {
		String programname = filename.substring(0,filename.lastIndexOf('.'));
		emit("\"%s.mexe\" = main in",programname);
		emit("!");
		emit("{{");
		for( var f: funs ) f.generate();
		emit("}}");
		emit("*");
		emit("BASIS;");
	}

    static void emit( String fmt, Object... args ) {
		System.out.format(fmt,args);
		System.out.println();
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
