package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class UnionDefnMatcher extends TypeSafeMatcher<UnionTypeDefn> {
	private final String name;
	private final List<String> polys = new ArrayList<>();
	private int kwloc = -1;
	private int typeloc = -1;
	private FieldsDefn.FieldsType objty = FieldsDefn.FieldsType.STRUCT;

	public UnionDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("StructDefn(");
		arg0.appendValue(objty);
		arg0.appendText(":");
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
	protected boolean matchesSafely(UnionTypeDefn arg0) {
		if (!arg0.myName().uniqueName().equals(name))
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
	
	public UnionDefnMatcher locs(int kw, int type) {
		kwloc = kw;
		typeloc = type;
		return this;
	}
	
	public UnionDefnMatcher as(FieldsDefn.FieldsType ty) {
		objty = ty;
		return this;
	}

	public static UnionDefnMatcher match(String name) {
		return new UnionDefnMatcher(name);
	}
}
