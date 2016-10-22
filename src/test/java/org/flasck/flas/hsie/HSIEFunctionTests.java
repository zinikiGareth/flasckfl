package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class HSIEFunctionTests {
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingConstant() {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		c1.provideCaseName("ME.primes_0");
		s.define("primes", "ME.primes", c1);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		System.out.println(rw.functions);
		HSIEForm primesForm = new HSIE(errors, rw).handle(null, rw.functions.get("ME.primes"));
		assertNotNull(primesForm);
		HSIETestData.assertHSIE(HSIETestData.testPrimes(), primesForm);
	}
	
	@Test
	public void testConvertingFib() {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 0 = 1"));
		c1.provideCaseName("ME.fib_0");
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		c2.provideCaseName("ME.fib_1");
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		c3.provideCaseName("ME.fib_2");
		s.define("fib", "ME.fib", c1);
		s.define("fib", "ME.fib", c2);
		s.define("fib", "ME.fib", c3);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		System.out.println(rw.functions);
		HSIEForm fibForm = new HSIE(errors, rw).handle(null, rw.functions.get("ME.fib"));
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

	@Test
	public void testConvertingTake() {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n [] = []"));
		c1.provideCaseName("ME.take_0");
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		c2.provideCaseName("ME.take_1");
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		c3.provideCaseName("ME.take_2");
		s.define("take", "ME.take", c1);
		s.define("take", "ME.take", c2);
		s.define("take", "ME.take", c3);
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		System.out.println(rw.functions);
		HSIEForm takeForm = new HSIE(errors, rw).handle(null, rw.functions.get("ME.take"));
		assertNotNull(takeForm);
		assertEquals(0, errors.count());
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
}
