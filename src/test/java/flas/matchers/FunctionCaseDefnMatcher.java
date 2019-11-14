package flas.matchers;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FunctionCaseDefnMatcher extends TypeSafeMatcher<FunctionCaseDefn> {

	public enum FCDType {
		GUARDED, DEFAULT
	}

	private FCDType fcdType;

	public FunctionCaseDefnMatcher(FCDType type) {
		fcdType = type;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("FCD[");
		desc.appendValue(fcdType);
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(FunctionCaseDefn fcd) {
		return (fcdType == FCDType.GUARDED && fcd.guard != null) || (fcdType == FCDType.DEFAULT && fcd.guard == null);
	}

	public static FunctionCaseDefnMatcher isGuarded() {
		return new FunctionCaseDefnMatcher(FCDType.GUARDED);
	}

	public static FunctionCaseDefnMatcher isDefault() {
		return new FunctionCaseDefnMatcher(FCDType.DEFAULT);
	}
}
