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
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.rewriter.ResolutionException;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewriter.Rewriter.CardContext;
import org.flasck.flas.rewriter.Rewriter.PackageContext;
import org.flasck.flas.rewriter.Rewriter.RootContext;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private Scope scope;
	private PackageDefn pkg;
	private Rewriter rewriter;
	private CardContext cx;
	private ErrorResult errors;
	private HSIE hsie;
	private TypeChecker tc;
	private Map<String, ContractDecl> contracts = new HashMap<>();
	private MethodConvertor convertor;
	private Map<String, HSIEForm> functions = new HashMap<>(); 
	private List<MethodInContext> methods = new ArrayList<>();

	public MethodConvertorTests() {
		scope = Builtin.builtinScope();
		pkg = new PackageDefn(null, scope, "org.foo");
		{
			rewriter = new Rewriter(errors, null);
			CardDefinition cd = new CardDefinition(null, pkg.innerScope(), "org.foo.Card");
			cd.state = new StateDefinition();
			cd.state.addField(new StructField(Type.reference(null, "String"), "str"));
			RootContext rx = rewriter.new RootContext(scope);
			PackageContext px = rewriter.new PackageContext(rx, pkg);
			cx = rewriter.new CardContext(px, cd);
			rewriter.rewrite(pkg.myEntry());
		}
		
		errors = new ErrorResult();
		hsie = new HSIE(errors);
		tc = new TypeChecker(errors);
		tc.populateTypes(rewriter);
		
		{
			ContractDecl contract1 = new ContractDecl(null, "org.foo.Contract1");
			ContractMethodDecl m1 = new ContractMethodDecl("down", "bar", new ArrayList<>());
			contract1.methods.add(m1);
			contracts.put(contract1.name(), contract1);
		}
		
		convertor = new MethodConvertor(errors, hsie, tc, contracts);
	}
	
	@Test
	public void testWeCanConvertNothingToNothing() {
		convertor.convertContractMethods(functions, methods);
	}

	@Test
	public void testTheImplementedContractMustExist() {
		MethodDefinition method = null;
		MethodInContext mic = new MethodInContext(scope, null, "NoContract", "org.foo.Card._C0.foo", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convertContractMethods(functions, methods);
		assertEquals(1, errors.count());
		assertEquals("cannot find contract NoContract", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustExist() {
		MethodDefinition method = null;
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.foo", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convertContractMethods(functions, methods);
		assertEquals(1, errors.count());
		assertEquals("cannot find method foo in org.foo.Contract1", errors.get(0).msg);
	}

	@Test
	public void testWeCanHaveAMethodWithNoActions() throws Exception {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0.bar", new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		cases.add(new MethodCaseDefn(intro));
		MethodDefinition method = new MethodDefinition(intro, cases);
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.bar", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convertContractMethods(functions, methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test(expected=ResolutionException.class)
	public void testTheTopLevelSlotInAnAssignmentMustBeResolvable() throws Exception {
		MethodMessage msg1 = new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "fred")), new NumericLiteral(null, "36"));
		rewriter.rewrite(cx, msg1);
	}

	@Test
	public void testWeCanOnlyAssignASlotWithTheRightType() throws Exception {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0.bar", new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		MethodMessage msg1 = new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new NumericLiteral(null, "36"));
		cs.messages.add(rewriter.rewrite(cx, msg1));
		cases.add(cs);
		MethodDefinition method = new MethodDefinition(intro, cases);
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.bar", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convertContractMethods(functions, methods);
		assertEquals(1, errors.count());
		assertEquals("cannot assign Number to slot of type String", errors.get(0).msg);
	}

	// TODO 2: divide this up into multiple cases some of which should pass and some should fail
	//   - the var cannot be found
	//   - the var is a parameter
	//   - (DENY explicit stateful keyword) the var is a parameter specifically marked with "stateful" (and all that that implies, which probably needs picking up in some other set of tests)
	//   - the var is a state member
	//   - the var is a contract var
	//   - the var is a function name
	//   - the var is of the wrong type
	
	// Then comes the issue of having multiple things with dots between them
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
	@Test
	public void testWeCanConvertASimpleAssignment() throws Exception {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0.bar", new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		MethodMessage msg1 = new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "str")), new StringLiteral(null, "hello"));

		cs.messages.add(rewriter.rewrite(cx, msg1));
		cases.add(cs);
		MethodDefinition method = new MethodDefinition(intro, cases);
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.bar", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convertContractMethods(functions, methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

}
