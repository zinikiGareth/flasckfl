package org.flasck.flas.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private final InputPosition posn = new InputPosition("test", 1, 1, null);
	private Scope orgFooScope;
	private Rewriter rewriter;
	private ErrorResult errors;
	private HSIE hsie;
	private TypeChecker tc;
	private MethodConvertor convertor;
	private Map<String, RWFunctionDefinition> functions = new HashMap<>(); 
	private CardDefinition cd;
	private ContractImplements ce;
	private ContractService se;
	private HandlerImplements he;

	public MethodConvertorTests() {
		errors = new ErrorResult();
		ImportPackage biscope = Builtin.builtins();
		RWUnionTypeDefn any = (RWUnionTypeDefn) biscope.get("Any");
		RWStructDefn send = (RWStructDefn) biscope.get("Send");
		{
			RWFunctionDefinition doSend = new RWFunctionDefinition(posn, CodeType.FUNCTION, "doSend", 1, false);
			doSend.setType(Type.function(posn, any, send));
			biscope.define("doSend", doSend);
		}
		orgFooScope = new Scope(null);
//		orgFooScope.define("doSend", "org.foo.doSend", new FunctionCaseDefn(posn, CodeType.FUNCTION, "org.foo.doSend", args, expr));
		{
			ContractDecl contract1 = new ContractDecl(posn, posn, "org.foo.Contract1");
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "down", "bar", new ArrayList<>());
			contract1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(posn, posn, posn, true, "up", "start", new ArrayList<>());
			contract1.methods.add(m2);
			ContractMethodDecl m3 = new ContractMethodDecl(posn, posn, posn, true, "up", "request", CollectionUtils.listOf(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			contract1.methods.add(m3);
			orgFooScope.define("Contract1", contract1.name(), contract1);
		}
		{
			ContractDecl service1 = new ContractDecl(posn, posn, "org.foo.Service1");
			ContractMethodDecl m0 = new ContractMethodDecl(posn, posn, posn, true, "up", "go", new ArrayList<>());
			service1.methods.add(m0);
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "up", "request", CollectionUtils.listOf(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			service1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(posn, posn, posn, true, "down", "respond", CollectionUtils.listOf(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			service1.methods.add(m2);
			orgFooScope.define("Service1", service1.name(), service1);
		}
		{
			ContractDecl handler1 = new ContractDecl(posn, posn, "org.foo.Handler1");
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "down", "handle", new ArrayList<>());
			handler1.methods.add(m1);
			orgFooScope.define("Handler1", handler1.name(), handler1);
		}
		{
			StructDefn struct = new StructDefn(posn, "org.foo.Thing", true);
			struct.addField(new StructField(posn, false, new TypeReference(posn, "String"), "x"));
			orgFooScope.define("Thing", struct.name(), struct);
		}
		
		{
			rewriter = new Rewriter(errors, null, biscope);
			cd = new CardDefinition(posn, posn, orgFooScope, "org.foo.Card");
			cd.state = new StateDefinition(posn);
			cd.state.addField(new StructField(posn, false, new TypeReference(posn, "String"), "str"));
			{
				ce = new ContractImplements(posn, posn, "org.foo.Contract1", posn, "ce");
				cd.contracts.add(ce);
			}
			{
				se = new ContractService(posn, posn, "org.foo.Service1", posn, "se");
				cd.services.add(se);
			}
			{
				he = new HandlerImplements(posn, posn, posn, "org.foo.MyHandler", "org.foo.Handler1", true, CollectionUtils.listOf((Object)new TypedPattern(posn, new TypeReference(posn, "Thing"), posn, "stateArg"), (Object)new VarPattern(posn, "freeArg")));
				cd.handlers.add(he);
			}
		}
		
		hsie = new HSIE(errors, rewriter);
		tc = new TypeChecker(errors);
		// I don't know how broken this is, but I don't believe in any of it anymore
//		tc.addExternal("String", (Type) biscope.get("String"));
//		tc.addExternal("join", (Type) biscope.get("join"));
//		tc.addExternal("map", (Type) biscope.get("map"));
//		tc.addExternal("org.foo.doSend", (Type) orgFooScope.get("doSend"));
//		tc.addTypeDefn(any);
//		tc.addStructDefn((RWStructDefn) biscope.get("Nil"));
//		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("List"));
//		tc.addStructDefn((RWStructDefn) biscope.get("Cons"));
//		tc.addStructDefn((RWStructDefn) biscope.get("Assign"));
//		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("Message"));
//		tc.addStructDefn(send);
	}
	
	public void stage2(boolean checkRewritingErrors) throws Exception {
		rewriter.rewritePackageScope("org.foo", orgFooScope);
		if (checkRewritingErrors)
			assertFalse(errors.singleString(), errors.hasErrors());
		tc.populateTypes(rewriter);
		convertor = new MethodConvertor(errors, hsie, tc, rewriter.contracts);
	}

	/* ---- Trivial Tests of top level functionality ---- */
	@Test
	public void testWeCanConvertNothingToNothing() throws Exception {
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
	}

	@Test
	public void testTheImplementedContractMustExist() throws Exception {
		cd.contracts.add(new ContractImplements(posn, posn, "NoContract", posn, "cf"));
		stage2(false);
		assertEquals(1, errors.count());
		assertEquals("could not resolve name NoContract", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustExist() throws Exception {
		defineContractMethod(ce, "foo");
		stage2(false);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("contract 'org.foo.Contract1' does not have a method 'foo' to implement", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustBeInTheRightDirection() throws Exception {
		defineContractMethod(ce, "start");
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot implement 'start' because it is an up method", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedServiceMethodMustBeInTheRightDirection() throws Exception {
		defineContractMethod(se, "respond");
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot implement 'respond' because it is a down method", errors.get(0).msg);
	}

	@Test
	public void testWeCanHaveAMethodWithNoActions() throws Exception {
		defineContractMethod(ce, "bar");
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("Nil", c1.expr.toString());
	}

	@Test
	public void testWeCanHaveAnEventHandlerWithNoActions() throws Exception {
		defineEHMethod(orgFooScope, "bar");
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("Nil", c1.expr.toString());
	}

	@Test
	public void testWeCannotFathomANumericExpressionByItself() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new NumericLiteral(new InputPosition("test", 1, 3, "<- 36"), "36", 5)));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("not a valid method message", errors.get(0).msg);
		assertEquals("test:         1.3", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotFathomARandomFunctionNotAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- (join []) ''"), new ApplyExpr(new InputPosition("test", 1, 3, "<- (join []) ''"), new UnresolvedVar(posn, "join"), new UnresolvedVar(posn, "Nil")), new StringLiteral(posn, ""))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#tcMessages (join Nil)) Nil)", c1.expr.toString());
	}

	/* ---- Tests of Assignment to a single slot ---- */
	@Test
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "fred")), new NumericLiteral(posn, "36", 2)));
		stage2(false);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("could not resolve name fred", errors.get(0).msg);
	}

	@Test // This doesn't really test the convertor anymore - we assume that TC checks Assign properly
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "str")), new NumericLiteral(posn, "36", 2)));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this._card \"str\" 36) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCanAssignToACardMember() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "str")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this._card \"str\" \"hello\") Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCanAssignToACardMember() throws Exception {
		defineEHMethod(cd.innerScope(), "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "str")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this \"str\" \"hello\") Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCannotAssignToAContractVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "ce")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a contract var: ce", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAServiceVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "se")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a service var: se", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "bar")), new StringLiteral(posn, "hello")));
		stage2(false);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("could not resolve name bar", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFunction() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "map")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFreeLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "freeArg")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to untyped handler lambda: freeArg", errors.get(0).msg);
	}

	@Test
	public void testWeCannotDirectlyAssignToAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "stateArg")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign directly to an object", errors.get(0).msg);
	}

	/* ---- Tests of Assignment to a nested slot ---- */
	@Test
	public void testWeCannotAssignToAFieldOfAString() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "str"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot extract member 'x' of a non-struct: 'String'", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToAFieldInAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "stateArg"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.handle", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign HL[org.foo.MyHandler.stateArg] null \"hello\") Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCanAssignToAFieldInALocalStatefulVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "t"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card.futz", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign org.foo.Card.futz.t null \"hello\") Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCannotAssignToAnUntypedVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "ev"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot use untyped argument as assign target: org.foo.Card.futz.ev", errors.get(0).msg);
	}

	// TODO: I think there's another case here where we can't assign to ev just because it's the event argument and therefore transient and you can't make it not transient
	
	@Test
	public void testWeCannotAssignToANonField() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, CollectionUtils.listOf(new LocatedToken(posn, "stateArg"), new LocatedToken(posn, "y")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no field 'y' in type org.foo.Thing", errors.get(0).msg);
	}

	/* ---- Send tests ---- */
	@Test
	public void testWeCanSendAMessageToAServiceWithNoArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start"), new ApplyExpr(posn, new UnresolvedOperator(posn, "."), new UnresolvedVar(posn, "ce"), new UnresolvedVar(posn, "start")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Send CardMember[org.foo.Card.ce] \"start\" Nil) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCanSendAMessageToAServiceWithOneArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new UnresolvedOperator(new InputPosition("test", 1, 6, "<- ce.request 'hello'"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.request 'hello'"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 7, "<- ce.request 'hello'"), "request")), new StringLiteral(new InputPosition("test", 1, 14, "<- ce.request 'hello'"), "hello"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Send CardMember[org.foo.Card.ce] \"request\" (Cons \"hello\" Nil)) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCannotSendAMessageToAServiceWhichDoesNotHaveThatMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(posn, new ApplyExpr(posn, new UnresolvedOperator(posn, "."), new UnresolvedVar(posn, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.unknown"), "unknown")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no method 'unknown' in org.foo.Contract1", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendADownMessageFromADownServiceHandler() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(posn, new ApplyExpr(posn, new UnresolvedOperator(posn, "."), new UnresolvedVar(posn, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.bar"), "bar")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call up methods on contract implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendAnUpMessageFromAnUpServiceHandler() throws Exception {
		defineContractMethod(se, "go", new MethodMessage(posn, null, new ApplyExpr(posn, new ApplyExpr(posn, new UnresolvedOperator(posn, "."), new UnresolvedVar(posn, "se"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- se.request"), "request")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call down methods on service implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendAMessageWithTooManyArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start 'hello'"), new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start"), new UnresolvedOperator(new InputPosition("test", 1, 6, "<- ce.start 'hello'"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.start 'hello'"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.start 'hello'"), "start")), new StringLiteral(new InputPosition("test", 1, 12, "<- ce.start 'hello'"), "hello"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		System.out.println(errors.singleString());
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals(errors.singleString(), "too many arguments to start", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}
	
	@Test
	public void testSendRequiresTheMethodToHaveAllItsArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request"), new UnresolvedOperator(new InputPosition("test", 1, 5, "<- ce.request"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.request"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.request"), "request"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		System.out.println(errors.singleString());
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals(errors.singleString(), "missing arguments in call of request", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}
	
	/* ---- Function case tests ---- */
	@Test
	public void testWeCanSendACallMapAcrossSomethingThatReturnsSend() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- map doSend []"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "map"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "doSend"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "Nil"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card._C0.bar", func.name);
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#tcMessages (map doSend Nil)) Nil)", c1.expr.toString());
	}

	/* ---- Helper Methods ---- */
	protected void defineContractMethod(Implements on, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(posn, "org.foo.Card._C0." + name, new ArrayList<>());
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		cs.provideCaseName(intro.name);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		on.methods.add(cs);
	}

	protected void defineEHMethod(Scope s, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(posn, "org.foo.Card." + name, CollectionUtils.listOf((Object)new TypedPattern(posn, new TypeReference(posn, "Thing"), posn, "t"), (Object)new VarPattern(posn, "ev")));
		EventCaseDefn cs = new EventCaseDefn(posn, intro);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		s.define(name, intro.name, cs);
	}
	
	// the other cases are where it's just <- ...
	//   - it could be <Action> but unknown exactly what
	//   - it could be [Action], with a function/method return
	//   - we could be calling "map" or "filter" over other methods
	
	// Multiple different types of method context
	//  - contract
	//  - service
	//  - handler
	//  - event
	//  - standalone
}
