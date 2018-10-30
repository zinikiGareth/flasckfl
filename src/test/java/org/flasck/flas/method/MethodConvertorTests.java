package org.flasck.flas.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructDefn.StructType;
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
import org.flasck.flas.types.Type;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private final InputPosition posn = new InputPosition("test", 1, 1, null);
	private Scope orgFooScope;
	private Rewriter rewriter;
	private ErrorResult errors;
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
			RWFunctionDefinition doSend = new RWFunctionDefinition(FunctionName.function(posn, null, "doSend"), 1, false);
			doSend.setType(Type.function(posn, any, send));
			biscope.define("doSend", doSend);
		}
		orgFooScope = Scope.topScope("org.foo");
//		orgFooScope.define("doSend", "org.foo.doSend", new FunctionCaseDefn(posn, CodeType.FUNCTION, "org.foo.doSend", args, expr));
		{
			SolidName cn = new SolidName(new PackageName("org.foo"), "Contract1");
			ContractDecl contract1 = new ContractDecl(posn, posn, cn);
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "down", FunctionName.contractDecl(posn, cn, "bar"), new ArrayList<>());
			contract1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(posn, posn, posn, true, "up", FunctionName.contractDecl(posn, cn, "start"), new ArrayList<>());
			contract1.methods.add(m2);
			ContractMethodDecl m3 = new ContractMethodDecl(posn, posn, posn, true, "up", FunctionName.contractDecl(posn, cn, "request"), Arrays.asList(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			contract1.methods.add(m3);
			orgFooScope.define("Contract1", contract1);
		}
		{
			SolidName cn = new SolidName(new PackageName("org.foo"), "Service1");
			ContractDecl service1 = new ContractDecl(posn, posn, cn);
			ContractMethodDecl m0 = new ContractMethodDecl(posn, posn, posn, true, "up", FunctionName.contractDecl(posn, cn, "go"), new ArrayList<>());
			service1.methods.add(m0);
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "up", FunctionName.contractDecl(posn, cn, "request"), Arrays.asList(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			service1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(posn, posn, posn, true, "down", FunctionName.contractDecl(posn, cn, "respond"), Arrays.asList(new TypedPattern(posn, new TypeReference(posn, "String"), posn, "s")));
			service1.methods.add(m2);
			orgFooScope.define("Service1", service1);
		}
		{
			SolidName cn = new SolidName(new PackageName("org.foo"), "Handler1");
			ContractDecl handler1 = new ContractDecl(posn, posn, cn);
			ContractMethodDecl m1 = new ContractMethodDecl(posn, posn, posn, true, "down", FunctionName.contractDecl(posn, cn, "handle"), new ArrayList<>());
			handler1.methods.add(m1);
			orgFooScope.define("Handler1", handler1);
		}
		{
			StructDefn struct = new StructDefn(posn, StructType.STRUCT, "org.foo", "Thing", true);
			struct.addField(new StructField(posn, false, new TypeReference(posn, "String"), "x"));
			orgFooScope.define("Thing", struct);
		}
		
		{
			rewriter = new Rewriter(errors, null, biscope, null);
			cd = new CardDefinition(posn, posn, orgFooScope, new CardName(new PackageName("org.foo"), "Card"));
			cd.state = new StateDefinition(posn);
			cd.state.addField(new StructField(posn, false, new TypeReference(posn, "String"), "str"));
			{
				ce = new ContractImplements(posn, posn, "org.foo.Contract1", posn, "ce");
				ce.setRealName(new CSName(cd.cardName, "_C0"));
				cd.contracts.add(ce);
			}
			{
				se = new ContractService(posn, posn, "org.foo.Service1", posn, "se");
				se.setRealName(new CSName(cd.cardName, "_S0"));
				cd.services.add(se);
			}
			{
				HandlerName hn = new HandlerName(new CardName(new PackageName("org.foo"), "Card"), "MyHandler");
				he = new HandlerImplements(posn, posn, posn, hn, "org.foo.Handler1", true, Arrays.asList((Object)new TypedPattern(posn, new TypeReference(posn, "Thing"), posn, "stateArg"), (Object)new VarPattern(posn, "freeArg")));
				he.setRealName(hn);
				cd.handlers.add(he);
			}
		}
	}
	
	public void stage2(boolean checkRewritingErrors) throws Exception {
		rewriter.rewritePackageScope(null, null, "org.foo", orgFooScope);
		if (checkRewritingErrors)
			assertFalse(errors.singleString(), errors.hasErrors());
		convertor = new MethodConvertor(errors, rewriter);
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
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("Nil", c1.expr.toString());
	}

	@Test
	public void testWeCanHaveAnEventHandlerWithNoActions() throws Exception {
		defineEHMethod(cd.innerScope(), "bar");
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card.bar", func.uniqueName());
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
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#tcMessages (join Nil)) Nil)", c1.expr.toString());
	}

	/* ---- Tests of Assignment to a single slot ---- */
	@Test
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "fred")), new NumericLiteral(posn, "36", 2)));
		stage2(false);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("could not resolve name fred", errors.get(0).msg);
	}

	@Test // This doesn't really test the convertor anymore - we assume that TC checks Assign properly
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "str")), new NumericLiteral(posn, "36", 2)));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this._card \"str\" (#assertType String 36)) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCanAssignToACardMember() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "str")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this._card \"str\" (#assertType String \"hello\")) Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCanAssignToACardMember() throws Exception {
		defineEHMethod(cd.innerScope(), "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "str")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign this \"str\" (#assertType String \"hello\")) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCannotAssignToAContractVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "ce")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a contract var: ce", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAServiceVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "se")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a service var: se", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "bar")), new StringLiteral(posn, "hello")));
		stage2(false);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("could not resolve name bar", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFunction() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "map")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFreeLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "freeArg")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to untyped handler lambda: freeArg", errors.get(0).msg);
	}

	@Test
	public void testWeCannotDirectlyAssignToAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "stateArg")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign directly to an object", errors.get(0).msg);
	}

	/* ---- Tests of Assignment to a nested slot ---- */
	@Test
	public void testWeCannotAssignToAFieldOfAString() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "str"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot extract member 'x' of a non-struct: 'String'", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToAFieldInAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "stateArg"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card._C0.handle", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign HL[org.foo.Card.MyHandler.stateArg] \"x\" (#assertType String \"hello\")) Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCanAssignToAFieldInALocalStatefulVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "t"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		assertEquals("org.foo.Card.futz", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (Assign org.foo.Card.futz.t \"x\" (#assertType String \"hello\")) Nil)", c1.expr.toString());
	}

	@Test
	public void testAnEventHandlerCannotAssignToAnUntypedVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "ev"), new LocatedToken(posn, "x")), new StringLiteral(posn, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot use untyped argument as assign target: org.foo.Card.futz.ev", errors.get(0).msg);
	}

	// TODO: I think there's another case here where we can't assign to ev just because it's the event argument and therefore transient and you can't make it not transient
	
	@Test
	public void testWeCannotAssignToANonField() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(posn, Arrays.asList(new LocatedToken(posn, "stateArg"), new LocatedToken(posn, "y")), new StringLiteral(posn, "hello")));
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
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#send CardMember[org.foo.Card.ce].\"start\"[]) Nil)", c1.expr.toString());
	}

	@Test
	public void testWeCanSendAMessageToAServiceWithOneArg() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(posn, null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new UnresolvedOperator(new InputPosition("test", 1, 6, "<- ce.request 'hello'"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.request 'hello'"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 7, "<- ce.request 'hello'"), "request")), new StringLiteral(new InputPosition("test", 1, 14, "<- ce.request 'hello'"), "hello"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		RWFunctionDefinition func = CollectionUtils.any(functions.values());
		System.out.println(func);
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#send CardMember[org.foo.Card.ce].\"request\"[\"hello\"]) Nil)", c1.expr.toString());
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
		assertEquals("org.foo.Card._C0.bar", func.uniqueName());
		assertEquals(1, func.cases.size());
		RWFunctionCaseDefn c1 = func.cases.get(0);
		assertEquals("(Cons (#tcMessages (map doSend Nil)) Nil)", c1.expr.toString());
	}

	/* ---- Helper Methods ---- */
	protected void defineContractMethod(Implements on, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(FunctionName.contractMethod(posn, new CSName(new CardName(new PackageName("org.foo"), "Card"), "_C0"), name), new ArrayList<>());
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		cs.provideCaseName(-1);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		on.methods.add(cs);
	}

	protected void defineEHMethod(IScope s, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(FunctionName.eventMethod(posn, new CardName(new PackageName("org.foo"), "Card"), name), Arrays.asList((Object)new TypedPattern(posn, new TypeReference(posn, "Thing"), posn, "t"), (Object)new VarPattern(posn, "ev")));
		EventCaseDefn cs = new EventCaseDefn(posn, intro);
		cs.provideCaseName(-1);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		s.define(name, cs);
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
