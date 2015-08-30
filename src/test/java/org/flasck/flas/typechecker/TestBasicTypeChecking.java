package org.flasck.flas.typechecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parser.ExprTester;
import org.flasck.flas.parser.Expression;
import org.flasck.flas.parser.FunctionParser;
import org.flasck.flas.parser.IntroParser;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;

public class TestBasicTypeChecking {
	ErrorResult errors = new ErrorResult();
	
	@Test
	public void testWeCanTypecheckANumber() {
		TypeChecker tc = new TypeChecker(errors);
		TypeState s = new TypeState(errors);
		HSIEForm fn = HSIETestData.simpleFn();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAVerySimpleLambda() {
		TypeChecker tc = new TypeChecker(errors);
		TypeState s = new TypeState(errors);
		s.gamma = s.gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(null, 1)));
		Object te = tc.checkHSIE(s, HSIETestData.simpleFn());
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be A -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type.name());
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeVar);
		assertEquals("Number", ((TypeExpr)rte.args.get(1)).type.name());
	}

	@Test
	public void testWeCanTypecheckID() {
		TypeChecker tc = new TypeChecker(errors);
		TypeState s = new TypeState(errors);
		s.gamma = s.gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(null, 1)));
		Object te = tc.checkHSIE(s, HSIETestData.idFn());
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be A -> A
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type.name());
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeVar);
		assertTrue(rte.args.get(1) instanceof TypeVar);
		assertEquals(rte.args.get(1), rte.args.get(0));
	}

	
	@Test
	public void testExternalPlus1HasExpectedType() {
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("plus1", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		TypeState s = new TypeState(errors);
		HSIEForm fn = HSIETestData.returnPlus1();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
//		assertEquals("Number->Number", rte.asType(tc).toString());
		assertEquals("->", rte.type.name());
		assertEquals(2, rte.args.size());
		assertTrue(rte.args.get(0) instanceof TypeExpr);
		assertTrue(rte.args.get(1) instanceof TypeExpr);
		assertEquals("Number", ((TypeExpr)rte.args.get(0)).type.name());
		assertEquals("Number", ((TypeExpr)rte.args.get(1)).type.name());
	}

	@Test
	public void testWeCanTypecheckSimpleFunctionApplication() {
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("plus1", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		TypeState s = new TypeState(errors);
		HSIEForm fn = HSIETestData.plus1Of1();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAFunctionApplicationWithTwoArguments() {
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("plus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		TypeState s = new TypeState(errors);
		HSIEForm fn = HSIETestData.plus2And2();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanUseIDTwiceWithDifferentInstationsOfItsSchematicVar() {
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("id", Type.function(null, Type.polyvar(null, "A"), Type.polyvar(null, "A")));
		tc.addExternal("decode", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Char")));
		TypeState s = new TypeState(errors);
		HSIEForm fn = HSIETestData.idDecode();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		System.out.println(te);
		// The type should be Char
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Char", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}
	
	@Test
	public void testWeCanCheckTwoFunctionsAtOnceBecauseTheyAreMutuallyRecursive() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("FLEval.plus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addExternal("FLEval.minus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addTypeDefn(new UnionTypeDefn(null, false, "Any"));
		tc.typecheck(orchardOf(HSIETestData.rdf1(), HSIETestData.rdf2()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		// Four things should now be defined: -, +, f, g
		assertEquals(4, tc.knowledge.size());
		{
			Object rdf1 = tc.knowledge.get("ME.f");
			assertNotNull(rdf1);
			System.out.println(rdf1);
			assertTrue(rdf1 instanceof Type);
			assertEquals("Number->Any", rdf1.toString());
		}
		{
			Object rdf2 = tc.knowledge.get("ME.g");
			assertNotNull(rdf2);
			assertTrue(rdf2 instanceof Type);
			assertEquals("Number->Any", rdf2.toString());
		}
	}

	@Test
	public void testWeCanUseSwitchToLimitId() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		TypeState s = new TypeState(errors);
		s.gamma = s.gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(null, 1)));
		Object te = tc.checkHSIE(s, HSIETestData.numberIdFn());
		System.out.println(te);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("->", rte.type.name());
		assertEquals(2, rte.args.size());
		{
			Object te1 = rte.args.get(0);
			assertTrue(te1 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te1).type.name());
			assertEquals(0, ((TypeExpr)te1).args.size());
		}
		{
			Object te2 = rte.args.get(1);
			assertTrue(te2 instanceof TypeExpr);
			assertEquals("Number", ((TypeExpr)te2).type.name());
			assertEquals(0, ((TypeExpr)te2).args.size());
		}
	}
	
	@Test
	public void testWeCanHandleConstantSwitching() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		tc.addExternal("FLEval.plus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addExternal("FLEval.minus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.typecheck(orchardOf(HSIETestData.fib()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Object te = tc.knowledge.get("fib");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Number
		assertTrue(te instanceof Type);
		assertEquals("Number->Number", te.toString());
	}

	@Test
	public void testWeCanHandleBindForCons() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addTypeDefn(new UnionTypeDefn(null, false, "Any"));
		tc.addStructDefn(new StructDefn(null, "Number", false));
		Type varA = Type.polyvar(null, "A");
		StructDefn nil = new StructDefn(null, "Nil", false);
		tc.addStructDefn(nil);
		StructDefn cons = new StructDefn(null, "Cons", false, varA); 
		cons.addField(new StructField(varA, "head"));
		cons.addField(new StructField(cons, "tail"));
		tc.addStructDefn(cons);
		UnionTypeDefn list = new UnionTypeDefn(null, false, "List", varA);
		list.addCase(nil);
		list.addCase(cons);
		tc.addTypeDefn(list);
				
		tc.addExternal("Nil", Type.function(null, nil));
		tc.addExternal("Cons", Type.function(null, varA, list, list));
		tc.addExternal("-", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.typecheck(orchardOf(HSIETestData.takeConsCase()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.knowledge.get("take");
		System.out.println(te);
		assertNotNull(te);
		// The type should be Number -> Cons -> List
		assertTrue(te instanceof Type);
		assertEquals("Number->Cons[A]->List[A]", te.toString());
	}

	
	@Test
	public void testWeCanDoASimpleUnionOfNilAndCons() throws Exception {
		TypeChecker tc = new TypeChecker(errors);

		Type number = Type.builtin(null, "Number");
		tc.addExternal("Number", number);
		Type varA = Type.polyvar(null, "A");
		StructDefn nil = new StructDefn(null, "Nil", false);
		tc.addStructDefn(nil);
		StructDefn cons = new StructDefn(null, "Cons", false, varA); 
		cons.addField(new StructField(varA, "head"));
		cons.addField(new StructField(cons, "tail"));
		tc.addStructDefn(cons);
		UnionTypeDefn list = new UnionTypeDefn(null, false, "List", varA);
		list.addCase(nil);
		list.addCase(cons);
		tc.addTypeDefn(list);
				
		tc.addExternal("Nil", Type.function(null, nil));
		tc.addExternal("Cons", Type.function(null, varA, list, list));

		tc.addExternal("FLEval.minus", Type.function(null, number, number, number));
		tc.typecheck(orchardOf(HSIETestData.take()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.knowledge.get("take");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("Number->List[A]->List[A]", te.toString());
	}

	@Test
	public void testWeCanCheckASimpleNestedFunction() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		tc.addExternal("FLEval.mul", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.typecheck(orchardOf(HSIETestData.simpleG()));
		tc.typecheck(orchardOf(HSIETestData.simpleF()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
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

	@Test
	public void testWeCanCheckANestedMutuallyRecursiveFunction() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		tc.addExternal("FLEval.mul", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		Orchard<HSIEForm> orchard = new Orchard<HSIEForm>();
		Tree<HSIEForm> tree = orchard.addTree(HSIETestData.mutualF());
		tree.addChild(tree.getRoot(), HSIETestData.mutualG());
		System.out.println(tree.getChildren(tree.getRoot()));
		tc.typecheck(orchard);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
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

	@Test
	public void testWeCanCheckSimpleIf() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		tc.addExternal("FLEval.mul", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addExternal("FLEval.compeq", Type.function(null, Type.polyvar(null, "A"), Type.polyvar(null, "A"), Type.builtin(null, "Boolean")));
		Orchard<HSIEForm> orchard = new Orchard<HSIEForm>();
		orchard.addTree(HSIETestData.simpleIf());
		tc.typecheck(orchard);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertEquals(3, tc.knowledge.size());
		System.out.println(tc.knowledge);
		{
			Object mf = tc.knowledge.get("ME.fact");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
	}

	@Test
	public void testWeCanCheckSimpleIfElse() throws Exception {
		TypeChecker tc = new TypeChecker(errors);
		tc.addStructDefn(new StructDefn(null, "Number", false));
		tc.addExternal("FLEval.mul", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addExternal("FLEval.minus", Type.function(null, Type.builtin(null, "Number"), Type.builtin(null, "Number"), Type.builtin(null, "Number")));
		tc.addExternal("FLEval.compeq", Type.function(null, Type.polyvar(null, "A"), Type.polyvar(null, "A"), Type.builtin(null, "Boolean")));
		Orchard<HSIEForm> orchard = new Orchard<HSIEForm>();
		orchard.addTree(HSIETestData.simpleIfElse());
		tc.typecheck(orchard);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		// Four things should now be defined: -, +, f, g
		assertEquals(4, tc.knowledge.size());
		System.out.println(tc.knowledge);
		{
			Object mf = tc.knowledge.get("ME.fact");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Number->Number", mf.toString());
		}
	}

	@Test
	public void testWeCanResolveAnyUnionIfCallingAFunctionWithAny() throws Exception {
		Scope biscope = Builtin.builtinScope();
		PackageDefn pkg = new PackageDefn(null, biscope, "ME");
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn f1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("f (Any a) = 42"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(f1);
		FunctionDefinition f = new FunctionDefinition(null, HSIEForm.CodeType.FUNCTION, f1.intro, CollectionUtils.listOf(f1));
		pkg.innerScope().define("f", "ME.f", f);
		FunctionCaseDefn g1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("g x = f [ 42, 'hello']"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(g1);
		FunctionDefinition g = new FunctionDefinition(null, HSIEForm.CodeType.FUNCTION, g1.intro, CollectionUtils.listOf(g1));
		pkg.innerScope().define("g", "ME.g", g);
		TypeChecker tc = new TypeChecker(errors);
		tc.addExternal("String", (Type) biscope.get("String"));
		tc.addExternal("join", (Type) biscope.get("join"));
		tc.addTypeDefn((UnionTypeDefn) biscope.get("Any"));
		tc.addStructDefn((StructDefn) biscope.get("Nil"));
		tc.addTypeDefn((UnionTypeDefn) biscope.get("List"));
		tc.addStructDefn((StructDefn) biscope.get("Cons"));
		tc.addStructDefn((StructDefn) biscope.get("Assign"));
		tc.addStructDefn((StructDefn) biscope.get("Send"));
		Orchard<HSIEForm> orchard = new Orchard<HSIEForm>();
		Rewriter rewriter = new Rewriter(errors, null);
		rewriter.rewrite(pkg.myEntry());
		assertEquals(errors.singleString(), 0, errors.count());
		HSIE hsie = new HSIE(errors, rewriter);
		tc.typecheck(orchardOf(hsie.handle(rewriter.functions.get("ME.f"))));
		assertEquals(errors.singleString(), 0, errors.count());
		tc.typecheck(orchardOf(hsie.handle(rewriter.functions.get("ME.g"))));
//		assertEquals(errors.singleString(), 0, errors.count());
		tc.typecheck(orchard);
		assertEquals(errors.singleString(), 0, errors.count());
//		// Four things should now be defined: -, +, f, g
		assertEquals(4, tc.knowledge.size());
		{
			Object mf = tc.knowledge.get("ME.f");
			assertNotNull(mf);
			assertTrue(mf instanceof Type);
			assertEquals("Any->Number", mf.toString());
		}
		{
			Object mg = tc.knowledge.get("ME.g");
			assertNotNull(mg);
			assertTrue(mg instanceof Type);
			assertEquals("Any->Number", mg.toString());
		}
	}

	private Orchard<HSIEForm> orchardOf(HSIEForm... hs) {
		Orchard<HSIEForm> ret = new Orchard<HSIEForm>();
		for (HSIEForm h : hs)
			ret.addTree(h);
		return ret;
	}
}