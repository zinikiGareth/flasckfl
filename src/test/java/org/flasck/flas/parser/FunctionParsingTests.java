package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.junit.Test;

public class FunctionParsingTests {

	@Test
	public void testParsingAContractDeclaration() {
		Block b = builder()
		  .line("fib 0 = 1")
		  .build();
		BlockParsingTests.showBlock(0, b);
		Object o = new BlockParser(SingleLineFunctionCase.class).parse(b);
		assertNotNull(o);
		assertTrue(o instanceof FunctionCaseDefn);
		FunctionCaseDefn fcd = (FunctionCaseDefn) o;
		assertEquals("fib", fcd.name);
		assertEquals(1, fcd.args.size());
		assertEquals("0", fcd.args.get(0));
		assertTrue(fcd.expr instanceof ItemExpr);
		ItemExpr ie = (ItemExpr) fcd.expr;
		assertEquals(ExprToken.NUMBER, ie.tok.type);
		assertEquals("1", ie.tok.text);
	}

	private BlockBuilder builder() {
		return new BlockBuilder();
	}

}
