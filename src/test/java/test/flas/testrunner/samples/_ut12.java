package test.flas.testrunner.samples;

import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLNumber;

public class _ut12 {
	public static void dotest(JVMRunner runner, FLEvalContext cxt) throws Exception {
		runner.assertSameValue(new FLNumber(42, null), new FLNumber(42, null));
	}
}
