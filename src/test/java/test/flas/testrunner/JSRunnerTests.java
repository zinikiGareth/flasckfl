package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner;
import org.flasck.flas.testrunner.JSRunner;
import org.jmock.Expectations;
import org.junit.Before;

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
	
	private JSStorage jse;

	@Before
	public void setup() {
		jse = context.mock(JSStorage.class);
		ArrayList<File> fileList = new ArrayList<>();
		fileList.add(new File("src/test/resources/jsrunner/tests.js"));
		ArrayList<String> pkgList = new ArrayList<>();
		pkgList.add("tests");
		context.checking(new Expectations() {{
			allowing(jse).packages(); will(returnValue(pkgList));
			allowing(jse).files(); will(returnValue(fileList));
			allowing(jse).fileFor("tests"); will(returnValue(fileList.get(0)));
		}});
	}

	@Override
	protected CommonTestRunner<?> prepareRunner(Configuration config, Repository repository)
			throws IOException, ErrorResultException {
		return new JSRunner(config, repository, jse, new HashMap<>());
	}

	protected String prefix() {
		return "JS";
	}
}
