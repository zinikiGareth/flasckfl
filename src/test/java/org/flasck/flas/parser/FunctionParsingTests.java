package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.sampleData.ParsedFormTestData;
import org.junit.Test;

public class FunctionParsingTests {

	@Test
	public void testParsingAContractDeclaration() {
		Block b = BlockTestData.fibBlock1();
		BlockParsingTests.showBlock(0, b);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefn1(), new BlockParser(SingleLineFunctionCase.class).parse(b));
	}
}
