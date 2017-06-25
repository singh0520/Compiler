package cop5556sp17;

import cop5556sp17.AST.Dec;
import java.util.*;

public class SymbolTable {

	Stack<Integer> stStack = new Stack<Integer>();
	HashMap<String, ArrayList<table>> hm = new HashMap<String, ArrayList<table>>();
	int currentScope, nextScope;

	/**
	 * to be called when block entered
	 */

	public void enterScope() {
		// currentScope++;
		currentScope = nextScope++;
		stStack.push(currentScope);
	}

	/**
	 * leaves scope
	 */

	public void leaveScope() {
		stStack.pop();
		currentScope = stStack.peek();

	}

	public boolean insert(String ident, Dec dec) {
		ArrayList<table> temp = new ArrayList<table>();
		table t = new table(currentScope, dec);
		if (hm.containsKey(ident)) {
			temp = hm.get(ident);
			for (table tb : temp) {
				if (tb.scope == currentScope)
					return false;
			}
		}
		temp.add(t);
		hm.put(ident, temp);
		return true;
	}

	public Dec lookup(String ident) {
		if (!hm.containsKey(ident))
			return null;

		Dec dec = null;
		ArrayList<table> ps = hm.get(ident);
		for (int i = ps.size() - 1; i >= 0; i--) {
			int temp_scope = ps.get(i).getScope();
			if (stStack.contains(temp_scope)) {
				dec = ps.get(i).getDec();
				break;
			}
		}
		return dec;
	}

	public SymbolTable() {
		this.currentScope = 0;
		this.nextScope = 1;
		stStack.push(0);
	}

	@Override
	public String toString() {
		return this.toString();
	}

	public class table {
		int scope;
		Dec dec;

		public table(int temp_scope, Dec temp_dec) {
			this.scope = temp_scope;
			this.dec = temp_dec;
		}

		public int getScope() {
			return scope;
		}

		public Dec getDec() {
			return dec;
		}
	}

}
