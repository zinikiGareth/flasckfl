package org.flasck.flas.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.flim.Builtin;
import org.flasck.flas.flim.ImportPackage;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.Implements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.RWUnionTypeDefn;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.junit.Test;
import org.slf4j.Logger;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private Scope orgFooScope;
	private Rewriter rewriter;
	private ErrorResult errors;
	private HSIE hsie;
	private TypeChecker tc;
	private MethodConvertor convertor;
	private Map<String, HSIEForm> functions = new HashMap<>(); 
	private CardDefinition cd;
	private ContractImplements ce;
	private ContractService se;
	private HandlerImplements he;

	public MethodConvertorTests() {
		errors = new ErrorResult();
		ImportPackage biscope = Builtin.builtinScope();
		RWUnionTypeDefn any = (RWUnionTypeDefn) biscope.get("Any");
		RWStructDefn send = (RWStructDefn) biscope.get("Send");
		orgFooScope = new Scope(null, null);
		orgFooScope.define("doSend", "org.foo.doSend", Type.function(null, any, send));
		{
			ContractDecl contract1 = new ContractDecl(null, null, "org.foo.Contract1");
			ContractMethodDecl m1 = new ContractMethodDecl(null, true, "down", "bar", new ArrayList<>());
			contract1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(null, true, "up", "start", new ArrayList<>());
			contract1.methods.add(m2);
			ContractMethodDecl m3 = new ContractMethodDecl(null, true, "up", "request", CollectionUtils.listOf(new TypedPattern(null, new TypeReference(null, "String"), null, "s")));
			contract1.methods.add(m3);
			orgFooScope.define("Contract1", contract1.name(), contract1);
		}
		{
			ContractDecl service1 = new ContractDecl(null, null, "org.foo.Service1");
			ContractMethodDecl m0 = new ContractMethodDecl(null, true, "up", "go", new ArrayList<>());
			service1.methods.add(m0);
			ContractMethodDecl m1 = new ContractMethodDecl(null, true, "up", "request", CollectionUtils.listOf(new TypedPattern(null, new TypeReference(null, "String"), null, "s")));
			service1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl(null, true, "down", "respond", CollectionUtils.listOf(new TypedPattern(null, new TypeReference(null, "String"), null, "s")));
			service1.methods.add(m2);
			orgFooScope.define("Service1", service1.name(), service1);
		}
		{
			ContractDecl handler1 = new ContractDecl(null, null, "org.foo.Handler1");
			ContractMethodDecl m1 = new ContractMethodDecl(null, true, "down", "handle", new ArrayList<>());
			handler1.methods.add(m1);
			orgFooScope.define("Handler1", handler1.name(), handler1);
		}
		{
			RWStructDefn struct = new RWStructDefn(null, "Thing", true);
			struct.addField(new RWStructField(null, false, (Type)biscope.get("String"), "x"));
			orgFooScope.define("Thing", struct.name(), struct);
		}
		
		{
			rewriter = new Rewriter(errors, null);
			cd = new CardDefinition(null, null, orgFooScope, "org.foo.Card");
			cd.state = new StateDefinition();
			cd.state.addField(new StructField(null, false, new TypeReference(null, "String"), "str", null));
			{
				ce = new ContractImplements(null, null, "org.foo.Contract1", null, "ce");
				cd.contracts.add(ce);
			}
			{
				se = new ContractService(null, null, "org.foo.Service1", null, "se");
				cd.services.add(se);
			}
			{
				he = new HandlerImplements(null, null, "org.foo.MyHandler", "org.foo.Handler1", true, CollectionUtils.listOf((Object)new TypedPattern(null, new TypeReference(null, "Thing"), null, "stateArg"), (Object)new VarPattern(null, "freeArg")));
				cd.handlers.add(he);
			}
		}
		
		hsie = new HSIE(errors, rewriter);
		tc = new TypeChecker(errors);
		tc.addExternal("String", (Type) biscope.get("String"));
		tc.addExternal("join", (Type) biscope.get("join"));
		tc.addExternal("map", (Type) biscope.get("map"));
		tc.addExternal("org.foo.doSend", (Type) orgFooScope.get("doSend"));
		tc.addTypeDefn(any);
		tc.addStructDefn((RWStructDefn) biscope.get("Nil"));
		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("List"));
		tc.addStructDefn((RWStructDefn) biscope.get("Cons"));
		tc.addStructDefn((RWStructDefn) biscope.get("Assign"));
		tc.addTypeDefn((RWUnionTypeDefn) biscope.get("Message"));
		tc.addStructDefn(send);
	}
	
	public void stage2(boolean checkErrors) throws Exception {
		rewriter.rewriteScope(rewriter.new RootContext(), orgFooScope);
		if (checkErrors)
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
		cd.contracts.add(new ContractImplements(null, null, "NoContract", null, "cf"));
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
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump((Logger)null);
	}

	@Test
	public void testWeCanHaveAnEventHandlerWithNoActions() throws Exception {
		defineEHMethod(orgFooScope, "bar");
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testWeCannotFathomANumericExpressionByItself() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new NumericLiteral(new InputPosition("test", 1, 3, "<- 36"), "36")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("not a valid method message", errors.get(0).msg);
		assertEquals("test:         1.3", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotFathomARandomFunctionNotAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- (join []) ''"), new ApplyExpr(new InputPosition("test", 1, 3, "<- (join []) ''"), new UnresolvedVar(null, "join"), new UnresolvedVar(null, "Nil")), new StringLiteral(null, ""))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
//		HSIEForm hsieForm = CollectionUtils.any(functions.values());
//		hsieForm.dump(null);
//		assertEquals(0, functions.size());
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("method expression must be of type Message or List[Message]", errors.get(0).msg);
		assertEquals("test:         1.3", errors.get(0).loc.toString());
	}

	/* ---- Tests of Assignment to a single slot ---- */
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "fred")), new NumericLiteral(null, "36")));
		stage2(true);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new NumericLiteral(null, "36")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot assign Number to slot of type String", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToACardMember() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1:clos1 [v0:clos0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump((Logger)null);
	}

	@Test
	public void testAnEventHandlerCanAssignToACardMember() throws Exception {
		defineEHMethod(cd.innerScope(), "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN v3:clos3 [v2:clos2]", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testWeCannotAssignToAContractVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "ce")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a contract var: ce", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAServiceVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "se")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a service var: se", errors.get(0).msg);
	}

	public void testWeCannotAssignToAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "bar")), new StringLiteral(null, "hello")));
		stage2(true);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFunction() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "map")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFreeLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "freeArg")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to untyped handler lambda: freeArg", errors.get(0).msg);
	}

	@Test
	public void testWeCannotDirectlyAssignToAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign directly to an object", errors.get(0).msg);
	}

	/* ---- Tests of Assignment to a nested slot ---- */
	@Test
	public void testWeCannotAssignToAFieldOfAString() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot extract member 'x' of a non-struct: 'String'", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToAFieldInAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1:clos1 [v0:clos0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump((Logger)null);
	}

	@Test
	public void testAnEventHandlerCanAssignToAFieldInALocalStatefulVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "t"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN v3:clos3 [v2:clos2]", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testAnEventHandlerCannotAssignToAnUntypedVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "ev"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertEventHandlers(rewriter, functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot use untyped argument as assign target: ev", errors.get(0).msg);
	}

	// TODO: I think there's another case here where we can't assign to ev just because it's the event argument and therefore transient and you can't make it not transient
	
	@Test
	public void testWeCannotAssignToANonField() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg"), new LocatedToken(null, "y")), new StringLiteral(null, "hello")));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no field 'y' in type Thing", errors.get(0).msg);
	}

	/* ---- Send tests ---- */
	@Test
	public void testWeCanSendAMessageToAServiceWithNoArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start"), new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(null, "start")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN v1:clos1 [v0:clos0]", hsieForm.nestedCommands().get(0).toString());
		assertEquals("PUSH Send", hsieForm.getClosure(new Var(0)).nestedCommands().get(0).toString());
		assertEquals("PUSH CardMember[org.foo.Card.ce]", hsieForm.getClosure(new Var(0)).nestedCommands().get(1).toString());
		assertEquals("PUSH \"start\"", hsieForm.getClosure(new Var(0)).nestedCommands().get(2).toString());
		assertEquals("PUSH Nil", hsieForm.getClosure(new Var(0)).nestedCommands().get(3).toString());
	}

	@Test
	public void testWeCanSendAMessageToAServiceWithOneArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request 'hello'"), new UnresolvedOperator(new InputPosition("test", 1, 6, "<- ce.request 'hello'"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.request 'hello'"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 7, "<- ce.request 'hello'"), "request")), new StringLiteral(new InputPosition("test", 1, 14, "<- ce.request 'hello'"), "hello"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN v2:clos2 [v0:clos0, v1:clos1]", hsieForm.nestedCommands().get(0).toString());
		List<HSIEBlock> clos1 = hsieForm.getClosure(new Var(1)).nestedCommands();
		assertEquals(4, clos1.size());
		assertEquals("PUSH Send", clos1.get(0).toString());
		assertEquals("PUSH CardMember[org.foo.Card.ce]", clos1.get(1).toString());
		assertEquals("PUSH \"request\"", clos1.get(2).toString());
		assertEquals("PUSH v0:clos0", clos1.get(3).toString());
	}

	@Test
	public void testWeCannotSendAMessageToAServiceWhichDoesNotHaveThatMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.unknown"), "unknown")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no method 'unknown' in org.foo.Contract1", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendADownMessageFromADownServiceHandler() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.bar"), "bar")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call up methods on contract implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendAnUpMessageFromAnUpServiceHandler() throws Exception {
		defineContractMethod(se, "go", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "se"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- se.request"), "request")))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call down methods on service implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendAMessageWithTooManyArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start 'hello'"), new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start"), new UnresolvedOperator(new InputPosition("test", 1, 6, "<- ce.start 'hello'"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.start 'hello'"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 7, "<- ce.start 'hello'"), "start")), new StringLiteral(new InputPosition("test", 1, 12, "<- ce.start 'hello'"), "hello"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		System.out.println(errors.singleString());
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals(errors.singleString(), "called with too many arguments", errors.get(0).msg);
		assertEquals("test:         1.12", errors.get(0).loc.toString());
	}
	
	@Test
	public void testSendRequiresTheMethodToHaveAllItsArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.request"), new UnresolvedOperator(new InputPosition("test", 1, 5, "<- ce.request"), "."), new UnresolvedVar(new InputPosition("test", 1, 5, "<- ce.request"), "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.request"), "request"))));
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
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- map doSend []"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "map"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "doSend"), new UnresolvedVar(new InputPosition("test", 1, 3, "<- map doSend []"), "Nil"))));
		stage2(true);
		convertor.convertContractMethods(rewriter, functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump((Logger)null);
		assertEquals("RETURN v1:clos1 [v0:clos0]", hsieForm.nestedCommands().get(0).toString());
		List<HSIEBlock> clos0 = hsieForm.getClosure(new Var(0)).nestedCommands();
		assertEquals("PUSH map", clos0.get(0).toString());
		assertEquals("PUSH org.foo.doSend", clos0.get(1).toString());
		assertEquals("PUSH Nil", clos0.get(2).toString());
	}

	/* ---- Helper Methods ---- */
	protected void defineContractMethod(Implements on, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0." + name, new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		cases.add(cs);
		MethodDefinition method = new MethodDefinition(intro, cases);
		on.methods.add(method);
	}

	protected void defineEHMethod(Scope s, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card." + name, CollectionUtils.listOf((Object)new TypedPattern(null, new TypeReference(null, "Thing"), null, "t"), (Object)new VarPattern(null, "ev")));
		List<EventCaseDefn> cases = new ArrayList<>();
		EventCaseDefn cs = new EventCaseDefn(null, intro);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		cases.add(cs);
		EventHandlerDefinition ev = new EventHandlerDefinition(intro, cases);
		s.define(name, intro.name, ev);
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
