package org.flasck.flas.hsie;

import static org.junit.Assert.assertNotNull;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

// Although these are tests, they are really just to make sure that the data
// we enter in HSIETestData is valid from programs.
public class HSIECodeGenerator {

	@Test
	public void testConvertingIdOf1() {
		FunctionParser p = new FunctionParser(null);
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("f = plus1 1"));
		FunctionDefinition fib = new FunctionDefinition("f", 0, CollectionUtils.listOf(c1));
		HSIEForm fibForm = HSIE.handle(fib);
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.plus1Of1(), fibForm);
	}
}
