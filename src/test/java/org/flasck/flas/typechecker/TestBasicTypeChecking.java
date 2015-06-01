package org.flasck.flas.typechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.HashMap;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.junit.Test;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;

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
		gamma = gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(1)));
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
		gamma = gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(1)));
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
		tc.addExternal("plus1", Type.function(Type.simple("Number"), Type.simple("Number")));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		HSIEForm fn = HSIETestData.returnPlus1();
		Object te = tc.checkExpr(new HashMap<String,Object>(), phi, gamma, fn, fn.nestedCommands().get(0));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
//		assertEquals("Number->Number", rte.asType(tc).toString());
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
		tc.addExternal("plus1", Type.function(Type.simple("Number"), Type.simple("Number")));
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
		tc.addExternal("plus", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
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
		tc.addExternal("id", Type.function(Type.polyvar("A"), Type.polyvar("A")));
		tc.addExternal("decode", Type.function(Type.simple("Number"), Type.simple("Char")));
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
		tc.addExternal("+", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.addExternal("-", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.typecheck(orchardOf(HSIETestData.rdf1(), HSIETestData.rdf2()));
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		// Four things should now be defined: -, +, f, g
		assertEquals(4, tc.knowledge.size());
		{
			Object rdf1 = tc.knowledge.get("f");
			assertNotNull(rdf1);
			System.out.println(rdf1);
			assertTrue(rdf1 instanceof Type);
			assertEquals("Number->A", rdf1.toString());
		}
		{
			Object rdf2 = tc.knowledge.get("g");
			assertNotNull(rdf2);
			assertTrue(rdf2 instanceof Type);
			assertEquals("Number->A", rdf2.toString());
		}
	}

	@Test
	public void testWeCanUseSwitchToLimitId() throws Exception {
		TypeChecker tc = new TypeChecker();
		tc.addStructDefn(new StructDefn("Number"));
		PhiSolution phi = new PhiSolution(tc.errors);
		TypeEnvironment gamma = new TypeEnvironment();
		gamma = gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(1)));
		Object te = tc.checkHSIE(new HashMap<String,Object>(), phi, gamma, HSIETestData.numberIdFn());
		System.out.println(te);
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type);
		assertEquals(2, rte.args.size());
		{
			Object te1 = rte.args.get(0);
			assertTrue(te1 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te1).type);
			assertEquals(0, ((TypeExpr)te1).args.size());
		}
		{
			Object te2 = rte.args.get(1);
			assertTrue(te2 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te2).type);
			assertEquals(0, ((TypeExpr)te2).args.size());
		}
	}
	
	@Test
	public void testWeCanHandleConstantSwitching() throws Exception {
		TypeChecker tc = new TypeChecker();
		tc.addStructDefn(new StructDefn("Number"));
		tc.addExternal("+", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.addExternal("-", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.typecheck(orchardOf(HSIETestData.fib()));
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		Object te = tc.knowledge.get("fib");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof Type);
		assertEquals("Number->Number", te.toString());
	}

	@Test
	public void testWeCanHandleBindForCons() throws Exception {
		TypeChecker tc = new TypeChecker();
		tc.addStructDefn(new StructDefn("Number"));
		tc.addStructDefn(
				new StructDefn("Cons").add("A")
				.addField(new StructField(new TypeReference("A"), "head"))
				.addField(new StructField(new TypeReference("Cons").with(new TypeReference("A")), "tail")));
		tc.addExternal("Nil", Type.function(Type.simple("Nil")));
		tc.addExternal("Cons", Type.function(Type.polyvar("A"), Type.simple("List", Type.polyvar("A")), Type.simple("List", Type.polyvar("A"))));
		tc.addExternal("-", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.typecheck(orchardOf(HSIETestData.takeConsCase()));
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		Object te = tc.knowledge.get("take");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Cons -> List
		assertTrue(te instanceof Type);
//		assertEquals("Number->Cons->List[A]", te.toString());
		assertEquals("Number->Cons[A]->List[A]", te.toString());
	}

	
	@Test
	public void testWeCanDoASimpleUnionOfNilAndCons() throws Exception {
		TypeChecker tc = new TypeChecker();
		
		TypeReference list = new TypeReference("List").with(new TypeReference("A"));
		tc.addStructDefn(new StructDefn("Number"));
		tc.addStructDefn(new StructDefn("Nil"));
		tc.addStructDefn(
				new StructDefn("Cons").add("A")
				.addField(new StructField(new TypeReference("A"), "head"))
				.addField(new StructField(list, "tail")));
		TypeDefn listDefn = new TypeDefn(list);
		listDefn.addCase(new TypeReference("Nil"));
		listDefn.addCase(new TypeReference("Cons").with(new TypeReference("A")));
		tc.addTypeDefn(listDefn);
		
		tc.addExternal("Nil", Type.function(Type.simple("Nil")));
		tc.addExternal("Cons", Type.function(Type.polyvar("A"), Type.simple("List", Type.polyvar("A")), Type.simple("List", Type.polyvar("A"))));
		tc.addExternal("-", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		tc.typecheck(orchardOf(HSIETestData.take()));
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		Object te = tc.knowledge.get("take");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("Number->List[A]->List[A]", te.toString());
	}

	
	@Test
	public void testWeCanCheckANestedMutuallyRecursiveFunction() throws Exception {
		TypeChecker tc = new TypeChecker();
		tc.addStructDefn(new StructDefn("Number"));
		tc.addExternal("FLEval.mul", Type.function(Type.simple("Number"), Type.simple("Number"), Type.simple("Number")));
		Orchard<HSIEForm> orchard = new Orchard<HSIEForm>();
		Tree<HSIEForm> tree = orchard.addTree(HSIETestData.mutualF());
		tree.addChild(tree.getRoot(), HSIETestData.mutualG());
		System.out.println(tree.getChildren(tree.getRoot()));
		tc.typecheck(orchard);
		tc.errors.showTo(new PrintWriter(System.out));
		assertFalse(tc.errors.hasErrors());
		// Four things should now be defined: -, +, f, g
		assertEquals(3, tc.knowledge.size());
		System.out.println(tc.knowledge);
		{
			Object mf = tc.knowledge.get("ME.f");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
		{
			Object mg = tc.knowledge.get("ME.f_0.g");
			assertNotNull(mg);
			assertTrue(mg instanceof Type);
			assertEquals("Number->Number", mg.toString());
		}
	}

	private Orchard<HSIEForm> orchardOf(HSIEForm... hs) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (HSIEForm h : hs)
			ret.addTree(h);
		return ret;
	}
}