package test.tc3;

import org.flasck.flas.parsedForm.PolyType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PolyVarMatcher extends TypeSafeMatcher<PolyType> {
	private final String name;

	public PolyVarMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Poly[");
		arg0.appendValue(name);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(PolyType ty) {
		return ty.name().equals(name);
	}

	public static PolyVarMatcher called(String name) {
		return new PolyVarMatcher(name);
	}

}
