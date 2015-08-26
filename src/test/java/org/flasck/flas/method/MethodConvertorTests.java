package org.flasck.flas.method;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodInContext;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Ignore;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class MethodConvertorTests {
	private Scope scope;
	private ErrorResult errors;
	private HSIE hsie;
	private TypeChecker tc;
	private Map<String, ContractDecl> contracts = new HashMap<>();
	private MethodConvertor convertor;
	private Map<String, HSIEForm> functions = new HashMap<>(); 
	private List<MethodInContext> methods = new ArrayList<>();

	public MethodConvertorTests() {
		scope = Builtin.builtinScope();
		errors = new ErrorResult();
		hsie = new HSIE(errors);
		tc = new TypeChecker(errors);
		
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
		convertor.convert(functions, methods);
	}

	@Test
	public void testTheImplementedContractMustExist() {
		MethodDefinition method = null;
		MethodInContext mic = new MethodInContext(scope, null, "NoContract", "org.foo.Card._C0.foo", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convert(functions, methods);
		assertEquals(1, errors.count());
		assertEquals("cannot find contract NoContract", errors.get(0).msg);
	}

	@Test
	public void testTheImplementedMethodMustExist() {
		MethodDefinition method = null;
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.foo", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convert(functions, methods);
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
		convertor.convert(functions, methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN Nil", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

	@Test
	public void testWeCanConvertASimpleAssignment() throws Exception {
		FunctionIntro intro = new FunctionIntro(null, "org.foo.Card._C0.bar", new ArrayList<>());
		List<MethodCaseDefn> cases = new ArrayList<>();
		MethodCaseDefn cs = new MethodCaseDefn(intro);
		MethodMessage msg1 = new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "x")), new NumericLiteral(null, "36"));
		cs.messages.add(msg1);
		cases.add(cs);
		MethodDefinition method = new MethodDefinition(intro, cases);
		MethodInContext mic = new MethodInContext(scope, null, "org.foo.Contract1", "org.foo.Card._C0.bar", HSIEForm.Type.CONTRACT, method);
		methods.add(mic);
		convertor.convert(functions, methods);
		assertFalse(errors.singleString(), errors.hasErrors());
		assertEquals(1, functions.size());
		HSIEForm hsieForm = CollectionUtils.any(functions.values());
		assertEquals("RETURN v1 [v0]", hsieForm.nestedCommands().get(0).toString());
		hsieForm.dump();
	}

}
