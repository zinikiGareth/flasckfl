package org.flasck.flas.typechecker;

public class TestBasicTypeChecking {
	/*
	static InputPosition posn = new InputPosition("test", 1, 1, null);
	ErrorResult errors = new ErrorResult();
	Type number = Type.builtin(posn, "Number");
	TypeChecker2 tc = new TypeChecker2(errors);

	@Before
	public void setup() {
		tc.addExternal("Number", number);
		tc.addExternal("*", Type.function(posn, number, number, number));
		tc.addExternal("+", Type.function(posn, number, number, number));
		tc.addExternal("-", Type.function(posn, number, number, number));
		tc.addExternal("==", Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "A"), Type.builtin(posn, "Boolean")));
		tc.addTypeDefn(new RWUnionTypeDefn(posn, false, "Any", new ArrayList<>()));
		Type varA = Type.polyvar(posn, "A");
		RWStructDefn nil = new RWStructDefn(posn, "Nil", false);
		tc.addStructDefn(nil);
		RWStructDefn cons = new RWStructDefn(posn, "Cons", false, varA); 
		cons.addField(new RWStructField(posn, false, varA, "head"));
		cons.addField(new RWStructField(posn, false, cons, "tail"));
		tc.addStructDefn(cons);
		RWUnionTypeDefn list = new RWUnionTypeDefn(posn, false, "List", CollectionUtils.listOf(varA));
		list.addCase(nil);
		list.addCase(cons);
		tc.addTypeDefn(list);
				
		tc.addExternal("Nil", Type.function(posn, nil));
		tc.addExternal("Cons", Type.function(posn, varA, list, list));
		LogManager.getLogger("TypeChecker").setLevel(Level.DEBUG);
	}

	@Test
	public void testWeCanTypecheckANumber() throws IOException {
		TypeState s = new TypeState(errors, tc);
		HSIEForm fn = HSIETestData.simpleFn();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAVerySimpleLambda() throws IOException {
		TypeState s = new TypeState(errors, tc);
		s.gamma = s.gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(null, 1)));
		Object te = tc.checkHSIE(s, HSIETestData.simpleFn());
		errors.showTo(new PrintWriter(System.out), 0);
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
	public void testWeCanTypecheckID() throws IOException {
		TypeState s = new TypeState(errors, tc);
		s.gamma = s.gamma.bind(new Var(0), new TypeScheme(null, new TypeVar(null, 1)));
		Object te = tc.checkHSIE(s, HSIETestData.idFn());
		errors.showTo(new PrintWriter(System.out), 0);
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
	public void testExternalPlus1HasExpectedType() throws IOException {
		tc.addExternal("plus1", Type.function(posn, number, number));
		TypeState s = new TypeState(errors, tc);
		HSIEForm fn = HSIETestData.returnPlus1();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		errors.showTo(new PrintWriter(System.out), 0);
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
	public void testWeCanTypecheckSimpleFunctionApplication() throws IOException {
		tc.addExternal("plus1", Type.function(posn, number, number));
		TypeState s = new TypeState(errors, tc);
		HSIEForm fn = HSIETestData.plus1Of1();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAFunctionApplicationWithTwoArguments() throws IOException {
		TypeState s = new TypeState(errors, tc);
		HSIEForm fn = HSIETestData.plus2And2();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertNotNull(te);
		// The type should be Number
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type.name());
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanUseIDTwiceWithDifferentInstationsOfItsSchematicVar() throws IOException {
		tc.addExternal("id", Type.function(posn, Type.polyvar(posn, "A"), Type.polyvar(posn, "A")));
		tc.addExternal("decode", Type.function(posn, number, Type.builtin(posn, "Char")));
		TypeState s = new TypeState(errors, tc);
		HSIEForm fn = HSIETestData.idDecode();
		Object te = tc.checkExpr(s, fn, fn.nestedCommands().get(0));
		errors.showTo(new PrintWriter(System.out), 0);
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
		tc.addTypeDefn(new RWUnionTypeDefn(posn, false, "Any", new ArrayList<>()));
		tc.typecheck(CollectionUtils.setOf(HSIETestData.rdf1(), HSIETestData.rdf2(3)));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertEquals(9, tc.knowledge.size());
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
		TypeState s = new TypeState(errors, tc);
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
		tc.typecheck(CollectionUtils.setOf(HSIETestData.fib()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		Object te = tc.knowledge.get("ME.fib");
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
		Object te = tc.knowledge.get("take");
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
		Object te = tc.knowledge.get("ME.take");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("Number->List[A]->List[A]", te.toString());
	}

	@Test
	@Ignore
	public void testWeCanCheckUnionTypes() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.unionType()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.singleString(), errors.hasErrors());
		Object te = tc.knowledge.get("ME.f");
		System.out.println(te);
		assertNotNull(te);
		assertTrue(te instanceof Type);
		assertEquals("List[Any]->Number", te.toString());
	}

	@Test
	public void testWeCanCheckASimpleNestedFunction() throws Exception {
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleG()));
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleF()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertEquals(9, tc.knowledge.size());
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
		{
			tc.typecheck(CollectionUtils.setOf(HSIETestData.mutualG()));
			errors.showTo(new PrintWriter(System.out), 0);
			assertFalse(errors.hasErrors());
		}
		{
			tc.typecheck(CollectionUtils.setOf(HSIETestData.mutualF()));
			errors.showTo(new PrintWriter(System.out), 0);
			assertFalse(errors.hasErrors());
		}
		assertEquals(9, tc.knowledge.size());
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
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleIf()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertEquals(8, tc.knowledge.size());
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
		tc.typecheck(CollectionUtils.setOf(HSIETestData.simpleIfElse()));
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		assertEquals(8, tc.knowledge.size());
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
		tc = new TypeChecker(errors);
		ImportPackage biscope = Builtin.builtins();
		FunctionParser p = new FunctionParser(new FLASStory.State(null, "ME", HSIEForm.CodeType.FUNCTION));
		FunctionCaseDefn f1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("f (Any a) = 42"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(f1);
		f1.provideCaseName("ME.f_0");
		Scope s = new Scope(null);
		s.define("f", "ME.f", f1);
		FunctionCaseDefn g1 = (FunctionCaseDefn) p.tryParsing(new Tokenizable("g x = f [ 42, 'hello']"));
		assertEquals(errors.singleString(), 0, errors.count());
		assertNotNull(g1);
		g1.provideCaseName("ME.g_0");
		s.define("g", "ME.g", g1);
		tc.addExternal("Number", (Type) biscope.get("Number"));
		tc.addExternal("String", (Type) biscope.get("String"));
		tc.addExternal("join", ((RWFunctionDefinition) biscope.get("join")).getType());
		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("Any"));
		tc.addStructDefn((RWStructDefn) biscope.get("Nil"));
		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("List"));
		tc.addStructDefn((RWStructDefn) biscope.get("Cons"));
		tc.addStructDefn((RWStructDefn) biscope.get("Assign"));
		tc.addStructDefn((RWStructDefn) biscope.get("Send"));
		Rewriter rewriter = new Rewriter(errors, null, biscope);
		rewriter.rewritePackageScope("ME", s);
		assertEquals(errors.singleString(), 0, errors.count());
		HSIE hsie = new HSIE(errors);
		
		{
			Orchard<RWFunctionDefinition> o1 = new Orchard<>();
			o1.addTree(rewriter.functions.get("ME.f"));
			hsie.createForms(o1);
			tc.typecheck(hsie.orchard(o1));
			assertEquals(errors.singleString(), 0, errors.count());
		}
		{
			Orchard<RWFunctionDefinition> o2 = new Orchard<>();
			o2.addTree(rewriter.functions.get("ME.g"));
			hsie.createForms(o2);
			tc.typecheck(hsie.orchard(o2));
			assertEquals(errors.singleString(), 0, errors.count());
		}
		assertEquals(5, tc.knowledge.size());
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
	*/
}