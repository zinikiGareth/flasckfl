package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectDefnMatcher extends TypeSafeMatcher<ObjectDefn> {
	private final String name;
	private final List<String> polys = new ArrayList<>();
	private int kwloc = -1;
	private int typeloc = -1;

	public ObjectDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ObjectDefn(");
		arg0.appendValue(name);
		arg0.appendText(")");
		if (!polys.isEmpty()) {
			arg0.appendValue(polys);
		}
		if (kwloc != -1) {
			arg0.appendText("@{");
			arg0.appendValue(kwloc);
			arg0.appendText(",");
			arg0.appendValue(typeloc);
			arg0.appendText("}");
		}
	}

	@Override
	protected boolean matchesSafely(ObjectDefn arg0) {
		if (!arg0.name().uniqueName().equals(name))
			return false;
		if (arg0.polys().size() != polys.size())
			return false;
		if (kwloc != -1 && kwloc != arg0.kw.off)
			return false;
		if (typeloc != -1 && typeloc != arg0.location().off)
			return false;
		return true;
	}
	
	public ObjectDefnMatcher poly(String poly) {
		polys.add(poly);
		return this;
	}
	
	public ObjectDefnMatcher locs(int kw, int type) {
		kwloc = kw;
		typeloc = type;
		return this;
	}
	
	public static ObjectDefnMatcher match(String name) {
		return new ObjectDefnMatcher(name);
	}
}
