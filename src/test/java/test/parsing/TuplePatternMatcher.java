package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.TuplePattern;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TuplePatternMatcher extends TypeSafeMatcher<Pattern> {
	public static class MemberMatcher extends TypeSafeMatcher<Pattern> {
		private final Matcher<Pattern> m;

		public MemberMatcher(Matcher<Pattern> m) {
			this.m = m;
		}

		@Override
		public void describeTo(Description arg0) {
			arg0.appendText("Member[");
			arg0.appendValue(m);
			arg0.appendText("]");
		}

		@Override
		protected boolean matchesSafely(Pattern patt) {
			return m.matches(patt);
		}

	}

	private final List<MemberMatcher> members = new ArrayList<>();

	public TuplePatternMatcher() {
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Tuple<");
		arg0.appendValue(members);
		arg0.appendText(">");
	}

	@Override
	protected boolean matchesSafely(Pattern arg0) {
		if (arg0 instanceof TuplePattern) {
			final TuplePattern cm = (TuplePattern)arg0;
			if (members.size() != cm.args.size())
				return false;
			for (int i=0;i<members.size();i++) {
				if (!members.get(i).matches(cm.args.get(i)))
					return false;
			}
			return true;
		}
		return false;
	}

	public static TuplePatternMatcher tuple() {
		return new TuplePatternMatcher();
	}

	public TuplePatternMatcher member(Matcher<Pattern> m) {
		members.add(new MemberMatcher(m));
		return this;
	}
}
