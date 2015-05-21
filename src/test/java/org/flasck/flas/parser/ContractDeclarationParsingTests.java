package org.flasck.flas.parser;

import static org.junit.Assert.assertNotNull;

import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class ContractDeclarationParsingTests {

	@Test
	public void testParsingAContractDeclaration() {
		Object o = new IntroParser().tryParsing(new Tokenizable(BlockTestData.contractIntroBlock().line.text()));
		assertNotNull(o);
	}

}
