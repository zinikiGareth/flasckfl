package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Test;

public class HSIEFunctionTests {
	private ErrorResult errors = new ErrorResult();
	
	/* TODO: simplify-parsing
	@Test
	public void testConvertingConstant() {
		Scope s = new Scope(null);
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		FunctionDefinition primes = new FunctionDefinition(null, CodeType.FUNCTION, "ME.primes", 0);
		ScopeEntry mf = s.define("primes", "ME.primes", primes);
		primes.cases.add(new FunctionCaseDefn(mf, c1, 0));
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
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		FunctionDefinition fib = new FunctionDefinition(null, CodeType.FUNCTION, "ME.fib", 1);
		ScopeEntry mf = s.define("fib", "ME.fib", fib);
		fib.cases.add(new FunctionCaseDefn(mf, c1, 0));
		fib.cases.add(new FunctionCaseDefn(mf, c2, 1));
		fib.cases.add(new FunctionCaseDefn(mf, c3, 2));
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
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		FunctionDefinition take = new FunctionDefinition(null, CodeType.FUNCTION, "ME.take", 2);
		ScopeEntry mf = s.define("take", "ME.take", take);
		take.cases.add(new FunctionCaseDefn(mf, c1, 0));
		take.cases.add(new FunctionCaseDefn(mf, c2, 1));
		take.cases.add(new FunctionCaseDefn(mf, c3, 2));
		Rewriter rw = new Rewriter(errors, null, Builtin.builtins());
		rw.rewritePackageScope("ME", s);
		System.out.println(rw.functions);
		HSIEForm takeForm = new HSIE(errors, rw).handle(null, rw.functions.get("ME.take"));
		assertNotNull(takeForm);
		assertEquals(0, errors.count());
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
	*/
}
