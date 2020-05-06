package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLNumber;
import org.flasck.jvm.fl.TestHelper;

public class _ut18 {
	public static void dotest(TestHelper runner, FLEvalContext cx) throws Exception {
		runner.assertSameValue(new FLNumber(42, null), new FLNumber(84, null));
	}
}
