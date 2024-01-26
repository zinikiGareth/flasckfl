package org.flasck.flas.testsupport.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class UnionDefnMatcher extends TypeSafeMatcher<UnionTypeDefn> {
	private final String name;
	private final List<String> polys = new ArrayList<>();
	private int typeloc = -1;

	public UnionDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("UnionDefn(");
		arg0.appendValue(name);
		if (!polys.isEmpty()) {
			arg0.appendValue(polys);
		}
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(UnionTypeDefn arg0) {
		if (!arg0.name().uniqueName().equals(name))
			return false;
		if (arg0.polys().size() != polys.size())
			return false;
		if (typeloc != -1 && typeloc != arg0.location().off)
			return false;
		return true;
	}
	
	public UnionDefnMatcher poly(String poly) {
		polys.add(poly);
		return this;
	}
	
	public UnionDefnMatcher locs(int type) {
		typeloc = type;
		return this;
	}
	
	public static UnionDefnMatcher match(String name) {
		return new UnionDefnMatcher(name);
	}
}
