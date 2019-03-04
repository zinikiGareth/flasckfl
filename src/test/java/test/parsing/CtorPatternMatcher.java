package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.VarPattern;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CtorPatternMatcher extends TypeSafeMatcher<Pattern> {
	private final String ctor;
	private final List<Pattern> patterns;

	public CtorPatternMatcher(String ctor, ArrayList<Pattern> children) {
		this.ctor = ctor;
		this.patterns = children;
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
			if (!ctor.equals(((ConstructorMatch)arg0).ctor))
				return false;
			return true;
		}
		return false;
	}

	public static CtorPatternMatcher ctor(String ctor) {
		return new CtorPatternMatcher(ctor, new ArrayList<>());
	}

	public CtorPatternMatcher arg(Matcher<Pattern> m) {
		return this;
	}
}
