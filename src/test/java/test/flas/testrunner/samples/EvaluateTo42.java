package test.flas.testrunner.samples;

import org.flasck.jvm.FLEvalContext;

public class EvaluateTo42 {
	public static final int nfargs = 0;
	
	public static Object eval(FLEvalContext env, Object[] args) {
		return 42.0;
	}

}
