package org.flasck.flas.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.BlockerTests;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.sampleData.ParsedFormTestData;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class FunctionParsingTests {

	@Test
	public void testParsingFibBlock1() {
		Block b = BlockTestData.fibBlock1();
		BlockerTests.showBlock(0, b);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefn1(), new FunctionParser().tryParsing(new Tokenizable(b.line.text())));
	}

	@Test
	public void testParsingFibBlock2() {
		Block b = BlockTestData.fibBlock2();
		BlockerTests.showBlock(0, b);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefn2(), new FunctionParser().tryParsing(new Tokenizable(b.line.text())));
	}

	@Test
	public void testParsingFibBlockN() {
		Block b = BlockTestData.fibBlockN();
		BlockerTests.showBlock(0, b);
		Object pf = new FunctionParser().tryParsing(new Tokenizable(b.line.text()));
		assertNotNull(pf);
		assertTrue("Return was not an FCD", pf instanceof FunctionCaseDefn);
		FunctionCaseDefn fcd = (FunctionCaseDefn) pf;
		assertTrue("Parsed form was not an apply", fcd.expr instanceof ApplyExpr);
		((ApplyExpr)ParsedFormTestData.fibDefnN().expr).showTree(0);
		((ApplyExpr)fcd.expr).showTree(0);
		ParsedFormTestData.assertFormsEqual(ParsedFormTestData.fibDefnN(), pf);
	}
}
