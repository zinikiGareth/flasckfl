package org.flasck.flas.testsupport.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ObjectCtor;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectCtorMatcher extends TypeSafeMatcher<ObjectCtor> {
	private final String name;
	private final List<PatternMatcher> args = new ArrayList<>();

	private ObjectCtorMatcher(String name) {
		this.name = "_ctor_" + name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Ctor ");
		arg0.appendValue(name);
		arg0.appendText("[");
		for (PatternMatcher p : args)
			p.describeTo(arg0);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(ObjectCtor ctor) {
		if (!ctor.name().name.equals(name))
			return false;
		if (ctor.args().size() != args.size())
			return false;
		return true;
	}

	public static ObjectCtorMatcher called(String name) {
		return new ObjectCtorMatcher(name);
	}

	public ObjectCtorMatcher arg(PatternMatcher p) {
		this.args.add(p);
		return this;
	}

}
