package test.flas.testrunner;

import java.io.IOException;

import org.flasck.flas.Configuration;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner;
import org.flasck.flas.testrunner.JVMRunner;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected CommonTestRunner prepareRunner(Configuration config, Repository repository) throws IOException, ErrorResultException {
		return new JVMRunner(config, repository, getClass().getClassLoader());
	}

	/*
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
	*/
}
