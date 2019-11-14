package flas.matchers;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.tc3.ConsolidateTypes;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ConsolidatedTypeMatcher extends TypeSafeMatcher<ConsolidateTypes>{
	private final List<Matcher<Type>> types;

	public ConsolidatedTypeMatcher(List<Matcher<Type>> list) {
		this.types = list;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ConsolidatedTypes");
		arg0.appendValue(types);
	}

	@Override
	protected boolean matchesSafely(ConsolidateTypes arg0) {
		if (arg0.types.size() != types.size())
			return false;
		for (int i=0;i<types.size();i++)
			if (!types.get(i).matches(arg0.types.get(i)))
				return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public static ConsolidatedTypeMatcher with(Matcher<Type>... tys) {
		return new ConsolidatedTypeMatcher(Arrays.asList(tys));
	}

}
