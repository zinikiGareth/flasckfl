package org.flasck.flas.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.sampleData.ParsedFormTestData;
import org.junit.Test;

public class FunctionParsingTests {

	@Test
	public void testParsingFibBlock1() {
		Block b = BlockTestData.fibBlock1();
		BlockParsingTests.showBlock(0, b);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefn1(), new BlockParser(SingleLineFunctionCase.class).parse(b));
	}

	@Test
	public void testParsingFibBlock2() {
		Block b = BlockTestData.fibBlock2();
		BlockParsingTests.showBlock(0, b);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefn2(), new BlockParser(SingleLineFunctionCase.class).parse(b));
	}

	@Test
	public void testParsingFibBlockN() {
		Block b = BlockTestData.fibBlockN();
		BlockParsingTests.showBlock(0, b);
		Object pf = new BlockParser(SingleLineFunctionCase.class).parse(b);
		assertNotNull(pf);
		assertTrue("Return was not an FCD", pf instanceof FunctionCaseDefn);
		FunctionCaseDefn fcd = (FunctionCaseDefn) pf;
		assertTrue("Parsed form was not an apply", fcd.expr instanceof ApplyExpr);
		((ApplyExpr)ParsedFormTestData.fibDefnN().expr).showTree(0);
		((ApplyExpr)fcd.expr).showTree(0);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefnN(), pf);
	}
}
