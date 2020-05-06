package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLNumber;
import org.flasck.jvm.fl.TestHelper;

public class _ut12 {
	public static void dotest(TestHelper runner, FLEvalContext cxt) throws Exception {
		runner.assertSameValue(new FLNumber(42, null), new FLNumber(42, null));
	}
}
