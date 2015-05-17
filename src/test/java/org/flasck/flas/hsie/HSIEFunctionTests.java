package org.flasck.flas.hsie;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.SingleLineFunctionCase;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class HSIEFunctionTests {

	@Test
	public void test() {
		SingleLineFunctionCase p = new SingleLineFunctionCase();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib 0 = 1")));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib 1 = 1")));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib n = fib (n-1) + fib (n-2)")));
		FunctionDefinition fib = new FunctionDefinition(CollectionUtils.listOf(c1, c2, c3));
		HSIEForm fibForm = HSIE.handle(fib);
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

}
