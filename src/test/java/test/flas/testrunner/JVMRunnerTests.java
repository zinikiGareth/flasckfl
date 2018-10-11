package test.flas.testrunner;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.ziniki.cbstore.json.FLConstructorServer;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.runner.script", prior, testScope);
		JVMRunner jr = new JVMRunner(prior, new FLConstructorServer());
		jr.considerResource(new File(jvmClasses(), "classes"));
		jr.prepareScript("test.runner.script", sc, testScope);
		jr.prepareCase();
		runner = jr;
	}
	
	private File jvmClasses() {
		File jvmdir;
		File jd = new File("/Users/gareth/Ziniki/Over/FLASJvm");
		if (!jd.exists()) {
			jd = new File("../FLASJvm");
			if (!jd.exists()) {
				System.err.println("There is no directory for the FLASJvm code");
				jd = null;
			}
		}
		jvmdir = jd;

		File jvmbin = new File(jvmdir, "jvm/bin");
		if (!jvmbin.exists())
			jvmbin = new File(jvmdir, "jvm/qbout");
		if (!jvmbin.exists())
			throw new RuntimeException("No jvm bin directory could be found");
		return jvmbin;
	}
}
