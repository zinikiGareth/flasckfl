package org.flasck.flas.stories;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.Rewriter;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.sampleData.BlockTestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class FlasStoryTests {
	private final Rewriter rewriter = new Rewriter();

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
		assertEquals("ME.f.g", c1.innerScope().resolve("g"));
		System.out.println(f.cases.get(0));
		System.out.println(f.cases.get(0).expr);
		assertEquals(1, c1.innerScope().size());
		HSIEForm form = HSIE.handle(f);
		HSIETestData.assertHSIE(HSIETestData.mutualF(), form);
		FunctionDefinition g = (FunctionDefinition) c1.innerScope().get("g");
		HSIEForm gorm = HSIE.handle(g, form.vars.size());
		assertEquals(2, gorm.externals.size());
		assertTrue(gorm.externals.contains("FLEval.mul"));
		assertTrue(gorm.externals.contains("_scoped.x"));
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
		System.out.println(c1);
		System.out.println(c1.expr);
		assertEquals(1, c1.innerScope().size());
		assertEquals(1, c2.innerScope().size());
		HSIEForm form = HSIE.handle(f);
		HSIETestData.assertHSIE(HSIETestData.splitF(), form);
		FunctionDefinition g1 = (FunctionDefinition) c1.innerScope().get("g");
		HSIEForm gorm1 = HSIE.handle(g1, form.vars.size());
		gorm1.dump();
		HSIETestData.assertHSIE(HSIETestData.splitF_G1(), gorm1);
		FunctionDefinition g2 = (FunctionDefinition) c2.innerScope().get("g");
		HSIEForm gorm2 = HSIE.handle(g2, form.vars.size());
		gorm2.dump();
		HSIETestData.assertHSIE(HSIETestData.splitF_G2(), gorm2);
//		assertEquals(2, gorm.externals.size());
//		assertTrue(gorm.externals.contains("FLEval.mul"));
//		assertTrue(gorm.externals.contains("_scoped.x"));
//		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}
}
