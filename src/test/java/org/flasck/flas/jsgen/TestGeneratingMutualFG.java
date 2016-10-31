package org.flasck.flas.jsgen;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class TestGeneratingMutualFG {

	@Test
	public void testMFG() {
		HSIEForm f = HSIETestData.mutualF();
		HSIEForm g = HSIETestData.mutualG();
		new Generator(new JSTarget("ME")).generate(f);
		new Generator(new JSTarget("ME")).generate(g);
	}

	@Test
	public void testSplitMFG() {
		HSIEForm f = HSIETestData.splitF();
		HSIEForm g1 = HSIETestData.splitF_G1();
		HSIEForm g2 = HSIETestData.splitF_G2();
		new Generator(new JSTarget("ME")).generate(f);
		new Generator(new JSTarget("ME")).generate(g1);
		new Generator(new JSTarget("ME")).generate(g2);
	}

}
