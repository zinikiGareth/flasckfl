package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.fl.TestHelper;

public class _ut18 {
	public static void dotest(TestHelper runner, FLEvalContext cx) throws Exception {
		runner.assertSameValue(cx, 42.0, 84.0);
	}
}
