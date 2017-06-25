package cop5556sp17;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Scanner {

	public static enum State {
		START, IDENTIFIER, KEYWORD, FRAME_OP_KEYWORD, FILTER_OP_KEYWORD, IMAGE_OP_KEYWORD, BOOLEAN_LITERAL, INT_LITERAL, SEPARATOR, OPERATOR, AFTER_EQL, AFTER_LESS_SIGN, AFTER_GREAT_SIGN, AFTER_LINE, AFTER_EXCLA, AFTER_HIGH, AFTER_SLASH, COMMENT;

	}

	public static enum Kind {
		IDENT(""), INT_LIT(""), KW_INTEGER("integer"), KW_BOOLEAN("boolean"), KW_IMAGE("image"), KW_URL("url"), KW_FILE(
				"file"), KW_FRAME("frame"), KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"), SEMI(
						";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"), RBRACE("}"), ARROW("->"), BARARROW(
								"|->"), OR("|"), AND("&"), EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(
										">="), PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"), ASSIGN(
												"<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE(
														"convolve"), KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH(
																"screenwidth"), OP_WIDTH("width"), OP_HEIGHT(
																		"height"), KW_XLOC("xloc"), KW_YLOC(
																				"yloc"), KW_HIDE("hide"), KW_SHOW(
																						"show"), KW_MOVE(
																								"move"), OP_SLEEP(
																										"sleep"), KW_SCALE(
																												"scale"), EOF(
																														"eof");

		Kind(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}

	public HashMap<String, Kind> hm = new HashMap<String, Kind>();

	/**
	 * Thrown by Scanner when an illegal character is encountered
	 */
	@SuppressWarnings("serial")
	public static class IllegalCharException extends Exception {
		public IllegalCharException(String message) {
			super(message);
		}
	}

	/**
	 * Thrown by Scanner when an int literal is not a value that can be
	 * represented by an int.
	 */
	@SuppressWarnings("serial")
	public static class IllegalNumberException extends Exception {
		public IllegalNumberException(String message) {
			super(message);
		}
	}

	/**
	 * Holds the line and position in the line of a token.
	 */
	static class LinePos {
		public final int line;
		public final int posInLine;

		public LinePos(int line, int posInLine) {
			super();
			this.line = line;
			this.posInLine = posInLine;
		}

		@Override
		public String toString() {
			return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
		}
	}

	public class Token {
		public final Kind kind;
		public final int pos;
		public final int length;

		// returns the text of this Token
		public String getText() {
			if (chars == "") {
				return null;
			} else {
				return (chars.substring(pos, pos + length));
			}
		}

		// returns a LinePos object representing the line and column of this
		// Token
		LinePos getLinePos() {
			int temp_index;
			temp_index = Collections.binarySearch(al, pos);
			int temp_line;
			temp_line = temp_index < 0 ? Math.abs(temp_index + 2) : temp_index;
			LinePos lp = new LinePos(temp_line, (pos - al.get(temp_line) - 1));
			return lp;
		}

		Token(Kind kind, int pos, int length) {
			this.kind = kind;
			this.pos = pos;
			this.length = length;
		}

		/**
		 * Precondition: kind = Kind.INT_LIT, the text can be represented with a
		 * Java int. Note that the validity of the inhm.put should have been
		 * checked when the Token was created. So the exception should never be
		 * thrown.
		 * 
		 * @return int value of this token, which should represent an INT_LIT
		 * @throws NumberFormatException
		 */
		public int intVal() throws NumberFormatException {
			// return 0;
			return (Integer.parseInt(chars.substring(pos, pos + length)));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
			result = prime * result + length;
			result = prime * result + pos;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Token)) {
				return false;
			}
			Token other = (Token) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (kind != other.kind) {
				return false;
			}
			if (length != other.length) {
				return false;
			}
			if (pos != other.pos) {
				return false;
			}
			return true;
		}

		private Scanner getOuterType() {
			return Scanner.this;
		}

	}

	Scanner(String chars) {
		this.chars = chars;
		tokens = new ArrayList<Token>();
		hm.put("integer", Kind.KW_INTEGER);
		hm.put("boolean", Kind.KW_BOOLEAN);
		hm.put("image", Kind.KW_IMAGE);
		hm.put("url", Kind.KW_URL);
		hm.put("file", Kind.KW_FILE);
		hm.put("frame", Kind.KW_FRAME);
		hm.put("while", Kind.KW_WHILE);
		hm.put("if", Kind.KW_IF);
		hm.put("true", Kind.KW_TRUE);
		hm.put("false", Kind.KW_FALSE);
		hm.put("blur", Kind.OP_BLUR);
		hm.put("gray", Kind.OP_GRAY);
		hm.put("convolve", Kind.OP_CONVOLVE);
		hm.put("screenheight", Kind.KW_SCREENHEIGHT);
		hm.put("screenwidth", Kind.KW_SCREENWIDTH);
		hm.put("width", Kind.OP_WIDTH);
		hm.put("height", Kind.OP_HEIGHT);
		hm.put("xloc", Kind.KW_XLOC);
		hm.put("yloc", Kind.KW_YLOC);
		hm.put("hide", Kind.KW_HIDE);
		hm.put("show", Kind.KW_SHOW);
		hm.put("move", Kind.KW_MOVE);
		hm.put("sleep", Kind.OP_SLEEP);
		hm.put("scale", Kind.KW_SCALE);

	}

	public int skipWhiteSpaces(int temp_pos, int length) {

		while (Character.isWhitespace(chars.charAt(temp_pos))) {
			if (chars.charAt(temp_pos) == 10) {
				return temp_pos;
			} else {
				temp_pos++;
				if (temp_pos == length) {
					return temp_pos;
				}

			}

		}
		return temp_pos;
	}

	/**
	 * Initializes Scanner object by traversing chars and adding tokens to
	 * tokens list.
	 * 
	 * @return this scanner
	 * @throws IllegalCharException
	 * @throws IllegalNumberException
	 */
	public Scanner scan() throws IllegalCharException, IllegalNumberException {
		int pos = 0;
		al.add(-1);
		int length = chars.length();
		State state = State.START;
		int startPos = 0;
		int ch;
		StringBuilder temp = new StringBuilder();
		while (pos <= length) {
			ch = pos < length ? chars.charAt(pos) : -1;

			switch (state) {
			case START: {
				if (pos < length)
					pos = skipWhiteSpaces(pos, length);
				ch = pos < length ? chars.charAt(pos) : -1;
				startPos = pos;
				temp.setLength(0);
				switch (ch) {
				case -1: {
					tokens.add(new Token(Kind.EOF, pos, 0));
					pos++;
				}
					break;

				case '0': {
					tokens.add(new Token(Kind.INT_LIT, pos, 1));
					pos++;
				}
					break;

				case ';': {
					tokens.add(new Token(Kind.SEMI, pos, 1));
					pos++;
				}
					break;

				case ',': {
					tokens.add(new Token(Kind.COMMA, pos, 1));
					pos++;
				}
					break;

				case '(': {
					tokens.add(new Token(Kind.LPAREN, pos, 1));
					pos++;
				}
					break;

				case ')': {
					tokens.add(new Token(Kind.RPAREN, pos, 1));
					pos++;
				}
					break;

				case '{': {
					tokens.add(new Token(Kind.LBRACE, pos, 1));
					pos++;
				}
					break;

				case '}': {
					tokens.add(new Token(Kind.RBRACE, pos, 1));
					pos++;
				}
					break;

				case '|': {
					state = State.AFTER_LINE;
					temp.append((char) ch);
					pos++;
				}
					break;

				case '&': {
					tokens.add(new Token(Kind.AND, pos, 1));
					pos++;
				}
					break;

				case '=': {
					state = State.AFTER_EQL;
					temp.append((char) ch);
					pos++;
				}
					break;

				case '!': {
					state = State.AFTER_EXCLA;
					temp.append((char) ch);
					pos++;
				}
					break;

				case '<': {
					state = State.AFTER_LESS_SIGN;
					pos++;
				}
					break;

				case '>': {
					state = State.AFTER_GREAT_SIGN;
					pos++;
				}
					break;

				case '+': {
					tokens.add(new Token(Kind.PLUS, pos, 1));
					pos++;
				}
					break;

				case '-': {
					state = State.AFTER_HIGH;
					pos++;
				}
					break;

				case '*': {
					tokens.add(new Token(Kind.TIMES, pos, 1));
					pos++;
				}
					break;

				case '/': {
					state = State.AFTER_SLASH;
					temp.append((char) ch);
					pos++;
				}
					break;

				case '%': {
					tokens.add(new Token(Kind.MOD, pos, 1));
					pos++;
				}
					break;
				case 10: {
					al.add(pos);
					pos++;
				}
					break;

				default: {
					if (Character.isDigit(ch)) {
						state = State.INT_LITERAL;
						temp.append((char) ch);
						pos++;
					} else if (Character.isJavaIdentifierStart(ch)) {
						state = State.IDENTIFIER;
						temp.append((char) ch);
						pos++;
					} else {
						throw new IllegalCharException("illegal char " + ch + " at pos " + pos);
					}
				}
					break;

				}
			}
				break;

			case IDENTIFIER: {
				if (Character.isJavaIdentifierPart(ch)) {
					temp.append((char) ch);
					pos++;
				} else {
					if (hm.containsKey(temp.toString())) {
						tokens.add(new Token(hm.get(temp.toString()), startPos, pos - startPos));
					} else {
						tokens.add(new Token(Kind.IDENT, startPos, pos - startPos));
					}

					state = State.START;
				}
			}
				break;

			case INT_LITERAL: {
				if (Character.isDigit(ch)) {
					temp.append(Character.getNumericValue(ch));
					pos++;
				} else {
					try {
						Integer.parseInt(temp.toString());
						tokens.add(new Token(Kind.INT_LIT, startPos, pos - startPos));
					} catch (NumberFormatException e) {
						throw new IllegalNumberException("Number out of range!");
					}
					state = State.START;
				}
			}
				break;

			case AFTER_LINE: {
				int temp_pos = pos;
				if (ch == '-') {
					temp_pos++;
					ch = temp_pos < length ? chars.charAt(temp_pos) : -1;
					if (ch == '>') {
						pos = temp_pos;
						tokens.add(new Token(Kind.BARARROW, startPos, 3));
						pos++;
					} else {
						tokens.add(new Token(Kind.OR, startPos, 1));
					}
				} else {
					tokens.add(new Token(Kind.OR, startPos, 1));
				}
				state = State.START;

			}
				break;

			case AFTER_EQL: {
				if (ch == '=') {
					tokens.add(new Token(Kind.EQUAL, startPos, 2));
					pos++;
					state = State.START;
				} else {
					throw new IllegalCharException("illegal char " + temp + " at pos " + pos--);
				}
			}
				break;

			case AFTER_EXCLA: {
				if (ch == '=') {
					temp.append((char) ch);
					tokens.add(new Token(Kind.NOTEQUAL, startPos, 2));
					pos++;
					state = State.START;
				} else {
					tokens.add(new Token(Kind.NOT, startPos, 1));
					state = State.START;
				}
			}
				break;

			case AFTER_LESS_SIGN: {
				if (ch == '=') {
					temp.append((char) ch);
					tokens.add(new Token(Kind.LE, startPos, 2));
					pos++;
					state = State.START;
				}

				else if (ch == '-') {
					temp.append((char) ch);
					tokens.add(new Token(Kind.ASSIGN, startPos, 2));
					pos++;
					state = State.START;
				}

				else {
					tokens.add(new Token(Kind.LT, startPos, 1));
					state = State.START;
				}
			}
				break;

			case AFTER_GREAT_SIGN: {
				if (ch == '=') {
					temp.append((char) ch);
					tokens.add(new Token(Kind.GE, startPos, 2));
					pos++;
					state = State.START;
				} else {
					tokens.add(new Token(Kind.GT, startPos, 1));
					state = State.START;
				}
			}
				break;

			case AFTER_HIGH: {
				if (ch == '>') {
					temp.append((char) ch);
					tokens.add(new Token(Kind.ARROW, startPos, 2));
					pos++;
					state = State.START;
				} else {
					tokens.add(new Token(Kind.MINUS, startPos, 1));
					state = State.START;
				}
			}
				break;

			case AFTER_SLASH: {
				if (ch == '*') {
					state = State.COMMENT;
					pos++;
				} else {
					tokens.add(new Token(Kind.DIV, startPos, 1));
					state = State.START;
				}
			}
				break;

			case COMMENT: {
				boolean flag = true;
				while (ch != -1 && flag == true) {
					if (ch == '*') {
						pos++;
						ch = pos < length ? chars.charAt(pos) : -1;
						if (ch == '/') {
							flag = false;
							pos++;
						}
					} else {
						pos++;
						ch = pos < length ? chars.charAt(pos) : -1;
					}
				}
				state = State.START;
			}
				break;

			default:
				break;

			}
		}
		return this;
	}

	final ArrayList<Token> tokens;
	final ArrayList<Integer> al = new ArrayList<Integer>();
	final String chars;
	int tokenNum;

	/*
	 * Return the next token in the token list and update the state so that the
	 * next call will return the Token..
	 */
	public Token nextToken() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum++);
	}

	/*
	 * Return the next token in the token list without updating the state. (So
	 * the following call to next will return the same token.)
	 */public Token peek() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum);
	}

	/**
	 * Returns a LinePos object containing the line and position in line of the
	 * given token.
	 * 
	 * Line numbers start counting at 0
	 * 
	 * @param t
	 * @return
	 */
	public LinePos getLinePos(Token t) {
		return t.getLinePos();
		// return null;
	}

}
