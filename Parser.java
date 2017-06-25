package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import static cop5556sp17.Scanner.Kind.*;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.*;
import java.util.*;

public class Parser {

	/**
	 * Exception to be thrown if a syntax error is detected in the input. You
	 * will want to provide a useful error message.
	 *
	 */
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		public SyntaxException(String message) {
			super(message);
		}
	}

	/**
	 * Useful during development to ensure unimplemented routines are not
	 * accidentally called during development. Delete it when the Parser is
	 * finished.
	 *
	 */
	@SuppressWarnings("serial")
	public static class UnimplementedFeatureException extends RuntimeException {
		public UnimplementedFeatureException() {
			super();
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * parse the input using tokens from the scanner. Check for EOF (i.e. no
	 * trailing junk) when finished
	 * 
	 * @throws SyntaxException
	 */
	Program parse() throws SyntaxException {
		Program p = null;
		p = program();
		matchEOF();
		return p;
	}

	Expression expression() throws SyntaxException {
		Token temp = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = term();
		while (t.kind.equals(LE) || t.kind.equals(LT) || t.kind.equals(GT) || t.kind.equals(GE) || t.kind.equals(EQUAL)
				|| t.kind.equals(NOTEQUAL)) {
			Token op = t;
			consume();
			e1 = term();
			e0 = new BinaryExpression(temp, e0, op, e1);
		}
		return e0;
	}

	Expression term() throws SyntaxException {
		Token temp = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = elem();
		while (t.kind.equals(PLUS) || t.kind.equals(MINUS) || t.kind.equals(OR)) {
			Token op = t;
			consume();
			e1 = elem();
			e0 = new BinaryExpression(temp, e0, op, e1);
		}
		return e0;
	}

	Expression elem() throws SyntaxException {
		Token temp = t;
		Expression e0 = null;
		Expression e1 = null;
		e0 = factor();
		while (t.kind.equals(TIMES) || t.kind.equals(DIV) || t.kind.equals(AND) || t.kind.equals(MOD)) {
			Token op = t;
			
			consume();
			e1 = factor();
			e0 = new BinaryExpression(temp, e0, op, e1);
		}
		return e0;
	}

	void relOp() throws SyntaxException {
		if (t.kind.equals(LT) || t.kind.equals(LE) || t.kind.equals(GT) || t.kind.equals(GE) || t.kind.equals(EQUAL)
				|| t.kind.equals(NOTEQUAL))
			consume();
		else
			throw new SyntaxException("illegal relOp");

	}

	void weakOp() throws SyntaxException {
		if (t.kind.equals(PLUS) || t.kind.equals(MINUS) || t.kind.equals(OR))
			consume();
		else
			throw new SyntaxException("illegal weakOp");
	}

	void strongOp() throws SyntaxException {
		if (t.kind.equals(TIMES) || t.kind.equals(DIV) || t.kind.equals(AND) || t.kind.equals(MOD))
			consume();
		else
			throw new SyntaxException("illegal strongOp");
	}

	Expression factor() throws SyntaxException {
		Expression e = null;
		Kind kind = t.kind;
		switch (kind) {
		case IDENT: {
			e = new IdentExpression(t);
			consume();
		}
			break;
		case INT_LIT: {
			e = new IntLitExpression(t);
			consume();
		}
			break;
		case KW_TRUE:
		case KW_FALSE: {
			e = new BooleanLitExpression(t);
			consume();
		}
			break;
		case KW_SCREENWIDTH:
		case KW_SCREENHEIGHT: {
			e = new ConstantExpression(t);
			consume();
		}
			break;
		case LPAREN: {
			consume();
			e = expression();
			match(RPAREN);
		}
			break;
		default:
			throw new SyntaxException("illegal factor");
		}
		return e;
	}

	Block block() throws SyntaxException {
		Token temp = t;
		ArrayList<Dec> arr0 = new ArrayList<Dec>();
		ArrayList<Statement> arr1 = new ArrayList<Statement>();
		match(LBRACE);
		while (t.kind.equals(KW_INTEGER) || t.kind.equals(KW_BOOLEAN) || t.kind.equals(KW_IMAGE)
				|| t.kind.equals(KW_FRAME) || t.kind.equals(OP_SLEEP) || t.kind.equals(KW_WHILE) || t.kind.equals(KW_IF)
				|| t.kind.equals(IDENT) || t.kind.equals(OP_GRAY) || t.kind.equals(OP_BLUR)
				|| t.kind.equals(OP_CONVOLVE) || t.kind.equals(KW_SHOW) || t.kind.equals(KW_HIDE)
				|| t.kind.equals(KW_MOVE) || t.kind.equals(KW_XLOC) || t.kind.equals(KW_YLOC) || t.kind.equals(OP_WIDTH)
				|| t.kind.equals(OP_HEIGHT) || t.kind.equals(KW_SCALE)) {
			if (t.kind.equals(KW_INTEGER) || t.kind.equals(KW_BOOLEAN) || t.kind.equals(KW_IMAGE)
					|| t.kind.equals(KW_FRAME)) {
				arr0.add(dec());
			} else {
				arr1.add(statement());
			}
		}
		match(RBRACE);
		Block b = new Block(temp, arr0, arr1);
		return b;

	}

	Program program() throws SyntaxException {
		Token temp = t;
		ArrayList<ParamDec> arr0 = new ArrayList<ParamDec>();
		match(IDENT);
		Block b = null;
		if (t.kind.equals(LBRACE)) {
			b = block();
		} else if (t.kind.equals(KW_URL) || t.kind.equals(KW_FILE) || t.kind.equals(KW_INTEGER)
				|| t.kind.equals(KW_BOOLEAN)) {
			arr0.add(paramDec());
			while (t.kind.equals(COMMA)) {
				consume();
				arr0.add(paramDec());
			}
			b = block();
		} else
			throw new SyntaxException("illegal program");

		Program p = new Program(temp, arr0, b);
		return p;
	}

	ParamDec paramDec() throws SyntaxException {
		ParamDec param = null;
		if (t.kind.equals(KW_INTEGER) || t.kind.equals(KW_URL) || t.kind.equals(KW_FILE) || t.kind.equals(KW_BOOLEAN)) {
			Token temp = t;
			consume();
			Token op = t;
			match(IDENT);
			param = new ParamDec(temp, op);
		} else {
			throw new SyntaxException("illegal paramDec");
		}
		return param;
	}

	Dec dec() throws SyntaxException {
		Dec dec = null;
		if (t.kind.equals(KW_INTEGER) || t.kind.equals(KW_BOOLEAN) || t.kind.equals(KW_IMAGE)
				|| t.kind.equals(KW_FRAME)) {
			Token temp = t;
			consume();
			Token op = t;
			match(IDENT);
			dec = new Dec(temp, op);
		} else {
			throw new SyntaxException("illegal Dec");
		}
		return dec;
	}

	Statement whileStatement() throws SyntaxException {
		Token temp = t;
		Expression e = null;
		Block b = null;
		Statement s = null;
		match(KW_WHILE);
		match(LPAREN);
		e = expression();
		match(RPAREN);
		b = block();
		s = new WhileStatement(temp, e, b);
		return s;

	}

	Statement ifStatement() throws SyntaxException {
		Token temp = t;
		Expression e = null;
		Block b = null;
		Statement s = null;
		match(KW_IF);
		match(LPAREN);
		e = expression();
		match(RPAREN);
		b = block();
		s = new IfStatement(temp, e, b);
		return s;
	}

	Statement statement() throws SyntaxException {
		Statement s = null;
		Expression e = null;
		if (t.kind.equals(OP_SLEEP)) {
			Token temp = t;
			consume();
			e = expression();
			match(SEMI);
			s = new SleepStatement(temp, e);
		}

		else if (t.kind.equals(KW_WHILE)) {
			s = whileStatement();
		}

		else if (t.kind.equals(KW_IF)) {
			s = ifStatement();
		}

		else if (t.kind.equals(IDENT)) {
			if (scanner.peek().kind == Kind.ASSIGN) {
				s = assign();
				match(SEMI);
			} else {
				s = chain();
				match(SEMI);
			}
		} else if (t.kind.equals(OP_BLUR) || t.kind.equals(OP_GRAY) || t.kind.equals(OP_CONVOLVE)
				|| t.kind.equals(KW_SHOW) || t.kind.equals(KW_HIDE) || t.kind.equals(KW_MOVE) || t.kind.equals(KW_XLOC)
				|| t.kind.equals(KW_YLOC) || t.kind.equals(OP_WIDTH) || t.kind.equals(OP_HEIGHT)
				|| t.kind.equals(KW_SCALE)) {
			s = chain();
			match(SEMI);
		} else {
			throw new SyntaxException("illegal statement");
		}

		return s;
	}

	Chain chain() throws SyntaxException {
		Token temp = t, op = null;
		Chain ch = null;
		ChainElem ce = null;
		ch = chainElem();
		op = t;
		arrowOp();
		ce = chainElem();
		ch = new BinaryChain(temp, ch, op, ce);
		while (t.kind.equals(ARROW) || t.kind.equals(BARARROW)) {
			op = t;
			arrowOp();
			ce = chainElem();
			ch = new BinaryChain(temp, ch, op, ce);
		}
		return ch;
	}

	AssignmentStatement assign() throws SyntaxException {
		Token temp = t;
		IdentLValue l = new IdentLValue(t);
		Expression e = null;
		AssignmentStatement as = null;
		match(IDENT);
		match(ASSIGN);
		e = expression();
		as = new AssignmentStatement(temp, l, e);
		return as;
	}

	void arrowOp() throws SyntaxException {
		if (t.kind.equals(ARROW) || t.kind.equals(BARARROW)) {
			consume();
		} else {
			throw new SyntaxException("illegal arrowOp");
		}
	}

	ChainElem chainElem() throws SyntaxException {
		ChainElem ce = null;
		Token op = null;
		Tuple tu = null;
		if (t.kind.equals(IDENT)) {
			ce = new IdentChain(t);
			consume();
		} else if (t.kind.equals(OP_BLUR) || t.kind.equals(OP_GRAY) || t.kind.equals(OP_CONVOLVE)) {
			op = t;
			filterOp();
			tu = arg();
			ce = new FilterOpChain(op, tu);
		} else if (t.kind.equals(KW_SHOW) || t.kind.equals(KW_HIDE) || t.kind.equals(KW_MOVE) || t.kind.equals(KW_XLOC)
				|| t.kind.equals(KW_YLOC)) {
			op = t;
			frameOp();
			tu = arg();
			ce = new FrameOpChain(op, tu);
		} else if (t.kind.equals(OP_WIDTH) || t.kind.equals(OP_HEIGHT) || t.kind.equals(KW_SCALE)) {
			op = t;
			imageOp();
			tu = arg();
			ce = new ImageOpChain(op, tu);
		} else {
			throw new SyntaxException("illegal chainElem");
		}

		return ce;
	}

	void filterOp() throws SyntaxException {
		if (t.kind.equals(OP_BLUR) || t.kind.equals(OP_GRAY) || t.kind.equals(OP_CONVOLVE)) {
			consume();
		} else {
			throw new SyntaxException("illegal filterOp");
		}
	}

	void frameOp() throws SyntaxException {
		if (t.kind.equals(KW_SHOW) || t.kind.equals(KW_HIDE) || t.kind.equals(KW_MOVE) || t.kind.equals(KW_XLOC)
				|| t.kind.equals(KW_YLOC)) {
			consume();
		} else {
			throw new SyntaxException("illegal frameOp");
		}
	}

	void imageOp() throws SyntaxException {
		if (t.kind.equals(OP_WIDTH) || t.kind.equals(OP_HEIGHT) || t.kind.equals(KW_SCALE)) {
			consume();
		} else {
			throw new SyntaxException("illegal imageOp");
		}
	}

	Tuple arg() throws SyntaxException {
		ArrayList<Expression> arr = new ArrayList<Expression>();
		Tuple tp = null;
		Token temp = null;
		if (t.kind.equals(LPAREN)) {
			temp = t;
			consume();
			arr.add(expression());
			while (t.kind.equals(COMMA)) {
				consume();
				arr.add(expression());
			}
			match(RPAREN);

		}
		tp = new Tuple(temp, arr);
		return tp;
	}

	/**
	 * Checks whether the current token is the EOF token. If not, a
	 * SyntaxException is thrown.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.kind.equals(EOF)) {
			return t;
		}
		throw new SyntaxException("expected EOF");
	}

	/**
	 * Checks if the current token has the given kind. If so, the current token
	 * is consumed and returned. If not, a SyntaxException is thrown.
	 * 
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		if (t.kind.equals(kind)) {
			return consume();
		}
		throw new SyntaxException("saw " + t.kind + "expected " + kind);
	}

	/**
	 * Checks if the current token has one of the given kinds. If so, the
	 * current token is consumed and returned. If not, a SyntaxException is
	 * thrown.
	 * 
	 * * Precondition: for all given kinds, kind != EOF
	 * 
	 * @param kinds
	 *            list of kinds, matches any one
	 * @return
	 * @throws SyntaxException
	 */

	/**
	 * Gets the next token and returns the consumed token.
	 * 
	 * Precondition: t.kind != EOF
	 * 
	 * @return
	 * 
	 */
	private Token consume() throws SyntaxException {
		Token tmp = t;
		t = scanner.nextToken();
		return tmp;
	}

}
