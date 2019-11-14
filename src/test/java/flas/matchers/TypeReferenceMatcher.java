package flas.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.TypeReference;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TypeReferenceMatcher extends TypeSafeMatcher<TypeReference> {
	private final String name;
	private final List<String> polys = new ArrayList<>();

	public TypeReferenceMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("TypeReference(");
		arg0.appendValue(name);
		if (!polys.isEmpty()) {
			arg0.appendValue(polys);
		}
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(TypeReference arg0) {
		if (!arg0.name().equals(name))
			return false;
		if (arg0.polys().size() != polys.size())
			return false;
		return true;
	}
	
	public TypeReferenceMatcher poly(String poly) {
		polys.add(poly);
		return this;
	}
	
	public static TypeReferenceMatcher type(String name) {
		return new TypeReferenceMatcher(name);
	}
}
