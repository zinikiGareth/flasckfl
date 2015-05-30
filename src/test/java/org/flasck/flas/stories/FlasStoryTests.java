package org.flasck.flas.stories;

import static org.junit.Assert.*;

import org.flasck.flas.Rewriter;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
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
		assertEquals("ME.f", f.innerScope().resolve("f"));
		assertEquals("ME.f.g", f.innerScope().resolve("g"));
		System.out.println(f.cases.get(0));
		System.out.println(f.cases.get(0).expr);
		assertEquals(1, f.innerScope().size());
		HSIEForm form = HSIE.handle(f);
		HSIETestData.assertHSIE(HSIETestData.mutualF(), form);
		FunctionDefinition g = (FunctionDefinition) f.innerScope().get("g");
		HSIEForm gorm = HSIE.handle(g);
		assertEquals(2, gorm.externals.size());
		assertTrue(gorm.externals.contains("FLEval.mul"));
		assertTrue(gorm.externals.contains("_scoped.x"));
		HSIETestData.assertHSIE(HSIETestData.mutualG(), gorm);
	}

}
