package test.parsing;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectMethodMatcher extends TypeSafeMatcher<ObjectMethod> {

	private final NameOfThing scope;
	private final String name;
	private Integer hasArgs;

	public ObjectMethodMatcher(NameOfThing scope, String name) {
		this.scope = scope;
		this.name = name;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("ObjectMethod{");
		if (scope != null) {
			desc.appendValue(scope.uniqueName());
			desc.appendText(".");
		}
		desc.appendValue(name);
		if (hasArgs != null) {
			desc.appendText("/");
			desc.appendValue(hasArgs);
		}
		desc.appendText("}");
	}

	@Override
	protected boolean matchesSafely(ObjectMethod meth) {
		FunctionName fn = meth.name();
		if ((fn.inContext == null) != (scope == null))
			return false;
		if (scope != null && !scope.uniqueName().equals(fn.inContext.uniqueName()))
			return false;
		if (!fn.name.equals(name))
			return false;
		if (hasArgs != null && hasArgs != meth.args().size())
			return false;
		return true;
	}

	public static ObjectMethodMatcher called(NameOfThing scope, String name) {
		return new ObjectMethodMatcher(scope, name);
	}

	public ObjectMethodMatcher withArgs(int cnt) {
		hasArgs = cnt;
		return this;
	}
}
