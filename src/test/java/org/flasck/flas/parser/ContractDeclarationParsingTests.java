package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class ContractDeclarationParsingTests {

	@Test
	public void testParsingAContractIntroduction() {
		Object o = new IntroParser(new State(null, null)).tryParsing(new Tokenizable(BlockTestData.contractIntroBlock()));
		assertNotNull(o);
		assertTrue(o instanceof ContractDecl);
		ContractDecl cd = (ContractDecl) o;
		assertEquals("OnTick", cd.nameAsName().baseName());
	}

	@Test
	public void testParsingAContractDeclarationWithFields() {
		Object o = new IntroParser(new State(null, null)).tryParsing(new Tokenizable(BlockTestData.contractWithMethodBlock()));
		assertNotNull(o);
		assertTrue(o instanceof ContractDecl);
		ContractDecl cd = (ContractDecl) o;
		assertEquals("OnTick", cd.nameAsName().baseName());
		new FieldParser(FieldParser.CARD).tryParsing(new Tokenizable(BlockTestData.contractWithMethodBlock().nested.get(0)));
	}

	@Test
	public void testParsingAContractDeclarationField() {
		Object o = new MethodParser(new State(null, null)).tryParsing(new Tokenizable(BlockTestData.contractWithMethodBlock().nested.get(0)));
		assertNotNull(o);
		assertTrue(o instanceof ContractMethodDecl);
		ContractMethodDecl cd = (ContractMethodDecl) o;
		assertEquals("up", cd.dir);
		assertEquals("call", cd.name.name);
		assertEquals(1, cd.args.size());
		Object patt = cd.args.get(0);
		assertTrue(patt instanceof VarPattern);
		VarPattern p = (VarPattern) patt;
		assertEquals("x", p.var);
	}
}
