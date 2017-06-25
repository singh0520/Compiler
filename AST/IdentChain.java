package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;

public class IdentChain extends ChainElem {

	public Dec identDec;
	public Object chainType;

	public IdentChain(Token firstToken) {
		super(firstToken);
	}

	public Dec getDec() {
		return identDec;
	}

	public void setDec(Dec id) {
		this.identDec = id;
	}

	@Override
	public String toString() {
		return "IdentChain [firstToken=" + firstToken + "]";
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitIdentChain(this, arg);
	}

}
