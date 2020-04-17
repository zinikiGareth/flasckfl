package org.flasck.flas.parser.ut;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public class ConsumeDefinitions implements UnitTestDefinitionConsumer {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private final UnitTestPackage pkg;

	public ConsumeDefinitions(ErrorReporter errors, TopLevelDefinitionConsumer consumer, UnitTestPackage pkg) {
		this.errors = errors;
		this.consumer = consumer;
		this.pkg = pkg;
	}

	@Override
	public void testCase(UnitTestCase utc) {
		pkg.testCase(utc);
	}

	@Override
	public void data(UnitDataDeclaration data) {
		consumer.newTestData(errors, data);
		pkg.data(data);
	}
	
	@Override
	public void nestedData(UnitDataDeclaration data) {
		consumer.newTestData(errors, data);
	}

	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
		consumer.newIntroduction(errors, var);
	}
}
