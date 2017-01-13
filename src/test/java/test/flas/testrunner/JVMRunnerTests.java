package test.flas.testrunner;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.junit.Before;

import test.supporting.SupportingFunctions;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.runner.script", prior, testScope);
		JVMRunner jr = new JVMRunner(prior);
		jr.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		jr.prepareScript(sc, testScope);
		jr.prepareCase();
		runner = jr;
	}
	
	@Before
	public void defineSupportingFunctions() {
		SupportingFunctions.defineSupportingFunctions(bce);
	}
}
