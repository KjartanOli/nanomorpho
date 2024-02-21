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
		while (getToken().type() != Token.EOF)
			function();
		return new Vector<Object[]>();
	}

	static Object[] function() throws Exception {
		varCount = 0;
		varTable = new HashMap<String,Integer>();

		over(Token.FUN);
		var name = over(Token.NAME);
		over('(');
		parameter_list();
		over(')');
		function_body();

		// We're done with this symbol table so we'll erase it
		varTable = null;
		return null;
	}

	static Object[] parameter_list() throws Exception {
		if (getToken().type() != Token.NAME)
			return null;
		over(Token.NAME);
		while (getToken().type() == ',') {
			over(',');
			over(Token.NAME);
		}
		return null;
	}

	static Object[] declaration_list() throws Exception {
		while (getToken().type() == Token.VAR) {
			decl();
			over(';');
		}
		return null;
	}

	static Object[] function_body() throws Exception {
		over('{');
		declaration_list();
		while (getToken().type() != '}') {
			expr();
			over(';');
		}
		over('}');
		return null;
	}

    // decl = 'var', Token.NAME, { ',' Token.NAME } ;
    static void decl() throws Exception
    {
			over(Token.VAR);
			addVar(over(Token.NAME).lexeme());
			while( getToken().type() == ',' )
				{
					over(',');
					addVar(over(Token.NAME).lexeme());
				}
		}

    static Object[] expr() throws Exception
    {
			if (getToken().type() == Token.RETURN) {
				over(Token.RETURN);
				expr();
			}
			else if (getToken().type() == Token.NAME && getToken2().type() == '=') {
				over(Token.NAME);
				over('=');
				expr();
			}
			else {
				binopexpr(Token.OP1);
			}
			return null;
		}

    // For the parser you do not need the pri argument and
    // in the compiler you only need it if you wish to
    // distinguish priorities of operators
    static Object[] binopexpr( int pri ) throws Exception {
		smallexpr();
		while (Token.OP1 <= getToken().type() && getToken().type() <= Token.OP7) {
			advance();
			smallexpr();
		}
		return null;
	}

    static Object[] smallexpr() throws Exception {
		switch (getToken().type()) {
			case Token.NAME:
				over(Token.NAME);
				if (getToken().type() == '(') {
					over('(');
					if (getToken().type() != ')') {
						expr();
						while (getToken().type() == ',') {
							over(',');
							expr();
						}
					}
					over(')');
				}
				break;
			case Token.OP1:
			case Token.OP2:
			case Token.OP3:
			case Token.OP4:
			case Token.OP5:
			case Token.OP6:
			case Token.OP7:
				advance();
				smallexpr();
				break;
			case Token.LITERAL:
				over(Token.LITERAL);
				break;
			case '(':
				over('(');
				expr();
				over(')');
				break;
			case Token.IF:
				ifexpr();
				break;
			case Token.ELSEIF:
				expected("'if' before 'elseif'");
				break;
			case Token.ELSE:
				expected("'if' before 'else'");
				break;
			case Token.WHILE:
				over(Token.WHILE);
				expr();
				body();
				break;
		}
		//...
		return null;
	}

    // Alternative syntax with 'elsif'.
    // Slightly more complicated and requires that
    // the caller verifies beforehand that 'if'
    // and 'elsif' are used in their proper places.

    // ifexpr = 'if', expr, body, [ ifrest ] ;
    // ifrest = 'else', body | 'elsif', expr, body, [ ifrest ] ;
    static Object[] ifexpr() throws Exception {
		if( getToken().type() == Token.ELSEIF )
			over(Token.ELSEIF);
		else
			over(Token.IF);

		Object[] cond = expr();
		Object[] thenpart = body();
		if( getToken().type() != Token.ELSE && getToken().type() != Token.ELSEIF )
			return new Object[]{"IF1",cond,thenpart};
		if( getToken().type() == Token.ELSEIF )
			return new Object[]{"IF2",cond,thenpart,ifexpr()};
		over(Token.ELSE);
		return new Object[]{"IF2",cond,thenpart,body()};
    }

    static Object[] body() throws Exception {
		over('{');
		expr();
		over(';');
		while (getToken().type() != '}') {
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
