package test.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public abstract class ExprMatcher extends TypeSafeMatcher<Expr> {
	private InputPosition pos;

	public static ExprMatcher unresolved(final String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is unresolved var '" + name + "'");
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

	public static ExprMatcher operator(final String name) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is unresolved operator '" + name + "'");
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

	public static ExprMatcher number(final Integer k) {
		return new ExprMatcher() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is numeric literal '" + k + "'");
			}

			@Override
			protected boolean matchesSafely(Expr expr) {
				return expr instanceof NumericLiteral && ((NumericLiteral)expr).text.equals(Integer.toString(k));
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
				return expr instanceof StringLiteral && ((StringLiteral)expr).text.equals(s);
			}
		};
	}

	@SafeVarargs
	public static Matcher<Object> apply(Matcher<Expr> fn, final Matcher<Object>... args) {
		return new TypeSafeMatcher<Object>() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is apply of ");
				fn.describeTo(desc);
				desc.appendText(" to");
				for (Matcher<Object> m : args) {
					desc.appendText(" ");
					m.describeTo(desc);
				}
			}

			@Override
			protected boolean matchesSafely(Object expr) {
				if (!(expr instanceof ApplyExpr))
					return false;
				ApplyExpr ae = (ApplyExpr) expr;
				if (!fn.matches(ae.fn) || ae.args.size() != args.length)
					return false;
				for (int i=0;i<args.length;i++) {
					if (!args[i].matches(ae.args.get(i)))
						return false;
				}
				return true;
			}
		};
	}

	public ExprMatcher location(String file, int line, int off) {
		pos = new InputPosition(file, line, off, "");
		return this;
	}
}
