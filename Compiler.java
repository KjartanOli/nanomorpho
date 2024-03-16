import java.util.Arrays;
import java.util.Vector;
import java.io.FileReader;

class Compiler {
	static String filename = null;
	static NanoMorphoLexer lexer = null;
	static SymbolTable st = new SymbolTable();
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

	static void emit( String fmt, Object... args ) {
		System.out.format(fmt,args);
		System.out.println();
	}

	// All existing labels, i.e. labels in the generated
	// code that we have already produced, should be
	// of form
	//	  _xxxx
	// where xxxx corresponds to an integer n
	// such that 0 <= n < nextLab.
	// So we should update nextLab as we generate
	// new labels.
	// The first generated label would be _0, the
	// next would be _1, and so on.
	private static int nextLab = 0;
	// Returns a new, previously unused, label.
	// Useful for control-flow expressions.
	static String newLabel()
	{
		return "_"+(nextLab++);
	}

	static void generateProgram(Vector<Function> funs ) {
		String programname = filename.substring(0,filename.lastIndexOf('.'));
		emit("\"%s.mexe\" = main in",programname);
		emit("!");
		emit("{{");
		for( var f: funs ) f.generate();
		emit("}}");
		emit("*");
		emit("BASIS;");
	}

	static public void main( String[] args ) throws Exception
	{
		Vector<Object[]> code = null;
		try
		{
			filename = args[0];
			lexer = new NanoMorphoLexer(new FileReader(filename));
			NanoMorphoParser parser = new NanoMorphoParser(lexer);
			parser.parse();
		}
		catch( Throwable e )
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
