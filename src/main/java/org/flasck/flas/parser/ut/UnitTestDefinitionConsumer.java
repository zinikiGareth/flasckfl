package org.flasck.flas.parser.ut;

import org.flasck.flas.parsedForm.ut.UnitTestCase;

public interface UnitTestDefinitionConsumer extends IntroductionConsumer {
	void testCase(UnitTestCase utc);
	void data(UnitDataDeclaration data);
	void nestedData(UnitDataDeclaration data);
}
