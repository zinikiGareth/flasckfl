package test.flas.testrunner;

import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JSRunner;

public class JSRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJS("test.golden.script", prior, testScope);
		JSRunner jr = new JSRunner(prior);
//		jr.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		jr.prepareScript(sc, testScope);
		runner = jr;
	}
}
