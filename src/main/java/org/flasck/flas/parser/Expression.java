package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.UtilException;

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

	private static class ParenExpr implements Locatable {
		final Locatable nested;

		public ParenExpr(Object nested) {
			this.nested = (Locatable) nested;
		}

		@Override
		public InputPosition location() {
			return nested.location();
		}
		
		@Override
		public String toString() {
			return nested.toString();
		}
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		int mark = line.at(); // do this now in case we need it later
		ExprToken s = ExprToken.from(line);
		if (s == null)
			return null; // we presumably saw an error?
		if (s.type == ExprToken.PUNC) {
			if (s.text.equals("(") || s.text.equals("[")) {
				; // drop through to main case
			} else if (s.text.equals(")") || s.text.equals("]") || s.text.equals(",")) {
				line.reset(mark);
				return null;
			} else {
				System.out.println("What was this punc? " + s);
				return null;
			}
		}
		if (s.type == ExprToken.NUMBER || s.type == ExprToken.STRING || s.type == ExprToken.IDENTIFIER || s.type == ExprToken.SYMBOL || s.type == ExprToken.PUNC) {
			List<Object> args = new ArrayList<Object>();
			if (s.type != ExprToken.PUNC)
				args.add(ItemExpr.from(s));
			else {
				if (s.text.equals("(")) {
					Object pp = parseParenthetical(line, ")");
					if (pp == null || pp instanceof ErrorResult) return pp;
					args.add(pp);
				} else if (s.text.equals("[")) {
					Object pp = parseParenthetical(line, "]");
					if (pp == null || pp instanceof ErrorResult) return pp;
					args.add(pp);
				}
			}
			while (line.hasMore()) {
				mark = line.at();
				s = ExprToken.from(line);
				if (s == null)
					return null;
				if (s.type == ExprToken.PUNC && s.text.equals("(")) {
					Object pp = parseParenthetical(line, ")");
					if (pp == null || pp instanceof ErrorResult) return pp;
					args.add(pp);
				} else if (s.type == ExprToken.PUNC && s.text.equals("[")) {
					Object pp = parseParenthetical(line, "]");
					if (pp == null || pp instanceof ErrorResult) return pp;
					args.add(pp);
				} else if (s.type == ExprToken.PUNC && (s.text.equals(")") || s.text.equals(",") || s.text.equals("]"))) {
					line.reset(mark);
					break;
				} else if (s.type == ExprToken.PUNC) {
					if (s.text.equals("."))
						args.add(ItemExpr.from(s));
					else {
						System.out.println("Random PUNC");
						return ErrorResult.oneMessage(line, "unrecognized punctuation");
					}
				} else
					args.add(promoteConstructors(ItemExpr.from(s)));
			}
			if (args.size() == 2 && args.get(0) instanceof UnresolvedVar && ((UnresolvedVar)args.get(0)).var.equals("type") && (args.get(1) instanceof UnresolvedVar || args.get(1) instanceof ApplyExpr && ((ApplyExpr)args.get(1)).args.isEmpty())) {
				if (args.get(1) instanceof UnresolvedVar)
					return args.get(1);
				return ((ApplyExpr)args.get(1)).fn;
			} else if (args.size() == 3 && args.get(0) instanceof UnresolvedVar && ((UnresolvedVar)args.get(0)).var.equals("downcast")) {
				Object ex = args.get(1);
				if (ex instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) ex;
					if (ae.args.isEmpty())
						ex = ae.fn;
				}
				if (!(ex instanceof UnresolvedVar))
					return ErrorResult.oneMessage(((Locatable)args.get(1)).location(), "cannot downcast to " + args.get(1));
				UnresolvedVar type = (UnresolvedVar)ex;
				return new CastExpr(type.location, type.var, args.get(2));
			} else if (args.size() == 1)
				return promoteConstructors(deparen(args.get(0)));
			else
				return deparen(opstack(args));
		} else {
			// error reporting - some sort of syntax error
			System.out.println("What was this? " + s);
			return null;
		}
	}

	/* In 90% of cases, constructors are functions, even if they don't take arguments.
	 * The other 10% constitute the ones that are outside Cards (Handlers and the like take the card as an implicit argument).
	 * Technically, we only need to promote the ones that have hidden args, but that's hard to check here, but we do know that Nil is a common exception - handle that one specially
	 */
	private Object promoteConstructors(Object from) {
		if (from instanceof UnresolvedVar) {
			UnresolvedVar v = (UnresolvedVar) from;
			if (Character.isUpperCase(v.var.charAt(0)) && !v.var.equals("Nil"))
				return new ApplyExpr(v.location, v);
		}
		return from;
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
				Locatable dot = (Locatable) args.remove(i);
				Object right = args.remove(i);
				ApplyExpr ae = new ApplyExpr(dot.location(), dot, left, right);
				args.add(i, ae);
			}
		}
		
		// Step 2.  The trickiest thing is to handle all the straightforward function calls, so do that next
		for (int i=0,j=0;i<=args.size();i++) {
			if (i == args.size() || args.get(i) instanceof UnresolvedOperator) {
				if (i>j+1) { // collapse a fn defn to the left
					List<Object> inargs = new ArrayList<Object>();
					for (int k=j+1;k<i;k++) {
						inargs.add(deparen(args.remove(j+1)));
					}
					Locatable op = (Locatable) args.remove(j);
					ApplyExpr ae = new ApplyExpr(op.location(), deparen(op), inargs);
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
			if (i == args.size() || args.get(i) instanceof UnresolvedOperator) {
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
						op = ((UnresolvedOperator)args.get(i)).op;
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
						args.add(i-2, new ApplyExpr(null, o1, o2));
						i--;
					}
					else {
						if (stack.size() > 0)
							stack.remove(0);
						Object o1 = deparen(args.remove(i-3));
						Locatable o2 = (Locatable) deparen(args.remove(i-3));
						Object o3 = deparen(args.remove(i-3));
						if (isCurryVar(o1) && isCurryVar(o3))
							return new ParenExpr(o2);
						else if (isCurryVar(o1))
							args.add(i-3, new ApplyExpr(o2.location(), curry1st(o2), o3));
						else if (isCurryVar(o3))
							args.add(i-3, new ApplyExpr(o2.location(), o2, o1));
						else
							args.add(i-3, new ApplyExpr(o2.location(), o2, o1, o3));
						i-=2;
					}
				} while (action == REDUCE);
			}
		}
		return args.remove(0);
	}

	private boolean isDot(Object tok) {
		return tok instanceof UnresolvedOperator && ((UnresolvedOperator)tok).op.equals(".");
	}

	private Object deparen(Object pe) {
		if (pe instanceof ErrorResult)
			return pe;
		else if (pe instanceof UnresolvedOperator)
			return rehash((UnresolvedOperator) pe);
		else if (pe instanceof ParenExpr)
			return deparen(((ParenExpr)pe).nested);
		else if (pe instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) pe;
			List<Object> args = new ArrayList<Object>();
			for (Object o : ae.args)
				args.add(deparen(o));
			return new ApplyExpr(ae.location, deparen(ae.fn), args);
		} else if (pe instanceof CastExpr) {
			CastExpr ce = (CastExpr) pe;
			return new CastExpr(ce.location, ce.castTo, deparen(ce.expr));
		} else if (pe instanceof NumericLiteral || pe instanceof UnresolvedVar || pe instanceof UnresolvedOperator || pe instanceof StringLiteral)
			return pe;
		else
			throw new UtilException("Expr not handled: " + pe.getClass());
	}

	private Object rehash(UnresolvedOperator ie) {
		if (ie.op.equals(":"))
			return new UnresolvedVar(ie.location, "Cons");
		return ie;
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

	private boolean isCurryVar(Object o) {
		return o instanceof UnresolvedVar && ((UnresolvedVar)o).var.equals("_");
	}

	private Object curry1st(Object o) {
		return new UnresolvedOperator(null, ((UnresolvedOperator)o).op+"_");
	}

	private Object parseParenthetical(Tokenizable line, String endsWith) {
		List<Object> objs = new ArrayList<Object>();
		while (true) {
			// TODO: I'm not sure about this way of doing it
			// It seems to me that [,,] might end up not an error but []
			InputPosition startsAt = line.realinfo();
			Object expr = tryParsing(line);
			if (expr instanceof ErrorResult)
				return expr;
			if (expr != null)
				objs.add(expr);
			ExprToken crb = ExprToken.from(line);
			if (crb == null)
				return null;
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
							return new ApplyExpr(null, ItemExpr.from(new ExprToken(startsAt, ExprToken.SYMBOL, "()")), objs);
						}
					}
					else if (endsWith.equals("]")) {
						Object base = ItemExpr.from(new ExprToken(startsAt, ExprToken.IDENTIFIER, "Nil"));
						for (int i=objs.size()-1;i>=0;i--) {
							Locatable o = (Locatable) objs.get(i);
							base = new ApplyExpr(o.location(), ItemExpr.from(new ExprToken(o.location(), ExprToken.IDENTIFIER, "Cons")), o, base);
						}
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
