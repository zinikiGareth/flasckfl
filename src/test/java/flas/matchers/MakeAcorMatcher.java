package flas.matchers;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MakeAcorMatcher extends TypeSafeMatcher<MakeAcor> {
	private final FunctionName acor;
	private final Matcher<Expr> on;
	private final int nargs;

	public MakeAcorMatcher(FunctionName acor, Matcher<Expr> on, int nargs) {
		this.acor = acor;
		this.on = on;
		this.nargs = nargs;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("MakeSend[");
		arg0.appendValue(acor);
		arg0.appendValue(on);
		arg0.appendText(":");
		arg0.appendValue(nargs);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(MakeAcor arg0) {
		if (!acor.uniqueName().equals(arg0.acorMeth.uniqueName()))
			return false;
		if (!on.matches(arg0.obj))
			return false;
		if (nargs != arg0.nargs)
			return false;
		return true;
	}

	public static MakeAcorMatcher acor(FunctionName acor, Matcher<Expr> on, int nargs) {
		return new MakeAcorMatcher(acor, on, nargs);
	}

}
