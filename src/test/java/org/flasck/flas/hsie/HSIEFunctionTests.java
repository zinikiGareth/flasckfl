package org.flasck.flas.hsie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class HSIEFunctionTests {
	private ErrorResult errors = new ErrorResult();
	
	@Test
	public void testConvertingConstant() {
		PackageDefn pkg = new PackageDefn(Builtin.builtinScope(), "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("primes = [2,3,5]"));
		FunctionDefinition primes = new FunctionDefinition(Type.FUNCTION, "primes", 0, CollectionUtils.listOf(c1));
		pkg.innerScope().define("primes", "primes", primes);
		Rewriter rw = new Rewriter(errors);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm primesForm = new HSIE(errors).handle(rw.functions.get("primes"));
		assertNotNull(primesForm);
		HSIETestData.assertHSIE(HSIETestData.testPrimes(), primesForm);
	}
	
	@Test
	public void testConvertingFib() {
		PackageDefn pkg = new PackageDefn(Builtin.builtinScope(), "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 0 = 1"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib 1 = 1"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("fib n = fib (n-1) + fib (n-2)"));
		FunctionDefinition fib = new FunctionDefinition(Type.FUNCTION, "fib", 1, CollectionUtils.listOf(c1, c2, c3));
		pkg.innerScope().define("fib", "fib", fib);
		Rewriter rw = new Rewriter(errors);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm fibForm = new HSIE(errors).handle(rw.functions.get("fib"));
		assertNotNull(fibForm);
		HSIETestData.assertHSIE(HSIETestData.fib(), fibForm);
	}

	@Test
	public void testConvertingTake() {
		PackageDefn pkg = new PackageDefn(Builtin.builtinScope(), "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.Type.FUNCTION));
		FunctionCaseDefn c1 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n [] = []"));
		FunctionCaseDefn c2 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take 0 Cons = []"));
		FunctionCaseDefn c3 = (FunctionCaseDefn)p.tryParsing(new Tokenizable("take n (a:b) = a:(take (n-1) b)"));
		FunctionDefinition take = new FunctionDefinition(Type.FUNCTION, "take", 2, CollectionUtils.listOf(c1, c2, c3));
		pkg.innerScope().define("take", "take", take);
		Rewriter rw = new Rewriter(errors);
		rw.rewrite(pkg.myEntry());
		System.out.println(rw.functions);
		HSIEForm takeForm = new HSIE(errors).handle(rw.functions.get("take"));
		assertNotNull(takeForm);
		assertEquals(0, errors.errors.size());
		HSIETestData.assertHSIE(HSIETestData.take(), takeForm);
	}
}
