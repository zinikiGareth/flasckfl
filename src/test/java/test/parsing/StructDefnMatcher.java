package test.parsing;

import org.flasck.flas.parsedForm.StructDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class StructDefnMatcher extends TypeSafeMatcher<StructDefn> {
	private final String name;

	public StructDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("StructDefn[");
		arg0.appendValue(name);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(StructDefn arg0) {
		return arg0.structName.uniqueName().equals(name);
	}

	public static StructDefnMatcher match(String name) {
		return new StructDefnMatcher(name);
	}

}
