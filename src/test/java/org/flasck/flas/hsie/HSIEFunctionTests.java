package org.flasck.flas.hsie;

import org.flasck.flas.errors.ErrorResult;

public class HSIEFunctionTests {
	private ErrorResult errors = new ErrorResult();
	
	/* TODO: big-divide
	@Test
	public void testConvertingConstant() {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		FunctionDefinition primes = new FunctionDefinition(null, CodeType.FUNCTION, "primes", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("primes", "primes", primes);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm primesForm = new HSIE(errors, rw).handle(null, rw.functions.get("primes"));
		assertNotNull(primesForm);
		HSIETestData.assertHSIE(HSIETestData.testPrimes(), primesForm);
	}
	
	@Test
	public void testConvertingFib() {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 0 = 1"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		FunctionDefinition fib = new FunctionDefinition(null, CodeType.FUNCTION, "fib", 1, CollectionUtils.listOf(c1, c2, c3));
		pkg.innerScope().define("fib", "fib", fib);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm fibForm = new HSIE(errors, rw).handle(null, rw.functions.get("fib"));
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

	@Test
	public void testConvertingTake() {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n [] = []"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		FunctionDefinition take = new FunctionDefinition(null, CodeType.FUNCTION, "take", 2, CollectionUtils.listOf(c1, c2, c3));
		pkg.innerScope().define("take", "take", take);
		Rewriter rw = new Rewriter(errors, null);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm takeForm = new HSIE(errors, rw).handle(null, rw.functions.get("take"));
		assertNotNull(takeForm);
		assertEquals(0, errors.count());
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
	*/
}
