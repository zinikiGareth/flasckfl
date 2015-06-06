package org.flasck.flas.stories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.Rewriter;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class FlasStoryTests {
	private final ErrorResult errors = new ErrorResult();
	private final Rewriter rewriter = new Rewriter(errors);

	@Test
	public void testProcessingFib() {
		Object o = new FLASStory().process("MathLib", BlockTestData.allFib());
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = (Scope) o;
		assertEquals(1, s.size());
	}

	@Test
	public void testProcessingMutualRecursion() {
		Object o = new FLASStory().process("ME", BlockTestData.simpleMutualRecursionBlock());
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = (Scope) o;
		s = rewriter.rewrite(s);
		assertEquals(1, s.size());
		FunctionDefinition f = (FunctionDefinition) s.get("f");
		assertEquals("ME.f", s.resolve("f"));
		FunctionCaseDefn c1 = f.cases.get(0);
		assertEquals("ME.f", c1.innerScope().resolve("f"));
		assertEquals("ME.f_0.g", c1.innerScope().resolve("g"));
		assertEquals(1, c1.innerScope().size());
		HSIEForm form = HSIE.handle(f);
		HSIETestData.assertHSIE(HSIETestData.mutualF(), form);
		FunctionDefinition g = (FunctionDefinition) c1.innerScope().get("g");
		HSIEForm gorm = HSIE.handle(g, form.vars.size(), form.varsFor(0));
		assertEquals(1, gorm.externals.size());
		assertTrue(gorm.externals.contains("FLEval.mul"));
		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}

	@Test
	public void testProcessingAMultiPartFunctionWithSeparateNestedScopes() throws IOException {
		Object o = new FLASStory().process("ME", BlockTestData.splitNestedBlocks());
		System.out.println(o);
//		((ErrorResult)o).showTo(new PrintWriter(System.out));
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = (Scope) o;
		s = rewriter.rewrite(s);
		assertEquals(1, s.size());
		FunctionDefinition f = (FunctionDefinition) s.get("f");
		assertEquals(2, f.cases.size());
		FunctionCaseDefn c1 = f.cases.get(0);
		FunctionCaseDefn c2 = f.cases.get(1);
		assertEquals("ME.f", s.resolve("f"));
		assertEquals("ME.f", c1.innerScope().resolve("f"));
		assertEquals("ME.f", c2.innerScope().resolve("f"));
		assertEquals("ME.f_0.g", c1.innerScope().resolve("g"));
		assertEquals("ME.f_1.g", c2.innerScope().resolve("g"));
		assertEquals(1, c1.innerScope().size());
		assertEquals(1, c2.innerScope().size());
		HSIEForm form = HSIE.handle(f);
		HSIETestData.assertHSIE(HSIETestData.splitF(), form);
		FunctionDefinition g1 = (FunctionDefinition) c1.innerScope().get("g");
		HSIEForm gorm1 = HSIE.handle(g1, form.vars.size(), form.varsFor(0));
		HSIETestData.assertHSIE(HSIETestData.splitF_G1(), gorm1);
		FunctionDefinition g2 = (FunctionDefinition) c2.innerScope().get("g");
		HSIEForm gorm2 = HSIE.handle(g2, form.vars.size(), form.varsFor(1));
		HSIETestData.assertHSIE(HSIETestData.splitF_G2(), gorm2);
//		assertEquals(2, gorm.externals.size());
//		assertTrue(gorm.externals.contains("FLEval.mul"));
//		assertTrue(gorm.externals.contains("_scoped.x"));
//		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}
	
	@Test
	public void testLiftingOfCardMethods() throws Exception {
		Object o = new FLASStory().process("ME", BlockTestData.cardWithMethods());
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = rewriter.rewrite((Scope)o);
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
		render = rewriter.rewriteFunction(cd.innerScope(), cd, render);
		assertNotNull(render);
		render.dumpTo(new PrintWriter(System.out));
		assertEquals("ME.Mycard.prototype.render", render.name);

		FunctionDefinition actionFD = MethodConvertor.convert(action.intro.name, rewriter.rewriteEventHandler(cd.innerScope(), cd, action));
		assertNotNull(actionFD);
		actionFD.dumpTo(new PrintWriter(System.out));
	}
}
