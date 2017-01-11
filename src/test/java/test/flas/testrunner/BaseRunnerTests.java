package test.flas.testrunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.AssertFailed;
import org.flasck.flas.testrunner.HTMLMatcher;
import org.flasck.flas.testrunner.NotMatched;
import org.flasck.flas.testrunner.TestRunner;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;

public abstract class BaseRunnerTests {
	static final int X_VALUE = 420;
	static final int X_OTHER_VALUE = 520;
	private static final String HELLO_STRING = "hello, world";
	private static final String HELLO_CLICKED = "hello clicked";
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null, null);
	TypeChecker2 tc = new TypeChecker2(errors, rw);
	// TODO: defining bce here feels out of place and should be in the JVMRunnerTest
	// But it is "part of" the CompileResult.  Why?
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	CompileResult prior;
	FLASCompiler sc = new FLASCompiler();
	TestRunner runner;
	String pkg = "test.runner";
	Scope mainScope = Scope.topScope(pkg);
	Scope testScope;
	CardName cn = new CardName(new PackageName("test.runner"), "Card");
	String spkg = pkg + ".script";
	
	@Before
	public void setup() {
		Main.setLogLevels();
		mainScope.define("x", null);
		CardDefinition cd = new CardDefinition(loc, loc, mainScope, cn);
		{
			ContractImplements ctr = new ContractImplements(loc, loc, "SetState", null, null);
			ctr.methods.add(new MethodCaseDefn(new FunctionIntro(FunctionName.contractMethod(loc, new CSName(cn, "_C0"), "setOn"), new ArrayList<>())));
			cd.contracts.add(ctr);
		}
		{
			ContractImplements ctr = new ContractImplements(loc, loc, "Echo", null, null);
			ctr.methods.add(new MethodCaseDefn(new FunctionIntro(FunctionName.contractMethod(loc, new CSName(cn, "_C1"), "saySomething"), Arrays.asList(new TypedPattern(loc, new TypeReference(loc, "String"), loc, "s")))));
			cd.contracts.add(ctr);
		}
		mainScope.define("Card", cd);
		tc.define("test.runner.x", Type.function(loc, new PrimitiveType(loc, new SolidName(null, "Number"))));
		prior = new CompileResult(mainScope, bce, tc);
		testScope = Scope.topScope(spkg);
	}
	
	@Test
	public void testAssertDoesNotThrowIfXDoesIndeedEqualX() throws Exception {
		testScope.define("expr1", function("expr1", new UnresolvedVar(loc, "x")));
		testScope.define("value1", function("value1", new NumericLiteral(loc, Integer.toString(X_VALUE), -1)));
		prepareRunner();
		runner.assertCorrectValue(1);
	}

	@Test(expected=AssertFailed.class)
	public void testAssertThrowsIfXIsNotThePrescribedValue() throws Exception {
		testScope.define("expr1", function("expr1", new UnresolvedVar(loc, "x")));
		testScope.define("value1", function("value1", new NumericLiteral(loc, Integer.toString(X_OTHER_VALUE), -1)));
		prepareRunner();
		runner.assertCorrectValue(1);
	}

	@Test
	public void testRunnerDoesNotThrowIfTheContentsMatches() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Contents("hello, world"), "div>span");
	}

	@Test
	public void testRunnerDoesNotThrowIfTheElementMatches() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Element("<span id=\"card_1_1\" class=\"\" onclick=\"card_1:1\">hello, world</span>"), "div>span");
	}

	@Test(expected=NotMatched.class)
	public void testRunnerThrowsIfTheRequestedElementIsNotThere() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Contents("irrelevant"), "div#missing");
	}

	@Test(expected=NotMatched.class)
	public void testRunnerThrowsIfTheElementCountExpectsZeroButItIsThere() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Count("0"), "div>span");
	}

	@Test(expected=NotMatched.class)
	public void testRunnerThrowsIfThereAreNoClassesButSomeExpected() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Class("bright"), "div>span");
	}

	@Test
	public void testRunnerDoesNotThrowIfThereAreNoClassesAndNoneWereExpected() throws Exception {
		prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Class(""), "div>span");
	}
	
	// We cannot "directly" test that "send" happens.  There are two visible effects:
	//  * the state updates and produces a visible effect through the template
	//  * we get some kind of message out
	// Test both of these in turn
	@Test
	public void testSendCausesTheShowTagToLightUp() throws Exception {
		prepareRunner();
		String cardVar = "q";
		String contractName = "SetState";
		String methodName = "setOn";
		runner.createCardAs(cn, cardVar);
		runner.send(cardVar, contractName, methodName, null);
		runner.match(new HTMLMatcher.Class("show"), "div>span");
	}

	@Test
	public void testSendCanCauseAMessageToComeBack() throws Exception {
		testScope.define("arg1", function("arg1", new StringLiteral(loc, HELLO_STRING)));
		testScope.define("earg2", function("earg2", new StringLiteral(loc, HELLO_STRING)));
		prepareRunner();
		String cardVar = "q";
		String contractName = "Echo";
		String methodName = "saySomething";
		List<Integer> args = new ArrayList<Integer>();
		args.add(1);
		List<Integer> eargs = new ArrayList<Integer>();
		eargs.add(2);
		runner.createCardAs(cn, cardVar);
		runner.expect(cardVar, pkg+"."+contractName, "echoIt", eargs);
		runner.send(cardVar, contractName, methodName, args);
	}

	@Test
	public void testWeCanClickOnAnAreaAndCauseAMessageToComeBack() throws Exception {
		testScope.define("earg1", function("earg1", new StringLiteral(loc, HELLO_CLICKED)));
		prepareRunner();
		String cardVar = "q";
		String contractName = "Echo";
		List<Integer> eargs = new ArrayList<Integer>();
		eargs.add(1);
		runner.createCardAs(cn, cardVar);
		runner.expect(cardVar, pkg+"."+contractName, "echoIt", eargs);
		runner.click("div>span");
	}

	protected abstract void prepareRunner() throws IOException, ErrorResultException;
	
	protected FunctionCaseDefn function(String name, Object expr) {
		FunctionCaseDefn defn = new FunctionCaseDefn(FunctionName.function(loc, new PackageName("test.runner.script"), name), new ArrayList<>(), expr);
		defn.provideCaseName(0);
		return defn;
	}
}
