package flas.matchers;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.MakeSend;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MakeSendMatcher extends TypeSafeMatcher<MakeSend> {
	private final FunctionName sendTo;
	private final Matcher<Expr> sendOn;
	private final int nargs;

	public MakeSendMatcher(FunctionName sendTo, Matcher<Expr> sendOn, int nargs) {
		this.sendTo = sendTo;
		this.sendOn = sendOn;
		this.nargs = nargs;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("MakeSend[");
		arg0.appendValue(sendTo);
		arg0.appendValue(sendOn);
		arg0.appendText(":");
		arg0.appendValue(nargs);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(MakeSend arg0) {
		if (!sendTo.uniqueName().equals(arg0.sendMeth.uniqueName()))
			return false;
		if (!sendOn.matches(arg0.obj))
			return false;
		if (nargs != arg0.nargs)
			return false;
		return true;
	}

	public static MakeSendMatcher sending(FunctionName sendTo, Matcher<Expr> sendOn, int nargs) {
		return new MakeSendMatcher(sendTo, sendOn, nargs);
	}

}
