package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.sampleData.BlockBuilder;
import org.flasck.flas.sampleData.BlockTestData;
import org.junit.Test;

public class ContractDeclarationParsingTests {

	@Test
	public void testParsingAContractDeclaration() {
		Object o = new BlockParser(ContractDeclarationSyntax.class).parse(BlockTestData.contractIntroBlock());
		
	}

}
