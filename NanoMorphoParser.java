import java.util.Vector;
import java.util.HashMap;

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

	static class Expr {
		public final String type;
		public final Object[] args;
		public Expr(String type, Object[] args) {
			this.type = type;
			this.args = args;
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
			return new Expr("RETURN", new Object[] {expr()});
		}
		else if (getToken().type() == Token.NAME && getToken2().type() == '=') {
			var name = over(Token.NAME);
			var pos = findVar(name.lexeme());
			over('=');
			return new Expr("STORE", new Object[]{pos, expr()});
		}
		else {
			return binopexpr(Token.OP1);
		}
	}

    // For the parser you do not need the pri argument and
    // in the compiler you only need it if you wish to
    // distinguish priorities of operators
    static Expr binopexpr( int pri ) throws Exception {
		var left = smallexpr();
		if (left == null)
			expected("Something");
		Token op = null;
		Expr right = null;
		while (Token.OP1 <= getToken().type() && getToken().type() <= Token.OP7) {
			op = advance();
			right = smallexpr();
			left = new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left, right}});
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
					return new Expr("CALL", new Object[]{name.lexeme(), args.toArray(new Expr[]{})});
				}
				return new Expr("FETCH", new Object[]{findVar(name.lexeme())});
			case Token.OP1:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr1()});
			case Token.OP2:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr2()});
			case Token.OP3:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr3()});
			case Token.OP4:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr4()});
			case Token.OP5:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr5()});
			case Token.OP6:
			    var op = advance();
			    return new Expr("CALL", new Object[]{op.lexeme(), binopexpr6()});
			case Token.OP7:
				var op = advance();
				return new Expr("CALL", new Object[] {op.lexeme(), smallexpr()});
			case Token.LITERAL:
				var token = over(Token.LITERAL);
				return new Expr("LITERAL", new Object[] {token.lexeme()});
			case '(':
				over('(');
				var res = expr();
				over(')');
				return res;
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
				return new Expr("WHILE", new Object[] {test, body});
		}
		//...
		return null;
	}

    static Expr binopexpr1() throws Exception{
	Expr left = binopexpr2();
	if(left = null)
	    expected("Something");
	if(Token.OP1 <= getToken().type() && getToken().type <= Token.OP7){
	    op = aandvance();
	    right = binopexpr2(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
    }

    static Expr binopexpr2() throws Exception{
	if(getToken().type() == Token.OP1)
	    expected("OP2 to OP7");
	Expr left = binopexpr3();
	if(left = null)
	    expected("Something");
	if(Token.OP2 <= getToken().type() && getToken().type <= Token.OP7){
	    op = andvance();
	    right = binopexpr3(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
    }

    static Expr binopexpr3() throws Exception{
	if(getToken().type() == Token.OP1 || getToken().type() == Token.OP2)
	    expected("OP3 to OP7");
	Expr left = binopexpr4();
	if(left = null)
	    expected("Something");
	if(Token.OP3 <= getToken().type() && getToken().type <= Token.OP7){
	    op = andvance();
	    right = binopexpr4(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
    }

    static Expr binopexpr4() throws Exception{
	if(getToken().type() >= Token.OP1 && getToken().type() <= Token.OP3)
	    expected("OP4 to OP7");
	Expr left = binopexpr5();
	if(left = null)
	    expected("Something");
	if(Token.OP4 <= getToken().type() && getToken().type <= Token.OP7){
	    op = andvance();
	    right = binopexpr5(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
    }

    static Expr binopexpr5() throws Exception{
	if(getToken().type() >= Token.OP1 && getToken().type() <= Token.OP4)
	    expected("OP4 to OP7");
	Expr left = binopexpr6();
	if(left = null)
	    expected("Something");
	if(Token.OP5 <= getToken().type() && getToken().type <= Token.OP7){
	    op = andvance();
	    right = binopexpr6(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
    }

    static Expr binopexpr6() throws Exception{
        if(getToken().type() >= Token.OP1 && getToken().type() <= Token.OP5)
	    expected("OP5 to OP7");
	Expr left = smallexpr();
	if(left = null)
	    expected("Something");
	if(Token.OP6 <= getToken().type() && getToken().type <= Token.OP7){
	    op = andvance();
	    right = smallexpr(),
		return new Expr("CALL", new Object[]{op.lexeme(), new Expr[]{left,rigth}});
	}
	 return left;
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
			return new Expr("IF1", new Object[]{cond,thenpart});
		if( getToken().type() == Token.ELSEIF )
			return new Expr("IF2", new Object[]{cond,thenpart,ifexpr()});
		over(Token.ELSE);
		return new Expr("IF2", new Object[]{cond,thenpart,body()});
    }

    static Expr body() throws Exception {
		var res = new Vector<Expr>();
		if (getToken().type() != '{')
			return expr();

		over('{');
		res.add(expr());
		over(';');
		while (getToken().type() != '}') {
			res.add(expr());
			over(';');
		}
		over('}');
		return new Expr("BODY", res.toArray());
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
		for (var expr : fun.body)
			generateExpr(expr);
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

	static void generateExpr( Expr e ) {
		if (e == null)
			return;
		var type = e.type;
		if (type == "RETURN") {
			generateReturn(e);
		}
		if (type == "STORE") {
			var pos = (Integer) e.args[0];
			generateExpr((Expr) e.args[1]);
			emit("(Store %d)", pos);
		}
		if (type == "FETCH") {
			var pos = (Integer) e.args[0];
			emit("(Fetch %d)", pos);
		}
		if (type == "LITERAL") {
			emit("(MakeVal %s)", (String) e.args[0]);
		}
		if (type == "CALL") {
			generateFuncall(e);
		}
		if (type == "IF1") {
			generateIF1(e);
		}
		if (type == "IF2") {
			generateIF2(e);
		}
		if (type == "BODY") {
			generateBody(e);
		}
		if (type == "WHILE") {
			generateWhile(e);
		}
	}

	static void generateBody( Expr bod ) {
		var exprs = bod.args;
		for (var expr : exprs)
			generateExpr((Expr) expr);
	}

	static void generateWhile(Expr e) {
		var cond = (Expr) e.args[0];
		var body = (Expr) e.args[1];

		var startLab = newLabel();
		var endLab = newLabel();

		emit("%s:", startLab);
		generateExpr(cond);
		emit("(GoFalse %s)", endLab);
		generateBody(body);
		emit("(Go %s)", startLab);
		emit("%s:", endLab);
	}

	static void generateIF1(Expr e) {
		var cond = (Expr) e.args[0];
		var body = (Expr) e.args[1];

		var lab = newLabel();
		generateExpr(cond);
		emit("(GoFalse %s)", lab);
		generateExpr(body);
		emit("%s:", lab);
	}

	static void generateIF2(Expr e) {
		var cond = (Expr) e.args[0];
		var then = (Expr) e.args[1];
		var body = (Expr) e.args[2];

		var elseLab = newLabel();
		var endLab = newLabel();

		generateExpr(cond);
		emit("(GoFalse %s)", elseLab);
		generateExpr(then);
		emit("(Go %s)", endLab);
		emit("%s:", elseLab);
		generateExpr(body);
		emit("%s:", endLab);
	}

	static void generateReturn(Expr e) {
		var val = (Expr) e.args[0];
		if (val.type == "FETCH") {
			var pos = (Integer) val.args[0];
			emit("(FetchR %d)", pos);
		}
		else if (val.type == "CALL") {
			generateFuncall(val, true);
		}
		else {
			generateExpr(val);
			emit("(Return)");
		}
	}

	static void generateFuncall(Expr fun) {
		generateFuncall(fun, false);
	}

	static void generateFuncall(Expr fun, boolean tailCall) {
		var name = (String) fun.args[0];
		var args = (Object[]) fun.args[1];
		var argc = args.length;
		if (argc > 0) {
			for (var i = 0; i < argc - 1; ++i) {
				var arg = (Expr) args[i];
				generateExpr(arg);
				emit("(Push)");
			}
			generateExpr((Expr) args[argc - 1]);
		}
		if (tailCall)
			emit("(CallR #\"%s[f%d]\" %d)", name, argc, argc);
		else
			emit("(Call #\"%s[f%d]\" %d)", name, argc, argc);
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
