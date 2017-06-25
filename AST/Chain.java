package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.Type.TypeName;


public abstract class Chain extends Statement {
	public TypeName val;
	public boolean bo;
	
	
	public void setTypename(TypeName type){
		this.val = type;
	}
	
	public Chain(Token firstToken) {
		super(firstToken);
	}
	
	public TypeName getTypename(){
		return val;
	}

	public void setLeft(boolean b) {
		this.bo = b;
	}
	
	public boolean isLeft(){
		return bo;
	}
	

}
