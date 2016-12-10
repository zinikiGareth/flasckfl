package org.flasck.flas.testrunner;

import junit.framework.TestCase;

public class TestScript implements TestScriptBuilder {
	public TestScript() {
	}

	@Override
	public void add(TestCase test) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void error(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Deprecated // we want to return a Scope here, not a String
	public String flas() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(AssertTestStep assertTestStep) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTestCase(String message) {
		// TODO Auto-generated method stub
		
	}
}
