package org.flasck.flas.hsie;

import static org.junit.Assert.assertNotNull;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class HSIEFunctionTests {

	@Test
	public void testConvertingConstant() {
		FunctionParser p = new FunctionParser();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		FunctionDefinition primes = new FunctionDefinition("primes", 0, CollectionUtils.listOf(c1));
		HSIEForm primesForm = HSIE.handle(primes);
		assertNotNull(primesForm);
		HSIETestData.assertHSIE(HSIETestData.testPrimes(), primesForm);
	}
	
	@Test
	public void testConvertingFib() {
		FunctionParser p = new FunctionParser();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 0 = 1"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		FunctionDefinition fib = new FunctionDefinition("fib", 1, CollectionUtils.listOf(c1, c2, c3));
		HSIEForm fibForm = HSIE.handle(fib);
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

	@Test
	public void testConvertingTake() {
		FunctionParser p = new FunctionParser();
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n [] = []"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		FunctionDefinition take = new FunctionDefinition("take", 2, CollectionUtils.listOf(c1, c2, c3));
		HSIEForm takeForm = HSIE.handle(take);
		assertNotNull(takeForm);
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
}
