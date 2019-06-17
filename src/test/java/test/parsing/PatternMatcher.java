package test.parsing;

import org.flasck.flas.commonBase.Pattern;
import org.hamcrest.TypeSafeMatcher;

public abstract class PatternMatcher extends TypeSafeMatcher<Pattern> {

	public static VarPatternMatcher var(String var) {
		return new VarPatternMatcher(var);
	}

	public static TypedPatternMatcher typed(String type, String var) {
		return new TypedPatternMatcher(type, var);
	}

	public static CtorPatternMatcher ctor(String ctor) {
		return new CtorPatternMatcher(ctor);
	}
}
