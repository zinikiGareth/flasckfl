package org.flasck.flas.stories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class FlasStoryTests {
	private final ErrorResult errors = new ErrorResult();
	private final Rewriter rewriter = new Rewriter(errors, null);
	private final Scope s = new Scope(null, null);

	@Test
	public void testProcessingFib() {
		new FLASStory().process(s, BlockTestData.allFib());
		assertEquals(1, s.size());
	}

	@Test
	public void testProcessingMutualRecursion() {
		new FLASStory().process(s, BlockTestData.simpleMutualRecursionBlock());
		rewriter.rewritePackageScope("ME", s);
		assertEquals(2, rewriter.functions.size());
		RWFunctionDefinition f = rewriter.functions.get("ME.f");
		assertEquals("ME.f", f.name());
		RWFunctionCaseDefn c1 = f.cases.get(0);
		assertEquals("ME.f", c1.methodName());
		assertEquals("ME.f_0", c1.caseName());
		HSIEForm form = new HSIE(errors, rewriter).handle(null, f);
		HSIETestData.assertHSIE(HSIETestData.mutualF(), form);
		RWFunctionDefinition g = rewriter.functions.get("ME.f_0.g");
		HSIEForm gorm = new HSIE(errors, rewriter).handle(null, g, form.vars.size(), form.varsFor(0));
		assertEquals(1, gorm.externals.size());
		assertTrue(gorm.externals.contains(new PackageVar(null, "FLEval.mul", null)));
		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}

	@Test
	public void testProcessingAMultiPartFunctionWithSeparateNestedScopes() throws IOException {
		new FLASStory().process(s, BlockTestData.splitNestedBlocks());
		rewriter.rewritePackageScope("ME", s);
		assertEquals(3, rewriter.functions.size());
		RWFunctionDefinition f = rewriter.functions.get("ME.f");
		assertEquals(2, f.cases.size());
//		RWFunctionCaseDefn c1 = f.cases.get(0);
//		RWFunctionCaseDefn c2 = f.cases.get(1);
		HSIEForm form = new HSIE(errors, rewriter).handle(null, f);
		HSIETestData.assertHSIE(HSIETestData.splitF(), form);
		RWFunctionDefinition g1 = rewriter.functions.get("ME.f_0.g");
		HSIEForm gorm1 = new HSIE(errors, rewriter).handle(null, g1, form.vars.size(), form.varsFor(0));
		HSIETestData.assertHSIE(HSIETestData.splitF_G1(), gorm1);
		RWFunctionDefinition g2 = rewriter.functions.get("ME.f_1.g");
		HSIEForm gorm2 = new HSIE(errors, rewriter).handle(null, g2, form.vars.size(), form.varsFor(1));
		HSIETestData.assertHSIE(HSIETestData.splitF_G2(), gorm2);
	}
	
	@Test
	public void testLiftingOfCardMethods() throws Exception {
		new FLASStory().process(s, BlockTestData.cardWithMethods());
		rewriter.rewritePackageScope("ME", s);
		assertEquals(2, s.size());
		CardDefinition cd = (CardDefinition) s.get("Mycard");
		assertNotNull(cd.state);
		assertNotNull(cd.template);
		assertNotNull(cd.innerScope());
		Scope is = cd.innerScope();
		assertEquals(2, is.size());
		FunctionDefinition render = (FunctionDefinition) is.get("render");
		EventHandlerDefinition action = (EventHandlerDefinition) is.get("action");
		
		// NOTE: this is now writing back into the card's "inner scope"
//		render = rewriter.rewriteFunction(cd.innerScope(), cd, render);
//		assertNotNull(render);
		render.dumpTo(new PrintWriter(System.out));
		assertEquals("ME.Mycard.render", render.name);

		// TODO: this doesn't work because converting methods is now harder than it was.
		// I'm not actually sure it's intrinsic to the value of this test anyway
		// If it is, this test should probably be moved to MethodConvertorTests
//		TypeChecker tc = new TypeChecker(errors);
//		tc.addExternal("Any", Type.builtin(null, "Any"));
//		MethodConvertor convertor = new MethodConvertor(errors, new HSIE(errors), tc, new HashMap<>());
//		FunctionDefinition actionFD = convertor.convertEventHandler(s, action.intro.name, action);
//		assertNotNull(actionFD);
//		actionFD.dumpTo(new PrintWriter(System.out));
	}
	
	@Test
	public void testSimpleIfThatErrors() throws Exception {
		new FLASStory().process(s, BlockTestData.simpleIf());
		rewriter.rewritePackageScope("ME", s);
		assertEquals(1, rewriter.functions.size());
		RWFunctionDefinition fact = rewriter.functions.get("ME.fact");
		assertEquals(1, fact.cases.size());
		HSIEForm form = new HSIE(errors, rewriter).handle(null, fact);
		errors.showTo(new PrintWriter(System.out), 0);
		assertTrue(!errors.hasErrors());
		HSIETestData.assertHSIE(HSIETestData.simpleIf(), form);
	}
	
	@Test
	public void testSimpleIfElse() throws Exception {
		new FLASStory().process(s, BlockTestData.simpleIfElse());
		rewriter.rewritePackageScope("ME", s);
		assertEquals(1, rewriter.functions.size());
		RWFunctionDefinition fact = rewriter.functions.get("ME.fact");
		assertEquals(1, fact.cases.size());
		HSIEForm form = new HSIE(errors, rewriter).handle(null, fact);
		errors.showTo(new PrintWriter(System.out), 0);
		assertTrue(!errors.hasErrors());
		HSIETestData.assertHSIE(HSIETestData.simpleIfElse(), form);
	}
}
