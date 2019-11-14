package flas.matchers;

import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class UnitTestCaseMatcher extends TypeSafeMatcher<UnitTestCase> {
	private final int number;
	private String description;

	public UnitTestCaseMatcher(int number) {
		this.number = number;
	}

	public UnitTestCaseMatcher description(String string) {
		this.description = string;
		return this;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("UT[");
		desc.appendValue(number);
		if (description != null) {
			desc.appendText(":");
			desc.appendValue(description);
		}
	}

	@Override
	protected boolean matchesSafely(UnitTestCase utc) {
		if (!description.equals(utc.description))
			return false;
		return true;
	}

	public static UnitTestCaseMatcher number(int number) {
		return new UnitTestCaseMatcher(number);
	}

}
