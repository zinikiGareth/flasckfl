package test.flas.testrunner.samples;

import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLNumber;

public class _ut18 {
	public static void dotest(JVMRunner runner, FLEvalContext cx) throws Exception {
		runner.assertSameValue(new FLNumber(42, null), new FLNumber(84, null));
	}
}
