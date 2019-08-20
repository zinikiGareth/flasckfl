package test.flas.testrunner.samples;

import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.jvm.builtin.FLNumber;
import org.flasck.jvm.fl.FLClosure;

public class _ut25 {
	public static void dotest(JVMRunner runner) throws Exception {
		Object v1 = FLClosure.simple(EvaluateTo42.class);
		runner.assertSameValue(v1, new FLNumber(42, null));
	}
}