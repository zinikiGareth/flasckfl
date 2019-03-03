package test.parsing;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FunctionCaseMatcher extends TypeSafeMatcher<FunctionCaseDefn> {

	private final PackageName pkg;
	private final String name;

	public FunctionCaseMatcher(PackageName pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("{function ");
		arg0.appendValue(pkg);
		arg0.appendValue(name);
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
		return true;
	}

	public static FunctionCaseMatcher called(PackageName pkg, String name) {
		return new FunctionCaseMatcher(pkg, name);
	}

}
