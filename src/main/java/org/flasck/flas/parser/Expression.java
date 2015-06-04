package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class Expression implements TryParsing {
	int SHIFT = 1;
	int REDUCE = 3;

	public class OpState {
		int idx;
		boolean prefix;
		int prec;

		public OpState(int idx, boolean prefix, int prec) {
			this.idx = idx;
			this.prefix = prefix;
			this.prec = prec;
		}
		
		@Override
		public String toString() {
			return "["+idx+";"+prefix+";"+prec+"]";
		}
	}

	private static class ParenExpr {
		final Object nested;

		public ParenExpr(Object nested) {
			this.nested = nested;
		}
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		int mark = line.at(); // do this now in case we need it later
		ExprToken s = ExprToken.from(line);
		if (s.type == ExprToken.NUMBER || s.type == ExprToken.IDENTIFIER || s.type == ExprToken.SYMBOL) {
			List<Object> args = new ArrayList<Object>();
			args.add(new ItemExpr(s));
			while (line.hasMore()) {
				mark = line.at();
				s = ExprToken.from(line);
				if (s.type == ExprToken.PUNC && s.text.equals("(")) { // I think this probably could be "[" as well
					args.add(parseParenthetical(line, ")"));
				} else if (s.type == ExprToken.PUNC && (s.text.equals(")") || s.text.equals(",") || s.text.equals("]"))) {
					line.reset(mark);
					break;
				} else if (s.type == ExprToken.PUNC) {
					if (s.text.equals("."))
						args.add(new ItemExpr(s));
					else {
						System.out.println("Random PUNC");
						return ErrorResult.oneMessage(line, "unrecognized punctuation");
					}
				} else
					args.add(new ItemExpr(s));
			}
			if (args.size() == 1)
				return deparen(args.get(0));
			else
				return deparen(opstack(args));
		} else if (s.type == ExprToken.PUNC) {
			if (s.text.equals("(") || s.text.equals("[")) {
				return parseParenthetical(line, s.text.equals("(")?")":"]");
			} else if (s.text.equals(")") || s.text.equals("]") || s.text.equals(",")) {
				line.reset(mark);
				return null;
			} else {
				System.out.println("What was this punc? " + s);
				return null;
			}
		} else {
			// error reporting - some sort of syntax error
			System.out.println("What was this? " + s);
			return null;
		}
	}

	private static Object rehash(Object o) {
		if (o instanceof ItemExpr) {
			ExprToken pe = ((ItemExpr)o).tok;
			if (pe.type == ExprToken.SYMBOL && pe.text.equals(":"))
				return new ItemExpr (new ExprToken(ExprToken.IDENTIFIER, "Cons"));
		}
		return o;
	}

	// By the time we get here, all the inner parentheses should have been resolved.
	// But we may have any combination of symbols, names and constants in any order
	// We now need to resolve those by operator precedence parsing
	private Object opstack(List<Object> args) {
		// Step 1. Reduce all the "." operators before ANYTHING else
		for (int i=0;i<args.size()-1;i++) {
			if (isDot(args.get(i))) {
				--i;
				Object left = args.remove(i);
				Object dot = args.remove(i);
				Object right = args.remove(i);
				ApplyExpr ae = new ApplyExpr(dot, left, right);
				args.add(i, ae);
			}
		}
		
		// Step 2.  The trickiest thing is to handle all the straightforward function calls, so do that next
		for (int i=0,j=0;i<=args.size();i++) {
			if (i == args.size() || isSymbol(args.get(i))) {
				if (i>j+1) { // collapse a fn defn to the left
					List<Object> inargs = new ArrayList<Object>();
					for (int k=j+1;k<i;k++) {
						inargs.add(deparen(args.remove(j+1)));
					}
					Object op = args.remove(j);
					ApplyExpr ae = new ApplyExpr(deparen(op), inargs);
					args.add(j, ae);
				}
				if (j == i)
					j=i+1;
				else
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
				do {
					OpState prev = null;
					if (stack.size() > 0)
						prev = stack.get(0);
					int myprec = -1;
					String op = "";
					boolean prefix = false;
					if (i == args.size()) {// at end, reduce to nothing
						if (i == 1)
							break;
						action = REDUCE;
					} else {
						op = ((ItemExpr)args.get(i)).tok.text;
						myprec = precedence(op);
						if (i == 0 || prev != null && prev.idx == i-1) { // if first token or if previous token was an operator, this must be a unary operator
							action = SHIFT;
							myprec = unaryprec(op);
							prefix = true;
						} else if (i == 1)   // for first binary operator, shift is only option
							action = SHIFT;
						else  {// apply the shift/reduce precedence
							action = compareActions(prev.prec, myprec);
						}
					}
					if (action == SHIFT)
						stack.add(0, new OpState(i, prefix, myprec));
					else if (prev.prefix) {
						if (stack.size() > 0)
							stack.remove(0);
						Object o1 = deparen(args.remove(i-2));
						Object o2 = deparen(args.remove(i-2));
						args.add(i-2, new ApplyExpr(o1, o2));
						i--;
					}
					else {
						if (stack.size() > 0)
							stack.remove(0);
						Object o1 = deparen(args.remove(i-3));
						Object o2 = deparen(args.remove(i-3));
						Object o3 = deparen(args.remove(i-3));
						if (isCurryVar(o1) && isCurryVar(o3))
							return new ParenExpr(o2);
						else if (isCurryVar(o1))
							args.add(i-3, new ApplyExpr(curry1st(o2), o3));
						else if (isCurryVar(o3))
							args.add(i-3, new ApplyExpr(o2, o1));
						else
							args.add(i-3, new ApplyExpr(o2, o1, o3));
						i-=2;
					}
				} while (action == REDUCE);
			}
		}
		return args.remove(0);
	}

	private boolean isDot(Object tok) {
		return tok instanceof ItemExpr && ((ItemExpr)tok).tok.text.equals(".");
	}

	public static Object deparen(Object pe) {
		while (pe instanceof ParenExpr)
			pe = ((ParenExpr)pe).nested;
		return rehash(pe);
	}

	private int compareActions(int prec, int myprec) {
		if (myprec < prec) // I have higher precedence
			return SHIFT;
		else if (myprec == prec && myprec % 2 == 0) // even numbers associate right
			return SHIFT;
		else
			return REDUCE;
	}

	private int precedence(String text) {
		if (text.equals("."))
			return 1;
		else if (text.equals("^"))
			return 3;
		else if (text.equals("*") || text.equals("/"))
			return 5;
		else if (text.equals("+") || text.equals("-"))
			return 7;
		else
			return 99; // default is to say it must be the lowest precedence
	}

	private int unaryprec(String text) {
		if (text.equals("-"))
			return 1;
		else
			return 99;
	}

	private boolean isSymbol(Object o) {
		return o instanceof ItemExpr && ((ItemExpr)o).tok.type == ExprToken.SYMBOL;
	}

	private boolean isCurryVar(Object o) {
		return o instanceof ItemExpr && ((ItemExpr)o).tok.text.equals("_");
	}

	private Object curry1st(Object o) {
		return new ItemExpr(new ExprToken(ExprToken.SYMBOL, ((ItemExpr)o).tok.text+"_"));
	}

	private Object parseParenthetical(Tokenizable line, String endsWith) {
		List<Object> objs = new ArrayList<Object>();
		while (true) {
			// TODO: I'm not sure about this way of doing it
			// It seems to me that [,,] might end up not an error but []
			Object expr = tryParsing(line);
			if (expr != null)
				objs.add(expr);
			ExprToken crb = ExprToken.from(line);
			if (crb.type == ExprToken.PUNC) {
				if (crb.text.equals(endsWith)) {
					if (endsWith.equals(")")) {
						if (objs.size() == 0) {
							System.out.println("()?");
							return null;
						}
						else if (objs.size() == 1)
							return new ParenExpr(objs.get(0));
						else {
							// The tuple case
							return new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.SYMBOL, "()")), objs);
						}
					}
					else if (endsWith.equals("]")) {
						Object base = new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Nil"));
						for (int i=objs.size()-1;i>=0;i--)
							base = new ApplyExpr(new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, "Cons")), objs.get(i), base);
						return base;
					} else {
						System.out.println("huh?");
						return null;
					}
				} else if (crb.text.equals(",")) {
				} else {
					System.out.println("this is an error");
					return null;
				}
			} else
				System.out.println("this would be an error");
		}
	}
}
