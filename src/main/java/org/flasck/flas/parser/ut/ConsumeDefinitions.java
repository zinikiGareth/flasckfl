package org.flasck.flas.parser.ut;

import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public class ConsumeDefinitions implements UnitTestDefinitionConsumer {
	private final TopLevelDefinitionConsumer consumer;
	private final UnitTestPackage pkg;

	public ConsumeDefinitions(TopLevelDefinitionConsumer consumer, UnitTestPackage pkg) {
		this.consumer = consumer;
		this.pkg = pkg;
	}

	@Override
	public void testCase(UnitTestCase utc) {
		pkg.testCase(utc);
	}

	@Override
	public void data(UnitDataDeclaration data) {
		consumer.newTestData(data);
		pkg.data(data);
	}
	
	@Override
	public void nestedData(UnitDataDeclaration data) {
		consumer.newTestData(data);
	}

	@Override
	public void newIntroduction(IntroduceVar var) {
		consumer.newIntroduction(var);
	}
}
