package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FunctionDefinitionMatcher extends TypeSafeMatcher<FunctionDefinition> {
	private final String name;
	private Integer nargs;
	private Integer nintros;

	public FunctionDefinitionMatcher(String name) {
		this.name = name;
	}
	
	public FunctionDefinitionMatcher args(int nargs) {
		this.nargs = nargs;
		return this;
	}

	public FunctionDefinitionMatcher intros(int nintros) {
		this.nintros = nintros;
		return this;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("Function[");
		desc.appendValue(name);
		if (nargs != null) {
			desc.appendText("(");
			desc.appendValue(nargs);
			desc.appendText(")");
		}
		if (nintros != null) {
			desc.appendText("{");
			desc.appendValue(nintros);
			desc.appendText("}");
		}
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(FunctionDefinition fd) {
		if (!fd.name().uniqueName().equals(name))
			return false;
		if (nargs != null && !(nargs.equals(fd.argCount())))
			return false;
		if (nintros != null && !(nintros.equals(fd.intros().size())))
			return false;
		return true;
	}

	public static FunctionDefinitionMatcher named(String name) {
		return new FunctionDefinitionMatcher(name);
	}

}
