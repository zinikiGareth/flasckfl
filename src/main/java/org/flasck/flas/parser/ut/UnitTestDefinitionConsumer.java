package org.flasck.flas.parser.ut;

import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;

public interface UnitTestDefinitionConsumer extends IntroductionConsumer, FunctionScopeUnitConsumer {
	void testCase(UnitTestCase utc);
	void data(UnitDataDeclaration data);
	void nestedData(UnitDataDeclaration data);
}
