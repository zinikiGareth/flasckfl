package org.flasck.flas.typechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;

public class TestBasicTypeChecking {

	@Test
	public void testWeCanTypecheckANumber() {
		TypeChecker tc = new TypeChecker();
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.simpleFn();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAVerySimpleLambda() {
		TypeChecker tc = new TypeChecker();
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		Object te = tc.checkHSIE(null, phi, gamma, HSIETestData.simpleFn());
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
		TypeChecker tc = new TypeChecker();
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		Object te = tc.checkHSIE(null, phi, gamma, HSIETestData.idFn());
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

	
	@Test
	public void testExternalPlus1HasExpectedType() {
		TypeChecker tc = new TypeChecker();
		tc.addExternal("plus1", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Number")));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.returnPlus1();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type);
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeExpr);
		assertTrue(rte.args.get(1) instanceof TypeExpr);
		assertEquals("Number", ((TypeExpr)rte.args.get(0)).type);
		assertEquals("Number", ((TypeExpr)rte.args.get(1)).type);
	}

	@Test
	public void testWeCanTypecheckSimpleFunctionApplication() {
		TypeChecker tc = new TypeChecker();
		tc.addExternal("plus1", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Number")));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.plus1Of1();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAFunctionApplicationWithTwoArguments() {
		TypeChecker tc = new TypeChecker();
		tc.addExternal("plus", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Number"))));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.plus2And2();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanUseIDTwiceWithDifferentInstationsOfItsSchematicVar() {
		TypeChecker tc = new TypeChecker();
		tc.addExternal("id", new TypeExpr("->", new TypeVar(0), new TypeVar(0)));
		tc.addExternal("decode", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Char")));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.idDecode();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		System.out.println(te);
		// The type should be Char
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Char", rte.type);
		assertTrue(rte.args.isEmpty());
	}
	
	@Test
	public void testWeCanCheckTwoFunctionsAtOnceBecauseTheyAreMutuallyRecursive() throws Exception {
		TypeChecker tc = new TypeChecker();
		tc.addExternal("+", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Number"))));
		tc.addExternal("-", new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("->", new TypeExpr("Number"), new TypeExpr("Number"))));
		Set<HSIEForm> set = new HashSet<HSIEForm>();
		set.add(HSIETestData.rdf1());
		set.add(HSIETestData.rdf2());
		tc.typecheck(set);
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		// Four things should now be defined: -, +, f, g
		assertEquals(4, tc.knowledge.size());
		{
			Object rdf1 = tc.knowledge.get("f");
			assertNotNull(rdf1);
			assertTrue(rdf1 instanceof TypeExpr);
			TypeExpr te = (TypeExpr)rdf1;
			assertEquals("->", te.type);
			assertEquals(2, te.args.size());
			Object te1 = te.args.get(0);
			assertTrue(te1 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te1).type);
			assertEquals(0, ((TypeExpr)te1).args.size());
			Object te2 = te.args.get(1);
			assertTrue(te2 instanceof TypeVar);
			assertEquals(new TypeVar(6), (TypeVar)te2);
		}
		{
			Object rdf2 = tc.knowledge.get("g");
			assertNotNull(rdf2);
			assertTrue(rdf2 instanceof TypeExpr);
			TypeExpr te = (TypeExpr)rdf2;
			assertEquals("->", te.type);
			assertEquals(2, te.args.size());
			Object te1 = te.args.get(0);
			assertTrue(te1 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te1).type);
			assertEquals(0, ((TypeExpr)te1).args.size());
			Object te2 = te.args.get(1);
			assertTrue(te2 instanceof TypeVar);
			assertEquals(new TypeVar(6), (TypeVar)te2);
		}
	}
}