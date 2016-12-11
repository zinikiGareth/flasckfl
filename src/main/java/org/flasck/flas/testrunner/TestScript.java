package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class TestScript implements TestScriptBuilder {
	private final Scope scope = new Scope(null);
	private final List<SingleTestCase> cases = new ArrayList<SingleTestCase>();
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
		{
			String key = "expr" + nextStep;
			String longName = defineInPkg+"."+key;
			scope.define(key, longName, new FunctionCaseDefn(step.evalPos, CodeType.FUNCTION, longName, new ArrayList<>(), step.eval));
		}
		{
			String key = "value" + nextStep;
			String longName = defineInPkg+"."+key;
			scope.define(key, longName, new FunctionCaseDefn(step.valuePos, CodeType.FUNCTION, longName, new ArrayList<>(), step.value));
		}
		nextStep++;
	}

	@Override
	public void addTestCase(String message) {
		cases.add(new SingleTestCase());
	}

	public Scope scope() {
		return scope;
	}

	public List<SingleTestCase> cases() {
		return cases;
	}
}
