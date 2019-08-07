package test.flas.testrunner;

import java.io.IOException;

import org.flasck.flas.Configuration;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner;
import org.junit.Ignore;

@Ignore
public class JSRunnerTests extends BaseRunnerTests {
	
//	protected JSRunner prepareRunner() throws IOException, ErrorResultException {
////		prior.addJS(new File("src/test/resources/jsrunner/test2/test2.js"));
////		sc.includePrior(prior);
////		File tmpdir = Files.createTempDirectory("testCode").toFile();
////		sc.writeJSTo(tmpdir);
////		sc.createJS("test.runner.script", prior.getPackage().uniqueName(), prior.getScope(), testScope);
//		JSRunner jr = new JSRunner(prior);
////		jr.prepareScript("foo.script", testScope);
////		jr.prepareCase();
//		return jr;
//	}

	@Override
	protected CommonTestRunner prepareRunner(Configuration config, Repository repository)
			throws IOException, ErrorResultException {
		// TODO Auto-generated method stub
		return null;
	}
}
