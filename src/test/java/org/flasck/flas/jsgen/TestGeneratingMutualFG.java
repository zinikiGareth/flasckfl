package org.flasck.flas.jsgen;

import static org.junit.Assert.*;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class TestGeneratingMutualFG {

	@Test
	public void testMFG() {
		HSIEForm f = HSIETestData.mutualF();
		HSIEForm g = HSIETestData.mutualG();
		JSForm fnF = new Generator().generate(f);
		fnF.insert(new Generator().generate(g));
		System.out.println(fnF);
	}

	@Test
	public void testSplitMFG() {
		HSIEForm f = HSIETestData.splitF();
		HSIEForm g1 = HSIETestData.splitF_G1();
		HSIEForm g2 = HSIETestData.splitF_G2();
		JSForm fnF = new Generator().generate(f);
		fnF.insert(new Generator().generate(g1));
		fnF.insert(new Generator().generate(g2));
		System.out.println(fnF);
	}

}
