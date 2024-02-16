import java.util.Vector;
import java.util.HashMap;

public class NanoMorphoParser
{
    final static int YYERRCODE = -1;
    final static int EOF = 0;
    final static int IF = 1001;
    final static int ELSE = 1002;
    final static int ELSEIF = 1003;
    final static int WHILE = 1004;
    final static int VAR = 1005;
    final static int RETURN = 1006;
    final static int NAME = 1007;
    final static int LITERAL = 1008;
    final static int OP1 = 1009;
    final static int OP2 = 1010;
    final static int OP3 = 1011;
    final static int OP4 = 1012;
    final static int OP5 = 1013;
    final static int OP6 = 1014;
    final static int OP7 = 1015;
	final static int FUN = 1016;

    static String advance() throws Exception {
		return NanoMorphoLexer.advance();
	}

	static String over( int tok ) throws Exception {
		return NanoMorphoLexer.over(tok);
	}

	static String over( char tok ) throws Exception {
		return NanoMorphoLexer.over(tok);
	}

	static int getToken() {
		return NanoMorphoLexer.getToken();
	}

	static String getTokenName() {
		return NanoMorphoLexer.getTokenName();
	}

	static String getLexeme() {
		return NanoMorphoLexer.getLexeme();
	}

	static int getToken2() {
		return NanoMorphoLexer.getToken2();
	}

	static int getLine() {
		return NanoMorphoLexer.getLine();
	}

	static int getColumn() {
		return NanoMorphoLexer.getColumn();
	}

	static void expected( int tok ) {
		NanoMorphoLexer.expected(tok);
	}

	static void expected( char tok ) {
		NanoMorphoLexer.expected(tok);
	}

	static void expected( String str ) {
		NanoMorphoLexer.expected(str);
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

    static Vector<Object[]> program() throws Exception {
			while (getToken() != EOF)
				function();
		return new Vector<Object[]>();
	}

	static Object[] function() throws Exception {
		varCount = 0;
		varTable = new HashMap<String,Integer>();

		over(FUN);
		var name = over(NAME);
		over('(');
		parameter_list();
		over(')');
		function_body();

		// We're done with this symbol table so we'll erase it
		varTable = null;
		return null;
	}

	static Object[] parameter_list() throws Exception {
		if (getToken() != NAME)
			return null;
		over(NAME);
		while (getToken() == ',') {
			over(',');
			over(NAME);
		}
		return null;
	}

	static Object[] declaration_list() throws Exception {
		while (getToken() == VAR) {
			decl();
			over(';');
		}
		return null;
	}

	static Object[] function_body() throws Exception {
		over('{');
		declaration_list();
		while (getToken() != '}') {
			expr();
			over(';');
		}
		over('}');
		return null;
	}

    // decl = 'var', NAME, { ',' NAME } ;
    static void decl() throws Exception
    {
			over(VAR);
			addVar(over(NAME));
			while( getToken() == ',' )
				{
					over(',');
					addVar(over(NAME));
				}
		}

    static Object[] expr() throws Exception
    {
			if (getToken() == RETURN) {
				over(RETURN);
				expr();
			}
			else if (getToken() == NAME && getToken2() == '=') {
				over(NAME);
				over('=');
				expr();
			}
			else {
				binopexpr(OP1);
			}
			return null;
		}

    // For the parser you do not need the pri argument and
    // in the compiler you only need it if you wish to
    // distinguish priorities of operators
    static Object[] binopexpr( int pri ) throws Exception {
		smallexpr();
		while (OP1 <= getToken() && getToken() <= OP7) {
			advance();
			smallexpr();
		}
		return null;
	}

    static Object[] smallexpr() throws Exception {
		switch (getToken()) {
			case NAME:
				over(NAME);
				if (getToken() == '(') {
					over('(');
					if (getToken() == NAME) {
						over(NAME);
						while (getToken() == ',') {
							over(',');
							over(NAME);
						}
					}
					over(')');
				}
				break;
			case OP1:
			case OP2:
			case OP3:
			case OP4:
			case OP5:
			case OP6:
			case OP7:
				advance();
				smallexpr();
				break;
			case LITERAL:
				over(LITERAL);
				break;
			case '(':
				over('(');
				expr();
				over(')');
				break;
			case IF:
				ifexpr();
				break;
			case ELSEIF:
				expected("'if' before 'elseif'");
				break;
			case ELSE:
				expected("'if' before 'else'");
				break;
			case WHILE:
				over(WHILE);
				expr();
				body();
				break;
		}
		//...
		return null;
	}

    // if-expression syntax without 'elsif'
    //
    // ifexpr = 'if', expr, body, [ 'else', ( ifexpr | body ) ] ;
    // static Object[] ifexpr() throws Exception {
	// 	over(IF);
	// 	Object[] cond = expr();
	// 	Object[] thenpart = body();
	// 	if( getToken() != ELSE )
	// 		return new Object[]{"IF1",cond,thenpart};
	// 	over(ELSE);
	// 	if( getToken() == IF )
	// 		return new Object[]{"IF2",cond,thenpart,ifexpr()};
	// 	else if( getToken() == '{' )
	// 		return new Object[]{"IF2",cond,thenpart,body()};
	// 	else
	// 		expected("'if' or '{' following 'else'");
	// 	throw new Error("This can't happen");
	// }

    // Alternative syntax with 'elsif'.
       // Slightly more complicated and requires that
       // the caller verifies beforehand that 'if'
       // and 'elsif' are used in their proper places.

    // ifexpr = 'if', expr, body, [ ifrest ] ;
       // ifrest = 'else', body | 'elsif', expr, body, [ ifrest ] ;
    static Object[] ifexpr() throws Exception {
		if( getToken() == ELSEIF )
			over(ELSEIF);
		else
			over(IF);

		Object[] cond = expr();
		Object[] thenpart = body();
		if( getToken() != ELSE && getToken() != ELSEIF )
			return new Object[]{"IF1",cond,thenpart};
		if( getToken() == ELSEIF )
			return new Object[]{"IF2",cond,thenpart,ifexpr()};
		over(ELSE);
		return new Object[]{"IF2",cond,thenpart,body()};
    }

    static Object[] body() throws Exception {
		over('{');
		expr();
		over(';');
		while (getToken() != '}') {
			expr();
			over(';');
		}
		over('}');
		return null;
	}

    // None of the following is needed in the parser
    static void generateProgram( String filename, Vector<Object[]> funs ) {
		String programname = filename.substring(0,filename.lastIndexOf('.'));
		emit("\"%s.mexe\" = main in",programname);
		emit("!");
		emit("{{");
		for( Object[] f: funs ) generateFunction(f);
		emit("}}");
		emit("*");
		emit("BASIS;");
	}

    static void emit( String fmt, Object... args ) {
		System.out.format(fmt,args);
		System.out.println();
	}

    static void generateFunction( Object[] fun ) {
		//...
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

	static void generateExpr( Object[] e ) {
		//...
	}

	static void generateBody( Object[] bod ) {
		//...
	}

	static public void main( String[] args ) throws Exception {
		Vector<Object[]> code = null;
		try {
			NanoMorphoLexer.startLexer(args[0]);
			code = program();
			if( getToken() != EOF ) expected("end of file");
		}
		catch(Throwable e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		// For the parser you do not need this line:
		generateProgram(args[0],code);
	}
}
