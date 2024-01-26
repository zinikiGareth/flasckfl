package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.parsedForm.AgentDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class AgentDefnMatcher extends TypeSafeMatcher<AgentDefinition> {
	private final String name;
	private int kwloc = -1;
	private int typeloc = -1;

	public AgentDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ObjectDefn(");
		arg0.appendValue(name);
		arg0.appendText(")");
		if (kwloc != -1) {
			arg0.appendText("@{");
			arg0.appendValue(kwloc);
			arg0.appendText(",");
			arg0.appendValue(typeloc);
			arg0.appendText("}");
		}
	}

	@Override
	protected boolean matchesSafely(AgentDefinition arg0) {
		if (!arg0.name().uniqueName().equals(name))
			return false;
		if (kwloc != -1 && kwloc != arg0.kw.off)
			return false;
		if (typeloc != -1 && typeloc != arg0.location().off)
			return false;
		return true;
	}
	
	public AgentDefnMatcher locs(int kw, int type) {
		kwloc = kw;
		typeloc = type;
		return this;
	}
	
	public static AgentDefnMatcher match(String name) {
		return new AgentDefnMatcher(name);
	}
}
