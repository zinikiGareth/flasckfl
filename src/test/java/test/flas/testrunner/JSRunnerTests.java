package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JSRunner;

public class JSRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		prior.addJS(new File("src/test/resources/jsrunner/test2/test2.js"));
		sc.includePrior(prior);
		File tmpdir = Files.createTempDirectory("testCode").toFile();
		sc.writeJSTo(tmpdir);
		sc.createJS("test.runner.script", prior.getPackage().uniqueName(), prior.getScope(), testScope);
		JSRunner jr = new JSRunner(prior);
		jr.prepareScript("foo.script", sc, testScope);
		jr.prepareCase();
		runner = jr;
	}
}
