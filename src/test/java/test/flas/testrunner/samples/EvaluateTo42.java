package test.flas.testrunner.samples;

import org.flasck.jvm.builtin.FLNumber;
import org.ziniki.ziwsh.json.FLEvalContext;

public class EvaluateTo42 {

	public static Object eval(FLEvalContext env, Object[] args) {
		return new FLNumber(42, null);
	}

}
