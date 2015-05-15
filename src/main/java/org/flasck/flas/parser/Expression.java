package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class Expression implements TryParsing {
	int SHIFT = 1;
	int SHIFTU = 2;
	int REDUCE = 3;

	public class OpState {
		int idx;
		int action;
		int prec;

		public OpState(int idx, int action, int prec) {
			this.idx = idx;
			this.action = action;
			this.prec = prec;
		}
	}

	@Override
	public Object tryParsing(Tokenizable line) {
		int mark = line.at(); // do this now in case we need it later
		ExprToken s = ExprToken.from(line);
		System.out.println("Start " + s);
		if (s.type == ExprToken.NUMBER)
			return new ItemExpr(s);
		if (s.type == ExprToken.IDENTIFIER || s.type == ExprToken.SYMBOL) {
			List<Object> args = new ArrayList<Object>();
			args.add(new ItemExpr(s));
			while (line.hasMore()) {
				mark = line.at();
				s = ExprToken.from(line);
				System.out.println("Inner " + s);
				if (s.type == ExprToken.PUNC && s.text.equals("(")) {
					args.add(parseParenthetical(line));
				} else if (s.type == ExprToken.PUNC && s.text.equals(")")) {
					line.reset(mark);
					break;
				} else if (s.type == ExprToken.PUNC) {
					System.out.println("Random PUNC");
					return null; // an error
				} else
					args.add(new ItemExpr(s));
			}
			if (args.size() == 1)
				return args.get(0);
			else
				return opstack(args);
		} else if (s.type == ExprToken.PUNC && s.text.equals("(")) {
			return parseParenthetical(line);
		} else {
			// error reporting - some sort of syntax error
			System.out.println("What was this? " + s);
			return null;
		}
	}

	// By the time we get here, all the inner parentheses should have been resolved.
	// But we may have any combination of symbols, names and constants in any order
	// We now need to resolve those by operator precedence parsing
	private Object opstack(List<Object> args) {
		// Step 1.  The trickiest thing is to handle all the straightforward function calls, so do that first
		for (int i=0,j=0;i<=args.size();i++) {
			System.out.println("with " + j + " and " + i + " of " + args.size());
			if (i == args.size() || isSymbol(args.get(i))) {
				if (i>j+1) { // collapse a fn defn to the left
					System.out.println("Collapsing " + j + " to " + i);
					List<Object> inargs = new ArrayList<Object>();
					for (int k=j+1;k<i;k++) {
						System.out.println("Removing " + (j+1));
						inargs.add(args.remove(j+1));
					}
					Object op = args.remove(j);
					System.out.println("Op = " + ((ItemExpr)op).tok.text);
					ApplyExpr ae = new ApplyExpr(op, inargs);
					args.add(j, ae);
				}
				j=i=j+2;
				
			}
		}
		// Now everything should be in the form A op B op C, i.e. with interleaved expressions and operators
		// The exception is for unary operators, in which case we might see e.g. 2 + ~3
		// We now do straightforward precedence/associate shift-reduce
		List<OpState> stack = new ArrayList<OpState>();
		for (int i=0;i<=args.size();i++) {
			if (i == args.size() || isSymbol(args.get(i))) {
				// we either need to shift this symbol OR reduce what's to the left
				int action = SHIFT;
				OpState prev = null;
				if (stack.size() > 0)
					prev = stack.get(0);
				do {
					int myprec = -1;
					if (i == args.size()) {// at end, reduce to nothing
						if (i == 1)
							break;
						action = REDUCE;
					}
					else if (i < 2)   // at start, shift is only option
						action = SHIFT;
					else if (prev.idx == i-1) // if previous token was an operator, this must be a unary operator
						action = SHIFTU;
					else if (prev.action == SHIFTU)  // if previous was a unary operator, reduce
						action = REDUCE;
					else  {// apply the shift/reduce precedence
						action = compareActions(prev.prec, myprec);
						myprec = precedence(((ItemExpr)args.get(i)).tok.text);
					}
					if (action == SHIFT || action == SHIFTU)
						stack.add(0,new OpState(i, action, myprec));
					else if (prev.action == SHIFTU) {
						System.out.println("prefix:"+args.size());
						if (stack.size() > 0)
							stack.remove(0);
						Object o1 = args.remove(i-2);
						Object o2 = args.remove(i-2);
						args.add(i-2, new ApplyExpr(o1, o2));
						i--;
					} else {
						System.out.println("infix:" +args.size());
						if (stack.size() > 0)
							stack.remove(0);
						Object o1 = args.remove(i-3);
						Object o2 = args.remove(i-3);
						Object o3 = args.remove(i-3);
						args.add(i-3, new ApplyExpr(o2, o1, o3));
						i-=2;
					}
				} while (action == REDUCE);
			}
		}
		return args.remove(0);
	}

	private int compareActions(int prec, int myprec) {
		// TODO Auto-generated method stub
		return 0;
	}

	private int precedence(String text) {
		// TODO Auto-generated method stub
		return 0;
	}

	private boolean isSymbol(Object o) {
		return o instanceof ItemExpr && ((ItemExpr)o).tok.type == ExprToken.SYMBOL;
	}

	private Object parseParenthetical(Tokenizable line) {
		System.out.println("nested");
		Object ret = tryParsing(line);
		ExprToken crb = ExprToken.from(line);
		if (crb.type != ExprToken.PUNC || !crb.text.equals(")"))
			System.out.println("this would be an error");
		return ret;
	}
}
