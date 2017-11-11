package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class HSIEFunctionTests {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingConstant() {
		Scope s = Scope.topScope("ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME"));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		c1.provideCaseName(0);
		s.define("primes", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		HSIEForm primesForm = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.primes"));
		assertNotNull(primesForm);
		HSIETestData.assertHSIE(HSIETestData.testPrimes(context), primesForm);
	}

	@Test
	public void testConvertingFib() {
		Scope s = Scope.topScope("ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME"));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 0 = 1"));
		c1.provideCaseName(0);
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		c2.provideCaseName(1);
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		c3.provideCaseName(2);
		s.define("fib", c1);
		s.define("fib", c2);
		s.define("fib", c3);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		HSIEForm fibForm = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.fib"));
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(context), fibForm);
	}

	@Test
	public void testConvertingTake() throws Exception {
		Scope s = Scope.topScope("ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME"));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n [] = []"));
		c1.provideCaseName(0);
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		c2.provideCaseName(1);
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		c3.provideCaseName(2);
		s.define("take", c1);
		s.define("take", c2);
		s.define("take", c3);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope(null, "ME", s);
		HSIEForm takeForm = HSIETestData.doHSIE(errors, rw, rw.functions.get("ME.take"));
		assertNotNull(takeForm);
		errors.showTo(new PrintWriter(System.out), 0);
		assertEquals(0, errors.count());
		HSIETestData.assertHSIE(HSIETestData.take(context), takeForm);
	}
}
