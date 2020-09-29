package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.fl.CallEval;
import org.flasck.jvm.fl.FLClosure;
import org.flasck.jvm.fl.TestHelper;

public class _ut25 {
	public static void dotest(TestHelper runner, FLEvalContext cx) throws Exception {
		Object v1 = FLClosure.simple(new CallEval(EvaluateTo42.class));
		runner.assertSameValue(cx, v1, 42.0);
	}
}
