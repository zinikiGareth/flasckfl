package test.methods;

import org.flasck.flas.parsedForm.MakeSend;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class MakeSendMatcher extends TypeSafeMatcher<MakeSend> {
	private final int nargs;

	public MakeSendMatcher(int nargs) {
		this.nargs = nargs;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("MakeSend[");
		arg0.appendValue(nargs);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(MakeSend arg0) {
		return arg0.nargs == nargs;
	}

	public static MakeSendMatcher curry(int nargs) {
		return new MakeSendMatcher(nargs);
	}

}
