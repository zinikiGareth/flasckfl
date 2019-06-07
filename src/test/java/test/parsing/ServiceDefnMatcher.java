package test.parsing;

import org.flasck.flas.parsedForm.ServiceDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ServiceDefnMatcher extends TypeSafeMatcher<ServiceDefinition> {
	private final String name;

	public ServiceDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Service(");
		arg0.appendValue(name);
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(ServiceDefinition arg0) {
		if (!arg0.serviceName.uniqueName().equals(name))
			return false;
		return true;
	}

	public static ServiceDefnMatcher called(String name) {
		return new ServiceDefnMatcher(name);
	}
}
