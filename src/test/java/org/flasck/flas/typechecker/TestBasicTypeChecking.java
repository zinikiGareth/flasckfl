package org.flasck.flas.typechecker;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class TestBasicTypeChecking {

	@Test
	public void testWeCanTypecheckANumber() {
		TypeChecker tc = new TypeChecker(new ArrayList<HSIEForm>());
		PhiSolution phi = new PhiSolution();
		TypeEnvironment gamma = new TypeEnvironment();
		Object te = tc.tcExpr(phi, gamma, HSIETestData.simpleFn().nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAVerySimpleLambda() {
		TypeChecker tc = new TypeChecker(new ArrayList<HSIEForm>());
		PhiSolution phi = new PhiSolution();
		TypeEnvironment gamma = new TypeEnvironment();
		Object te = tc.checkHSIE(phi, gamma, HSIETestData.simpleFn());
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be A -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type);
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeVar);
		assertEquals("Number", ((TypeExpr)rte.args.get(1)).type);
	}

	@Test
	public void testWeCanTypecheckID() {
		TypeChecker tc = new TypeChecker(new ArrayList<HSIEForm>());
		PhiSolution phi = new PhiSolution();
		TypeEnvironment gamma = new TypeEnvironment();
		Object te = tc.checkHSIE(phi, gamma, HSIETestData.idFn());
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be A -> A
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type);
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeVar);
		assertTrue(rte.args.get(1) instanceof TypeVar);
		assertEquals(rte.args.get(1), rte.args.get(0));
	}
}
