package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.StructDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class StructDefnMatcher extends TypeSafeMatcher<StructDefn> {
	private final String name;
	private final List<String> polys = new ArrayList<>();

	public StructDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("StructDefn(");
		arg0.appendValue(name);
		arg0.appendText(")");
		if (!polys.isEmpty()) {
			arg0.appendValue(polys);
		}
	}

	@Override
	protected boolean matchesSafely(StructDefn arg0) {
		if (!arg0.structName.uniqueName().equals(name))
			return false;
		if (arg0.polys().size() != polys.size())
			return false;
		return true;
	}
	
	public StructDefnMatcher poly(String poly) {
		polys.add(poly);
		return this;
	}

	public static StructDefnMatcher match(String name) {
		return new StructDefnMatcher(name);
	}

}
