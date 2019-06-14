package test.parsing;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FunctionDefinitionMatcher extends TypeSafeMatcher<FunctionDefinition> {
	private final String name;
	private Integer nargs;

	public FunctionDefinitionMatcher(String name) {
		this.name = name;
	}
	
	public FunctionDefinitionMatcher args(int nargs) {
		this.nargs = nargs;
		return this;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("Function[");
		desc.appendValue(name);
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(FunctionDefinition fd) {
		if (!fd.getName().uniqueName().equals(name))
			return false;
		if (nargs != null && !(nargs.equals(fd.getArgCount())))
			return false;
		return true;
	}

	public static FunctionDefinitionMatcher named(String name) {
		return new FunctionDefinitionMatcher(name);
	}

}
