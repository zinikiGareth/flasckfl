package org.flasck.flas.parser.ut;

import org.flasck.flas.parsedForm.ut.UnitTestCase;

public interface UnitTestDefinitionConsumer {

	void testCase(UnitTestCase utc);
	void data(UnitDataDeclaration data);

}
