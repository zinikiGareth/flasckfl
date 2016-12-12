package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.reflection.Reflection;

public class AssertTestStep implements TestStep {
//	private final InputPosition evalPos;
	private final Object eval;
//	private final InputPosition valuePos;
	private final Object value;
	private final int exprId;

	public AssertTestStep(int exprId, InputPosition evalPos, Object eval, InputPosition valuePos, Object value) {
		this.exprId = exprId;
//		this.evalPos = evalPos;
		this.eval = eval;
//		this.valuePos = valuePos;
		this.value = value;
	}
	
	@Override
	public void run(BCEClassLoader loader, String scriptPkg) throws AssertFailed, ClassNotFoundException {
		List<Class<?>> toRun = new ArrayList<>();
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$expr" + exprId, false, loader));
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$value" + exprId, false, loader));

		Class<?> fleval = Class.forName("org.flasck.jvm.FLEval", false, loader);
		Map<String, Object> evals = new TreeMap<String, Object>();
		for (Class<?> clz : toRun) {
			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
			Object o = Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
			o = Reflection.callStatic(fleval, "full", o);
			evals.put(key, o);
		}
		
		Object expected = evals.get("value" + exprId);
		Object actual = evals.get("expr" + exprId);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

	@Override
	public String toString() {
		return "expr " + eval + " should have value " + value;
	}
}
