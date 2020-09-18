package org.flasck.flas.parser.st;

import org.flasck.flas.parsedForm.st.SystemTestCleanup;
import org.flasck.flas.parsedForm.st.SystemTestConfiguration;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.ut.IntroductionConsumer;

public interface SystemTestDefinitionConsumer extends IntroductionConsumer, FunctionScopeUnitConsumer {
	void configure(SystemTestConfiguration utc);
	void test(SystemTestStage utc);
	void cleanup(SystemTestCleanup utc);
}
