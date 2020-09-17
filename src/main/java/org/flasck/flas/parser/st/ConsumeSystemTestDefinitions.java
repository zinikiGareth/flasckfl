package org.flasck.flas.parser.st;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestCleanup;
import org.flasck.flas.parsedForm.st.SystemTestConfiguration;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.Repository;

public class ConsumeSystemTestDefinitions implements SystemTestDefinitionConsumer {


	public ConsumeSystemTestDefinitions(ErrorReporter errors, Repository repository, UnitTestPackage stp) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void configure(SystemTestConfiguration utc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void test(SystemTestStage utc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cleanup(SystemTestCleanup utc) {
		// TODO Auto-generated method stub
		
	}
}
