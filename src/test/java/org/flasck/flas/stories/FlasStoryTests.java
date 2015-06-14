package org.flasck.flas.stories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class FlasStoryTests {
	private final ErrorResult errors = new ErrorResult();
	private final Rewriter rewriter = new Rewriter(errors);
	private final ScopeEntry se = new PackageDefn(Builtin.builtinScope(), "ME").myEntry();

	@Test
	public void testProcessingFib() {
		Object o = new FLASStory().process(se, BlockTestData.allFib());
		assertNotNull(o);
		assertTrue(o instanceof ScopeEntry);
		ScopeEntry se = (ScopeEntry) o;
		Scope s = ((PackageDefn)se.getValue()).innerScope();
		assertEquals(1, s.size());
	}

	@Test
	public void testProcessingMutualRecursion() {
		Object o = new FLASStory().process(se, BlockTestData.simpleMutualRecursionBlock());
		assertNotNull(o);
		assertTrue(o instanceof ScopeEntry);
		ScopeEntry se = (ScopeEntry) o;
		rewriter.rewrite(se);
		assertEquals(2, rewriter.functions.size());
		FunctionDefinition f = rewriter.functions.get("ME.f");
		assertEquals("ME.f", f.name);
		FunctionCaseDefn c1 = f.cases.get(0);
		assertEquals("ME.f", c1.intro.name);
		HSIEForm form = new HSIE(errors).handle(f);
		HSIETestData.assertHSIE(HSIETestData.mutualF(), form);
		FunctionDefinition g = rewriter.functions.get("ME.f_0.g");
		HSIEForm gorm = new HSIE(errors).handle(g, form.vars.size(), form.varsFor(0));
		assertEquals(1, gorm.externals.size());
		assertTrue(gorm.externals.contains(new AbsoluteVar("FLEval.mul", null)));
		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}

	@Test
	public void testProcessingAMultiPartFunctionWithSeparateNestedScopes() throws IOException {
		Object o = new FLASStory().process(se, BlockTestData.splitNestedBlocks());
		assertNotNull(o);
		assertTrue(o instanceof ScopeEntry);
		ScopeEntry se = (ScopeEntry) o;
		rewriter.rewrite(se);
		assertEquals(3, rewriter.functions.size());
		FunctionDefinition f = rewriter.functions.get("ME.f");
		assertEquals(2, f.cases.size());
		FunctionCaseDefn c1 = f.cases.get(0);
		FunctionCaseDefn c2 = f.cases.get(1);
		HSIEForm form = new HSIE(errors).handle(f);
		HSIETestData.assertHSIE(HSIETestData.splitF(), form);
		FunctionDefinition g1 = rewriter.functions.get("ME.f_0.g");
		HSIEForm gorm1 = new HSIE(errors).handle(g1, form.vars.size(), form.varsFor(0));
		HSIETestData.assertHSIE(HSIETestData.splitF_G1(), gorm1);
		FunctionDefinition g2 = rewriter.functions.get("ME.f_1.g");
		HSIEForm gorm2 = new HSIE(errors).handle(g2, form.vars.size(), form.varsFor(1));
		HSIETestData.assertHSIE(HSIETestData.splitF_G2(), gorm2);
	}
	
	@Test
	public void testLiftingOfCardMethods() throws Exception {
		Object o = new FLASStory().process(se, BlockTestData.cardWithMethods());
		assertNotNull(o);
		assertTrue(o instanceof ScopeEntry);
		ScopeEntry se = (ScopeEntry) o;
		rewriter.rewrite(se);
		Scope s = ((PackageDefn)se.getValue()).innerScope();
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

		FunctionDefinition actionFD = MethodConvertor.convert(s, action.intro.name, action);
		assertNotNull(actionFD);
		actionFD.dumpTo(new PrintWriter(System.out));
	}
}
