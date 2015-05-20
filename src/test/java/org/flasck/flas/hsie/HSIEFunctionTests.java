package org.flasck.flas.hsie;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.SingleLineFunctionCase;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class HSIEFunctionTests {

	@Test
	@Ignore
	public void testConvertingFib() {
		SingleLineFunctionCase p = new SingleLineFunctionCase();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib 0 = 1")));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib 1 = 1")));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("fib n = fib (n-1) + fib (n-2)")));
		FunctionDefinition fib = new FunctionDefinition("fib", 1, CollectionUtils.listOf(c1, c2, c3));
		HSIEForm fibForm = HSIE.handle(fib);
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

	@Test
	public void testConvertingTake() {
		SingleLineFunctionCase p = new SingleLineFunctionCase();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("take n [] = []")));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("take 0 Cons = []")));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable(new StringBuilder("take n (a:b) = a:(take (n-1) b)")));
		FunctionDefinition take = new FunctionDefinition("take", 2, CollectionUtils.listOf(c1, c2, c3));
		HSIEForm takeForm = HSIE.handle(take);
		assertNotNull(takeForm);
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
}
