package org.flasck.flas.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
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
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.ResolutionException;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private PackageDefn org;
	private PackageDefn pkg;
	private Scope orgFooScope;
	private Rewriter rewriter;
	private ErrorResult errors;
	private HSIE hsie;
	private TypeChecker tc;
	private Map<String, ContractDecl> contracts = new HashMap<>();
	private MethodConvertor convertor;
	private Map<String, HSIEForm> functions = new HashMap<>(); 
	private CardDefinition cd;
	private ContractImplements ce;
	private ContractService se;
	private HandlerImplements he;

	public MethodConvertorTests() {
		errors = new ErrorResult();
		Scope biscope = Builtin.builtinScope();
		org = new PackageDefn(null, biscope, "org");
		pkg = new PackageDefn(null, org.innerScope(), "foo");
		orgFooScope = pkg.innerScope();
		{
			ContractDecl contract1 = new ContractDecl(null, "org.foo.Contract1");
			ContractMethodDecl m1 = new ContractMethodDecl("down", "bar", new ArrayList<>());
			contract1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl("up", "start", new ArrayList<>());
			contract1.methods.add(m2);
			ContractMethodDecl m3 = new ContractMethodDecl("up", "request", CollectionUtils.listOf(new TypedPattern(null, "String", null, "s")));
			contract1.methods.add(m3);
			contracts.put(contract1.name(), contract1);
			orgFooScope.define("Contract1", contract1.name(), contract1);
		}
		{
			ContractDecl service1 = new ContractDecl(null, "org.foo.Service1");
			ContractMethodDecl m1 = new ContractMethodDecl("up", "request", CollectionUtils.listOf(new TypedPattern(null, "String", null, "s")));
			service1.methods.add(m1);
			ContractMethodDecl m2 = new ContractMethodDecl("down", "respond", CollectionUtils.listOf(new TypedPattern(null, "String", null, "s")));
			service1.methods.add(m2);
			contracts.put(service1.name(), service1);
			orgFooScope.define("Service1", service1.name(), service1);
		}
		{
			ContractDecl handler1 = new ContractDecl(null, "org.foo.Handler1");
			ContractMethodDecl m1 = new ContractMethodDecl("down", "handle", new ArrayList<>());
			handler1.methods.add(m1);
			contracts.put(handler1.name(), handler1);
			orgFooScope.define("Handler1", handler1.name(), handler1);
		}
		{
			StructDefn struct = new StructDefn(null, "Thing", true);
			struct.addField(new StructField(Type.reference(null, "String"), "x"));
			orgFooScope.define("Thing", struct.name(), struct);
		}
		
		{
			rewriter = new Rewriter(errors, null);
			cd = new CardDefinition(null, orgFooScope, "org.foo.Card");
			cd.state = new StateDefinition();
			cd.state.addField(new StructField(Type.reference(null, "String"), "str"));
			{
				ce = new ContractImplements(null, "org.foo.Contract1", null, "ce");
				cd.contracts.add(ce);
			}
			{
				se = new ContractService(null, "org.foo.Service1", null, "se");
				cd.services.add(se);
			}
			{
				he = new HandlerImplements(null, "org.foo.MyHandler", "org.foo.Handler1", CollectionUtils.listOf((Object)new TypedPattern(null, "Thing", null, "stateArg"), (Object)new VarPattern(null, "freeArg")));
				cd.handlers.add(he);
			}
		}
		
		hsie = new HSIE(errors);
		tc = new TypeChecker(errors);
		tc.addExternal("String", (Type) biscope.get("String"));
		tc.addExternal("Any", (Type) biscope.get("Any"));
		tc.addExternal("Nil", (Type) biscope.get("Nil"));
		tc.addExternal("List", (Type) biscope.get("List"));
		tc.addExternal("Cons", (Type) biscope.get("Cons"));
		tc.addExternal("join", (Type) biscope.get("join"));
		tc.addExternal("Assign", (Type) biscope.get("Assign"));
		tc.addExternal("Send", (Type) biscope.get("Send"));
	}
	
	public void stage2() {
		rewriter.rewrite(pkg.myEntry());
		tc.populateTypes(rewriter);
		convertor = new MethodConvertor(errors, hsie, tc, contracts);
	}

	/* ---- Trivial Tests of top level functionality ---- */
	@Test
	public void testWeCanConvertNothingToNothing() {
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
	}

	@Test
	public void testTheImplementedContractMustExist() {
		cd.contracts.add(new ContractImplements(null, "NoContract", null, "cf"));
		stage2();
		assertEquals(1, errors.count());
		assertEquals("could not resolve name NoContract", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustExist() {
		defineContractMethod(ce, "foo");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot find method foo in org.foo.Contract1", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustBeInTheRightDirection() {
		defineContractMethod(ce, "start");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot implement 'start' because it is an up method", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedServiceMethodMustBeInTheRightDirection() {
		defineContractMethod(se, "respond");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot implement 'respond' because it is a down method", errors.get(0).msg);
	}

	@Test
	public void testWeCanHaveAMethodWithNoActions() throws Exception {
		defineContractMethod(ce, "bar");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test
	public void testWeCanHaveAnEventHandlerWithNoActions() throws Exception {
		defineEHMethod(orgFooScope, "bar");
		stage2();
		convertor.convertEventHandlers(functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump();
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testWeCannotFathomANumericExpressionByItself() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new NumericLiteral(new InputPosition("test", 1, 3, "<- 36"), "36")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("not a valid method message", errors.get(0).msg);
		assertEquals("test:         1.3", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotFathomARandomFunctionNotAField() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- (join []) ''"), new ApplyExpr(null, new UnresolvedVar(null, "join"), new UnresolvedVar(null, "Nil")), new StringLiteral(null, ""))));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("not a valid method message", errors.get(0).msg);
		assertEquals("test:         1.3", errors.get(0).loc.toString());
	}

	/* ---- Tests of Assignment to a single slot ---- */
	@Test(expected=ResolutionException.class) // TODO: is it just that somebody else will catch this, or is this a bad pattern?  Should the rewriter catch this error and give me a proper message?
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "fred")), new NumericLiteral(null, "36")));
		stage2();
	}

	@Test
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new NumericLiteral(null, "36")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot assign Number to slot of type String", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToACardMember() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test
	public void testAnEventHandlerCanAssignToACardMember() throws Exception {
		defineEHMethod(cd.innerScope(), "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertEventHandlers(functions, rewriter.eventHandlers);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump();
		assertEquals("RETURN v3 [v2]", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testWeCannotAssignToAContractVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "ce")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a contract var: ce", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAServiceVar() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "se")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a service var: se", errors.get(0).msg);
	}

	@Test(expected=ResolutionException.class)
	public void testWeCannotAssignToAMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "bar")), new StringLiteral(null, "hello")));
		stage2();
	}

	@Test
	public void testWeCannotAssignToAFunction() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "map")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAFreeLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "freeArg")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to untyped handler lambda: freeArg", errors.get(0).msg);
	}

	@Test
	public void testWeCannotDirectlyAssignToAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign directly to an object", errors.get(0).msg);
	}

	/* ---- Tests of Assignment to a nested slot ---- */
	@Test
	public void testWeCannotAssignToAFieldOfAString() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot extract member of a non-struct: x", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToAFieldInAStructLambda() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test
	public void testAnEventHandlerCanAssignToAFieldInALocalStatefulVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "t"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertEventHandlers(functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump();
		assertEquals("RETURN v3 [v2]", hsieForm.nestedCommands().get(1).nestedCommands().get(0).toString());
	}

	@Test
	public void testAnEventHandlerCannotAssignToAnUntypedVar() throws Exception {
		defineEHMethod(cd.innerScope(), "futz", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "ev"), new LocatedToken(null, "x")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertEventHandlers(functions, rewriter.eventHandlers);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot use untyped argument as assign target: ev", errors.get(0).msg);
	}

	// TODO: I think there's another case here where we can't assign to ev just because it's the event argument and therefore transient and you can't make it not transient
	
	@Test
	public void testWeCannotAssignToANonField() throws Exception {
		defineContractMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg"), new LocatedToken(null, "y")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no field 'y' in type Thing", errors.get(0).msg);
	}

	/* ---- Send tests ---- */
	@Test
	public void testWeCanSendAMessageToAServiceWithNoArgs() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(new InputPosition("test", 1, 3, "<- ce.start"), new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(null, "start")))));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 0, errors.count());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		hsieForm.dump();
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		assertEquals("PUSH Send", hsieForm.getClosure(new Var(0)).nestedCommands().get(0).toString());
		assertEquals("PUSH CardMember[org.foo.Card.ce]", hsieForm.getClosure(new Var(0)).nestedCommands().get(1).toString());
		assertEquals("PUSH \"start\"", hsieForm.getClosure(new Var(0)).nestedCommands().get(2).toString());
		assertEquals("PUSH Nil", hsieForm.getClosure(new Var(0)).nestedCommands().get(3).toString());
	}

	@Test
	public void testWeCannotSendAMessageToAServiceWhichDoesNotHaveThatMethod() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.unknown"), "unknown")))));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("there is no method 'unknown' in org.foo.Contract1", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendADownMessageFromADownServiceHandler() throws Exception {
		defineContractMethod(ce, "bar", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "ce"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- ce.bar"), "bar")))));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call up methods on contract implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}

	@Test
	public void testWeCannotSendAnUpMessageFromAnUpServiceHandler() throws Exception {
		defineContractMethod(se, "request", new MethodMessage(null, new ApplyExpr(null, new ApplyExpr(null, new UnresolvedOperator(null, "."), new UnresolvedVar(null, "se"), new UnresolvedVar(new InputPosition("test", 1, 6, "<- se.request"), "request")))));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("can only call down methods on service implementations", errors.get(0).msg);
		assertEquals("test:         1.6", errors.get(0).loc.toString());
	}
	
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
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card." + name, CollectionUtils.listOf((Object)new TypedPattern(null, "Thing", null, "t"), (Object)new VarPattern(null, "ev")));
		List<EventCaseDefn> cases = new ArrayList<>();
		EventCaseDefn cs = new EventCaseDefn(intro);
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
