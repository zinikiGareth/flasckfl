package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;
import org.flasck.jvm.builtin.FLNumber;

public class EvaluateTo42 {
	public static final int nfargs = 0;
	
	public static Object eval(FLEvalContext env, Object[] args) {
		return new FLNumber(42, null);
	}

}
