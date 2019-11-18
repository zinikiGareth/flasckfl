package flas.matchers;

import org.flasck.flas.parsedForm.PolyType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PolyTypeMatcher extends TypeSafeMatcher<PolyType> {
	private final String name;

	public PolyTypeMatcher(String name) {
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
		return ty.shortName().equals(name);
	}

	public static PolyTypeMatcher called(String name) {
		return new PolyTypeMatcher(name);
	}

}
