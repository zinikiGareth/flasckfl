package flas.matchers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class AssignMessageMatcher extends TypeSafeMatcher<AssignMessage> {
	private InputPosition pos;
	
	public static class Builder {
		private String[] vars;

		public Builder(String[] vars) {
			this.vars = vars;
		}

		public AssignMessageMatcher with(ExprMatcher matcher) {
			return new AssignMessageMatcher() {
				@Override
				public void describeTo(Description desc) {
					for (int i=0;i<vars.length;i++)
						desc.appendText(vars[i] + " ");
					desc.appendText("<- ");
					matcher.describeTo(desc);
							
					if (super.pos != null) {
						desc.appendText("pos");
						desc.appendValue(super.pos);
					}
				}

				@Override
				protected boolean matchesSafely(AssignMessage msg) {
					if (!matchFrom(msg.slot, vars.length-1))
						return false;
//					if (msg.slot.size() != vars.length)
//						return false;
//					for (int i=0;i<msg.slot.size();i++)
//						if (!vars[i].equals(msg.slot.get(i).var))
//							return false;
					if (!matcher.matches(msg.expr))
						return false;
					if (super.pos != null) {
						if (msg.location() == null)
							return false;
						if (super.pos.compareTo(msg.location()) != 0)
							return false;
					}
					return true;
				}

				private boolean matchFrom(Expr slot, int vi) {
					if (vi == 0) {
						if (!(slot instanceof UnresolvedVar))
							return false;
						UnresolvedVar uv = (UnresolvedVar) slot;
						return vars[vi].equals(uv.var);
					} else {
						if (!(slot instanceof MemberExpr))
							return false;
						MemberExpr me = (MemberExpr) slot;
						if (!matchFrom(me.from, vi-1))
							return false;
						
						return vars[vi].equals(((UnresolvedVar)me.fld).var);
					}
				}
			};
		}
	}

	public static AssignMessageMatcher.Builder to(String... vars) {
		return new Builder(vars);
	}

	public AssignMessageMatcher location(String file, int line, int off, int end) {
		pos = new InputPosition(file, line, off, null, "");
		pos.endAt(end);
		return this;
	}
}
