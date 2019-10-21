package test.tc3;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.tc3.Application;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ApplicationMatcher extends TypeSafeMatcher<Application> {
	private Matcher<UnifiableType> ut;
	private List<Matcher<? extends Type>> args;

	private ApplicationMatcher(Matcher<UnifiableType> ut, Matcher<? extends Type>[] args) {
		this.ut = ut;
		this.args = Arrays.asList(args);
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ApplicationOf[");
		arg0.appendValue(ut);
		arg0.appendText("]To");
		arg0.appendValue(args);
	}

	@Override
	protected boolean matchesSafely(Application app) {
		if (!ut.matches(app.op))
			return false;
		if (args.size() != app.tys.size())
			return false;
		for (int i=0;i<args.size();i++) {
			if (!args.get(i).matches(app.tys.get(i)))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static ApplicationMatcher of(Matcher<UnifiableType> ut, Matcher<? extends Type>... args) {
		return new ApplicationMatcher(ut, args);
	}
}
