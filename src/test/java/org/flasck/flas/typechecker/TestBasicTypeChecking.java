package org.flasck.flas.typechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class TestBasicTypeChecking {
	static InputPosition posn = new InputPosition("test", 1, 1, null);
	ErrorResult errors = new ErrorResult();
	Type number = new PrimitiveType(posn, new SolidName(null, "Number"));
	Type charT = new PrimitiveType(posn, new SolidName(null, "Char"));
	Type booleanT = new PrimitiveType(posn, new SolidName(null, "Boolean"));
	Rewriter rw;
	TypeChecker2 tc;

	@Before
	public void setup() {
		LogManager.getLogger("TypeChecker").setLevel(Level.DEBUG);
		PolyVar varA = new PolyVar(posn, "A");
		ImportPackage pkg = new ImportPackage("");
		pkg.define("Number", number);
		pkg.define("Char", charT);
		pkg.define("Boolean", booleanT);
		fntype(pkg, "+", number, number, number);
		fntype(pkg, "-", number, number, number);
		fntype(pkg, "*", number, number, number);
		fntype(pkg, "==", varA, varA, booleanT);
		fntype(pkg, "plus1", number, number);
		fntype(pkg, "decode", number, charT);
		fntype(pkg, "id1", varA, varA);
		RWStructDefn nil = new RWStructDefn(posn, new SolidName(null, "Nil"), false);
		pkg.define("Nil", nil);
		RWStructDefn cons = new RWStructDefn(posn, new SolidName(null, "Cons"), false, varA); 
		cons.addField(new RWStructField(posn, false, varA, "head"));
		cons.addField(new RWStructField(posn, false, cons, "tail"));
		pkg.define("Cons", cons);
		RWUnionTypeDefn list = new RWUnionTypeDefn(posn, false, new SolidName(null, "List"), CollectionUtils.listOf(varA));
		list.addCase(nil);
		list.addCase(cons);
		pkg.define("List", list);
		rw = new Rewriter(errors, new ArrayList<>(), pkg);
		tc = new TypeChecker2(errors, rw);
		tc.populateTypes();
		/*
		tc.addExternal("==", Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.builtin(posn, "Boolean")));
		tc.addTypeDefn(new RWUnionTypeDefn(posn, false, "Any", new ArrayList<>()));
		 */
	}

	protected void fntype(ImportPackage pkg, String name, Type... types) {
		RWFunctionDefinition fn = new RWFunctionDefinition(FunctionName.function(posn, null, name), types.length-1, false);
		fn.setType(Type.function(posn, types));
		pkg.define(name, fn);
	}
	
	@Test
	public void testWeCanTypecheckASimpleFn() throws Exception {
		HSIEForm fn = HSIETestData.simpleFn();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("A->Number", ty.toString());
	}

	@Test
	public void testWeCanTypecheckID() throws Exception {
		HSIEForm fn = HSIETestData.idFn();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("A->A", ty.toString());
	}

	@Test
	public void testExternalPlus1HasExpectedType() throws Exception {
		HSIEForm fn = HSIETestData.returnPlus1();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("->(Number->Number)", ty.toString());
	}

	@Test
	public void testWeCanTypecheckSimpleFunctionApplication() throws Exception {
		HSIEForm fn = HSIETestData.plus1Of1();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("->Number", ty.toString());
	}

	@Test
	public void testWeCanTypecheckAFunctionApplicationWithTwoArguments() throws Exception {
		HSIEForm fn = HSIETestData.plus2And2();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("->Number", ty.toString());
	}

	@Test
	public void testWeCanUseIDTwiceWithDifferentInstationsOfItsSchematicVar() throws Exception {
		HSIEForm fn = HSIETestData.idDecode();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("->Char", ty.toString());
	}
	
	@Test
	public void testWeCanCheckTwoFunctionsAtOnceBecauseTheyAreMutuallyRecursive() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.rdf1(), HSIETestData.rdf2(3)));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		{
			Object rdf1 = tc.getExportedType("ME.f");
			assertNotNull(rdf1);
			System.out.println(rdf1);
			assertTrue(rdf1 instanceof Type);
			assertEquals("Number->A", rdf1.toString());
		}
		{
			Object rdf2 = tc.getExportedType("ME.g");
			assertNotNull(rdf2);
			assertTrue(rdf2 instanceof Type);
			assertEquals("Number->A", rdf2.toString());
		}
	}

	@Test
	public void testWeCanUseSwitchToLimitId() throws Exception {
		HSIEForm fn = HSIETestData.numberIdFn();
		tc.typecheck(CollectionUtils.setOf(fn));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Type ty = tc.getExportedType(fn.funcName.uniqueName());
		assertNotNull(ty);
		assertEquals("Number->Number", ty.toString());
	}
	
	@Test
	public void testWeCanHandleConstantSwitching() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.fib()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Object te = tc.getExportedType("ME.fib");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof Type);
		assertEquals("Number->Number", te.toString());
	}

	@Test
	public void testWeCanHandleBindForCons() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.takeConsCase()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.getExportedType("take");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Cons -> List
		assertTrue(te instanceof Type);
		assertEquals("Number->Cons[A]->List[A]", te.toString());
	}
	
	@Test
	public void testWeCanDoASimpleUnionOfNilAndCons() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.take()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.getExportedType("ME.take");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("Number->List[A]->List[A]", te.toString());
	}

	@Test
	public void testWeCanCheckUnionTypes() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.unionType()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.getExportedType("ME.f");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("List[A]->Number", te.toString());
	}
	@Test
	public void testWeCanCheckASimpleNestedFunction() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleG()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		{
			Object mg = tc.getExportedType("ME.f_0.g");
			assertNotNull(mg);
			assertTrue(mg instanceof Type);
			assertEquals("Number->Number", mg.toString());
		}
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleF()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		{
			Object mf = tc.getExportedType("ME.f");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
	}

	@Test
	public void testWeCanCheckANestedMutuallyRecursiveFunction() throws Exception {
		{
			tc.typecheck(CollectionUtils.setOf(HSIETestData.mutualG()));
			errors.showTo(new PrintWriter(System.out), 0);
			assertFalse(errors.hasErrors());
		}
		{
			Object mg = tc.getExportedType("ME.f_0.g");
			assertNotNull(mg);
			assertTrue(mg instanceof Type);
			assertEquals("Number->Number", mg.toString());
		}
		{
			tc.typecheck(CollectionUtils.setOf(HSIETestData.mutualF()));
			errors.showTo(new PrintWriter(System.out), 0);
			assertFalse(errors.hasErrors());
		}
		{
			Object mf = tc.getExportedType("ME.f");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->A", mf.toString());
		}
	}

	@Test
	public void testWeCanCheckSimpleIf() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleIf()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		{
			Object mf = tc.getExportedType("ME.fact");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
	}

	@Test
	public void testWeCanCheckSimpleIfElse() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleIfElse()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		{
			Object mf = tc.getExportedType("ME.fact");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
	}
	
	@Test
	@Ignore // I think this test *should* work, but we have eliminated Poly->Any promotion, which is how it used to work
	// I think it should work NOW because we should have a constraint on the input arg of f
	public void testWeCanResolveAnyUnionIfCallingAFunctionWithAny() throws Exception {
		ImportPackage biscope = Builtin.builtins();
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME"));
		FunctionCaseDefn f1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("f (Any a) = 42"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(f1);
		f1.provideCaseName(0);
		Scope s = Scope.topScope("ME");
		s.define("f", f1);
		FunctionCaseDefn g1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("g x = f [ 42, 'hello']"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(g1);
		g1.provideCaseName(0);
		s.define("g", g1);
		Rewriter rewriter = new Rewriter(errors, null, biscope);
		rewriter.rewritePackageScope(null, "ME", s);
		assertEquals(errors.singleString(), 0, errors.count());
		tc = new TypeChecker2(errors, rewriter);
		tc.populateTypes();
		HSIE hsie = new HSIE(errors, rewriter);
		
		{
			Set<RWFunctionDefinition> o1 = new HashSet<>();
			o1.add(rewriter.functions.get("ME.f"));
			hsie.createForms(o1);
			tc.typecheck(hsie.orchard(o1));
			assertEquals(errors.singleString(), 0, errors.count());
		}
		{
			Set<RWFunctionDefinition> o2 = new HashSet<>();
			o2.add(rewriter.functions.get("ME.g"));
			hsie.createForms(o2);
			tc.typecheck(hsie.orchard(o2));
			assertEquals(errors.singleString(), 0, errors.count());
		}
		{
			Object mf = tc.getExportedType("ME.f");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Any->Number", mf.toString());
		}
		{
			Object mg = tc.getExportedType("ME.g");
			assertNotNull(mg);
			assertTrue(mg instanceof Type);
			assertEquals("Any->Number", mg.toString());
		}
	}
}