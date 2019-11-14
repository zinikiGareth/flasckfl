package flas.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CtorPatternMatcher extends PatternMatcher {
	public static class FieldMatcher extends TypeSafeMatcher<ConstructorMatch.Field> {
		private final String fld;
		private final Matcher<Pattern> m;

		public FieldMatcher(String fld, Matcher<Pattern> m) {
			this.fld = fld;
			this.m = m;
		}

		@Override
		public void describeTo(Description arg0) {
			arg0.appendText("Field[");
			arg0.appendValue(fld);
			arg0.appendText(":");
			arg0.appendValue(m);
			arg0.appendText("]");
		}

		@Override
		protected boolean matchesSafely(Field field) {
			if (!fld.equals(field.field))
				return false;
			return m.matches(field.patt);
		}

	}

	private final String ctor;
	private final List<FieldMatcher> patterns = new ArrayList<>();

	public CtorPatternMatcher(String ctor) {
		this.ctor = ctor;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Ctor<");
		arg0.appendValue(ctor);
		arg0.appendValue(patterns);
		arg0.appendText(">");
	}

	@Override
	protected boolean matchesSafely(Pattern arg0) {
		if (arg0 instanceof ConstructorMatch) {
			final ConstructorMatch cm = (ConstructorMatch)arg0;
			if (!ctor.equals(cm.ctor))
				return false;
			if (patterns.size() != cm.args.size())
				return false;
			for (int i=0;i<patterns.size();i++) {
				if (!patterns.get(i).matches(cm.args.get(i)))
					return false;
			}
			return true;
		}
		return false;
	}

	public CtorPatternMatcher field(String fld, Matcher<Pattern> m) {
		patterns.add(new FieldMatcher(fld, m));
		return this;
	}
}
