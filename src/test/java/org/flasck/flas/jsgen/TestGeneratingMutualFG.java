package org.flasck.flas.jsgen;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestGeneratingMutualFG {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void testMFG() {
		HSIEForm f = HSIETestData.mutualF(context);
		HSIEForm g = HSIETestData.mutualG(context);
		new Generator(new JSTarget("ME")).generate(f);
		new Generator(new JSTarget("ME")).generate(g);
	}

	@Test
	public void testSplitMFG() {
		HSIEForm f = HSIETestData.splitF(context);
		HSIEForm g1 = HSIETestData.splitF_G1(context);
		HSIEForm g2 = HSIETestData.splitF_G2(context);
		new Generator(new JSTarget("ME")).generate(f);
		new Generator(new JSTarget("ME")).generate(g1);
		new Generator(new JSTarget("ME")).generate(g2);
	}

}
