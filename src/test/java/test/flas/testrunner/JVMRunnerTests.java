package test.flas.testrunner;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.golden.GoldenCGRunner;
import org.flasck.flas.testrunner.JVMRunner;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.runner.script", prior, testScope);
		JVMRunner jr = new JVMRunner(prior);
		jr.considerResource(new File(jvmClasses(), "classes"));
		jr.prepareScript(sc, testScope);
		jr.prepareCase();
		runner = jr;
	}
	
	private File jvmClasses() {
		File jvmbin = new File(GoldenCGRunner.jvmdir, "jvm/bin");
		if (!jvmbin.exists())
			jvmbin = new File(GoldenCGRunner.jvmdir, "jvm/qbout");
		if (!jvmbin.exists())
			throw new RuntimeException("No jvm bin directory could be found");
		return jvmbin;
	}
}
