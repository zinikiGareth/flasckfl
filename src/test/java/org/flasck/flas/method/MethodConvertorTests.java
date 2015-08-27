package org.flasck.flas.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
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
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.ResolutionException;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private Scope scope;
	private PackageDefn org;
	private PackageDefn pkg;
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
		scope = Builtin.builtinScope();
		org = new PackageDefn(null, scope, "org");
		pkg = new PackageDefn(null, org.innerScope(), "foo");
		Scope ps = pkg.innerScope();
		{
			ContractDecl contract1 = new ContractDecl(null, "org.foo.Contract1");
			ContractMethodDecl m1 = new ContractMethodDecl("down", "bar", new ArrayList<>());
			contract1.methods.add(m1);
			contracts.put(contract1.name(), contract1);
			ps.define("Contract1", contract1.name(), contract1);
		}
		{
			ContractDecl service1 = new ContractDecl(null, "org.foo.Service1");
			ContractMethodDecl m1 = new ContractMethodDecl("up", "request", new ArrayList<>());
			service1.methods.add(m1);
			contracts.put(service1.name(), service1);
			ps.define("Service1", service1.name(), service1);
		}
		{
			ContractDecl handler1 = new ContractDecl(null, "org.foo.Handler1");
			ContractMethodDecl m1 = new ContractMethodDecl("down", "handle", new ArrayList<>());
			handler1.methods.add(m1);
			contracts.put(handler1.name(), handler1);
			ps.define("Handler1", handler1.name(), handler1);
		}
		{
			StructDefn struct = new StructDefn(null, "Thing", true);
			struct.addField(new StructField(Type.reference(null, "String"), "x"));
			ps.define("Thing", struct.name(), struct);
		}
		
		{
			rewriter = new Rewriter(errors, null);
			cd = new CardDefinition(null, ps, "org.foo.Card");
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
	}
	
	public void stage2() {
		rewriter.rewrite(pkg.myEntry());
		tc.populateTypes(rewriter);
		convertor = new MethodConvertor(errors, hsie, tc, contracts);
	}

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
		defineMethod(ce, "foo");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot find method foo in org.foo.Contract1", errors.get(0).msg);
	}

	@Test
	public void testWeCanHaveAMethodWithNoActions() throws Exception {
		defineMethod(ce, "bar");
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test(expected=ResolutionException.class)
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "fred")), new NumericLiteral(null, "36")));
		stage2();
	}

	@Test
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new NumericLiteral(null, "36")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(1, errors.count());
		assertEquals("cannot assign Number to slot of type String", errors.get(0).msg);
	}

	@Test
	public void testWeCanAssignToACardMember() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test
	public void testWeCannotAssignToAContractVar() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "ce")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a contract var: ce", errors.get(0).msg);
	}

	@Test
	public void testWeCannotAssignToAServiceVar() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "se")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to a service var: se", errors.get(0).msg);
	}

	@Test(expected=ResolutionException.class)
	public void testWeCannotAssignToAMethod() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "bar")), new StringLiteral(null, "hello")));
		stage2();
	}

	@Test
	public void testWeCannotAssignToAFunction() throws Exception {
		defineMethod(ce, "bar", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "map")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign to non-state member: map", errors.get(0).msg);
	}

	@Test
	public void testWeCannotDirectlyAssignToAStructLambda() throws Exception {
		defineMethod(he, "handle", new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "stateArg")), new StringLiteral(null, "hello")));
		stage2();
		convertor.convertContractMethods(functions, rewriter.methods);
		assertEquals(errors.singleString(), 1, errors.count());
		assertEquals("cannot assign String to slot of type Thing", errors.get(0).msg);
	}

	protected void defineMethod(Implements on, String name, MethodMessage... msgs) {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0." + name, new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		for (MethodMessage m : msgs)
			cs.messages.add(m);
		cases.add(cs);
		MethodDefinition method = new MethodDefinition(intro, cases);
		on.methods.add(method);
	}

	// TODO 2: divide this up into multiple cases some of which should pass and some should fail
	//   - the var is a parameter or HL and CAN be assigned
	//   - the var is a parameter or HL and CANNOT be assigned
	//   - we can traverse a list of nested slots
	
	// the other cases are where it's just <- ...
	//   - it could be "Send"
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
