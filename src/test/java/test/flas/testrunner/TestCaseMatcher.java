package test.flas.testrunner;

import org.flasck.flas.testrunner.SingleTestCase;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;


public class TestCaseMatcher extends TypeSafeMatcher<SingleTestCase> {
	private final String caseName;

	public TestCaseMatcher(String caseName) {
		this.caseName = caseName;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("test is called " + caseName);
	}

	@Override
	protected boolean matchesSafely(SingleTestCase item) {
		return item.getDescription().equals(caseName);
	}

}
