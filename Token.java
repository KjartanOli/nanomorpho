class Token {
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

	public static String name(int type) {
		if(type < 1000)
			return String.format("'%c'", (char) type);
		switch(type) {
			case IF:
				return "IF";
			case ELSE:
				return "ELSE";
			case ELSEIF:
				return "ELSEIF";
			case WHILE:
				return "WHILE";
			case VAR:
				return "VAR";
			case RETURN:
				return "RETURN";
			case NAME:
				return "NAME";
			case LITERAL:
				return "LITERAL";
			case OP1:
				return "OP1";
			case OP2:
				return "OP2";
			case OP3:
				return "OP3";
			case OP4:
				return "OP4";
			case OP5:
				return "OP5";
			case OP6:
				return "OP6";
			case OP7:
				return "OP7";
		}
		throw new Error();
	}

	private final int _type;
	private final String _lexeme;

	public Token(int type, String lexeme) {
		this._type = type;
		this._lexeme = lexeme;
	}

	public String lexeme() {
		return this._lexeme;
	}

	public String name() {
		return Token.name(this.type());
	}

	public int type() {
		return this._type;
	}
}
