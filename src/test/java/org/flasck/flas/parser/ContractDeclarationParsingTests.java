package org.flasck.flas.parser;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.sampleData.BlockBuilder;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class ContractDeclarationParsingTests {

	@Test
	public void testParsingAContractDeclaration() {
		Object o = new ContractDeclarationSyntax().tryParsing(new Tokenizable(BlockTestData.contractIntroBlock().line.text()));
	}

}
