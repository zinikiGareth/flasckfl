package org.flasck.flas.testrunner;

import java.util.ArrayList;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class TestScript implements TestScriptBuilder {
	private final Scope scope = new Scope(null);
	private int nextStep = 1;
	private String defineInPkg;
	
	public TestScript(String defineInPkg) {
		this.defineInPkg = defineInPkg;
	}

	@Override
	public void error(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(AssertTestStep step) {
		String key = "expr" + nextStep;
		define(key, new FunctionCaseDefn(step.evalPos, CodeType.FUNCTION, key, new ArrayList<>(), step.eval));
		define("value" + nextStep, null);
		nextStep++;
	}

	@Override
	public void addTestCase(String message) {
		// TODO Auto-generated method stub
		
	}

	public Scope scope() {
		return scope;
	}

	private void define(String simple, Object fn) {
		scope.define(simple, defineInPkg+"."+simple, fn);
	}
}
