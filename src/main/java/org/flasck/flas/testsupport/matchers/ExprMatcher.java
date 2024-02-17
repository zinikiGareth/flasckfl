package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.ParenExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.DotOperator;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.Punctuator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public abstract class ExprMatcher extends TypeSafeMatcher<Expr> {
	private InputPosition pos;

	public static ExprMatcher unresolved(final String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("var '" + name + "'");
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof UnresolvedVar))
					return false;
				if (!((UnresolvedVar)expr).var.equals(name))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher typeref(String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("type '" + name + "'");
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof TypeReference))
					return false;
				if (!((TypeReference)expr).name().equals(name))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher operator(final String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("operator '" + name + "'");
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof UnresolvedOperator))
					return false;
				if (!((UnresolvedOperator)expr).op.equals(name))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}
	
	public static ExprMatcher dot() {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("dot");
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof DotOperator))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher punc(final String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is punc '" + name + "'");
				if (super.pos != null) {
					desc.appendText("pos");
					desc.appendValue(super.pos);
				}
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof Punctuator))
					return false;
				if (!((Punctuator)expr).punc.equals(name))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher number(final Integer k) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is numeric literal '" + k + "'");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof NumericLiteral))
					return false;
				final NumericLiteral number = (NumericLiteral)expr;
				if (number.text == null && number.val != k)
					return false;
				else if (number.text != null && !number.text.equals(Integer.toString(k)))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher typeof(String string) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("typeof '" + string + "'");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof TypeExpr))
					return false;
				expr = ((TypeExpr)expr).type;
				if (!(expr instanceof UnresolvedVar))
					return false;
				if (!((UnresolvedVar)expr).var.equals(string))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public static ExprMatcher string(final String s) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is string literal '" + s + "'");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof StringLiteral))
					return false;
				if (!((StringLiteral)expr).text.equals(s))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	@SafeVarargs
	public static ExprMatcher apply(Matcher<Expr> fn, final Matcher<Expr>... args) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("{apply (");
				fn.describeTo(desc);
				desc.appendText(") to [");
				String sep = "";
				for (Matcher<Expr> m : args) {
					desc.appendText(sep);
					m.describeTo(desc);
					sep = ", ";
				}
				desc.appendText("]}");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof ApplyExpr))
					return false;
				ApplyExpr ae = (ApplyExpr) expr;
				if (!fn.matches(ae.fn) || ae.args.size() != args.length)
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				for (int i=0;i<args.length;i++) {
					if (!args[i].matches(ae.args.get(i)))
						return false;
				}
				return true;
			}
		};
	}


	public static ExprMatcher paren(Matcher<Expr> wrapped) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("paren (");
				wrapped.describeTo(desc);
				desc.appendText(") ");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof ParenExpr))
					return false;
				ParenExpr ae = (ParenExpr) expr;
				return wrapped.matches(ae.expr);
			}
		};
	}

	public static ExprMatcher member(ExprMatcher from, ExprMatcher fld) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("(. ");
				from.describeTo(desc);
				desc.appendText(" ");
				fld.describeTo(desc);
				desc.appendText(")");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				if (!(expr instanceof MemberExpr))
					return false;
				MemberExpr ae = (MemberExpr) expr;
				if (!from.matches(ae.from) || !fld.matches(ae.fld))
					return false;
				if (super.pos != null) {
					if (expr.location() == null)
						return false;
					if (super.pos.compareTo(expr.location()) != 0)
						return false;
				}
				return true;
			}
		};
	}

	public ExprMatcher location(String file, int line, int off, int end) {
		pos = new InputPosition(file, line, off, null, "");
		pos.endAt(end);
		return this;
	}
}
