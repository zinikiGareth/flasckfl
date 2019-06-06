package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FunctionCaseMatcher extends TypeSafeMatcher<FunctionCaseDefn> {

	private final NameOfThing pkg;
	private final String name;
	private final List<PatternMatcher> patterns = new ArrayList<>();

	public FunctionCaseMatcher(NameOfThing pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}

	public FunctionCaseMatcher pattern(PatternMatcher pattern) {
		patterns.add(pattern);
		return this;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("{function ");
		arg0.appendValue(pkg);
		arg0.appendValue(name);
		arg0.appendValue(patterns);
		arg0.appendText("}");
	}

	@Override
	protected boolean matchesSafely(FunctionCaseDefn arg0) {
		final FunctionName fn = arg0.intro.name();
		if (!fn.name.equals(name))
			return false;
		if (fn.inContext != null && pkg == null)
			return false;
		if (fn.inContext != null && !fn.inContext.equals(pkg))
			return false;
		if (patterns.size() != arg0.intro.args.size())
			return false;
		for (PatternMatcher p : patterns) {
			if (!p.matches(arg0.intro.args.get(0)))
				return false;
		}
		return true;
	}

	public static FunctionCaseMatcher called(NameOfThing pkg, String name) {
		return new FunctionCaseMatcher(pkg, name);
	}

}
