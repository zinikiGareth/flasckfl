package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.fl.TestHelper;

public class _ut12 {
	public static void dotest(TestHelper runner, FLEvalContext cxt) throws Exception {
		runner.assertSameValue(cxt, 42.0, 42.0);
	}
}
