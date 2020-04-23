package test.flas.testrunner;

import java.io.IOException;

import org.flasck.flas.Configuration;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner;
import org.flasck.flas.testrunner.JVMRunner;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected CommonTestRunner prepareRunner(Configuration config, Repository repository) throws IOException, ErrorResultException {
		return new JVMRunner(config, repository, getClass().getClassLoader(), null);
	}

	protected String prefix() {
		return "JVM";
	}
}
