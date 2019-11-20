package test.flas.testrunner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.Configuration;
import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.CommonTestRunner;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;

// So this is just supposed to be a test of the runner, so we assume that everything already exists.
// For Java, we just use "this" classloader and it's all good.
// For JS, we need to pull in some handwritten JS, but it doesn't need to look like compiled JS if we don't want it to
// For both, it just needs to be runtime-compatible.
public abstract class BaseRunnerTests {
	static final int X_VALUE = 420;
	static final int X_OTHER_VALUE = 520;
	private static final String HELLO_STRING = "hello, world";
	private static final String HELLO_CLICKED = "hello clicked";
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null);
	// TODO: defining bce here feels out of place and should be in the JVMRunnerTest
	// But it is "part of" the CompileResult.  Why?
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	FLASCompiler sc = new FLASCompiler(null, null, new PrintWriter(System.out));
	String pkg = "test.runner";
	Scope mainScope = Scope.topScope(pkg);
	Scope testScope;
	CardName cn = new CardName(new PackageName("test.runner"), "TestCard");
	String spkg = pkg + ".script";
	StringWriter sw = new StringWriter();
	private PrintWriter pw = new PrintWriter(sw);
	
	@Before
	public void setup() {
		Main.setLogLevels();
//		mainScope.define(errors, "x", null);
//		CardDefinition cd = new CardDefinition(loc, loc, cn);
//		{
//			ContractImplements ctr = new ContractImplements(loc, loc, "SetState", null, null);
//			ctr.methods.add(new MethodCaseDefn(new FunctionIntro(FunctionName.contractMethod(loc, new CSName(cn, "_C0"), "setOn"), new ArrayList<>())));
//			cd.contracts.add(ctr);
//		}
//		{
//			ContractImplements ctr = new ContractImplements(loc, loc, "Echo", null, null);
//			ctr.methods.add(new MethodCaseDefn(new FunctionIntro(FunctionName.contractMethod(loc, new CSName(cn, "_C1"), "saySomething"), Arrays.asList(new TypedPattern(loc, new TypeReference(loc, "String"), loc, "s")))));
//			cd.contracts.add(ctr);
//		}
//		mainScope.define(errors, "TestCard", cd);
//		tc.define("test.runner.x", Type.function(loc, new PrimitiveType(loc, new SolidName(null, "Number"))));
//		prior = new CompileResult(mainScope, bce, tc);
//		testScope = Scope.topScope(spkg);
	}
	
	@Test
	public void testAssertIsOKIfXDoesIndeedEqualX() throws Exception {
		Configuration config = null;
		Repository repository = new Repository();
		CommonTestRunner runner = prepareRunner(config, repository);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.flas.testrunner"), "samples");
		UnitTestName utn = new UnitTestName(utfn, 12);
		UnitTestCase utc = new UnitTestCase(utn, "hello");
		runner.runit(pw, utc);
		assertEquals(prefix() + " PASS hello\n", sw.toString());
	}

	@Test
	public void testAssertFailsIfXDoesNotEqualAGivenValue() throws Exception {
		Configuration config = null;
		Repository repository = new Repository();
		CommonTestRunner runner = prepareRunner(config, repository);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.flas.testrunner"), "samples");
		UnitTestName utn = new UnitTestName(utfn, 18);
		UnitTestCase utc = new UnitTestCase(utn, "itfails");
		runner.runit(pw, utc);
		assertEquals(prefix() + " FAIL itfails\n  expected: 42\n  actual:   84\n", sw.toString());
	}

	@Test
	public void testAClosureIsFullyEvaluatedBeforeTheComparisonIsDone() throws Exception {
		Configuration config = null;
		Repository repository = new Repository();
		CommonTestRunner runner = prepareRunner(config, repository);
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("test.flas.testrunner"), "samples");
		UnitTestName utn = new UnitTestName(utfn, 25);
		UnitTestCase utc = new UnitTestCase(utn, "closures are expanded");
		runner.runit(pw, utc);
		assertEquals(prefix() + " PASS closures are expanded\n", sw.toString());
	}

	/*
	@Test(expected=AssertFailed.class)
	@Ignore
	public void testAssertThrowsIfXIsNotThePrescribedValue() throws Exception {
		testScope.define(errors, "expr1", function("expr1", new UnresolvedVar(loc, "x")));
		testScope.define(errors, "value1", function("value1", new NumericLiteral(loc, Integer.toString(X_OTHER_VALUE), -1)));
		CommonTestRunner runner = prepareRunner();
		runner.assertCorrectValue(1);
	}

	@Test
	@Ignore
	public void testRunnerDoesNotThrowIfTheContentsMatches() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Contents("hello, world"), "div>span:nth-of-type(1)");
	}

	@Test
	@Ignore
	public void testRunnerDoesNotThrowIfTheElementMatches() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Element("/<span id=\"card_.*\" class=\"\" onclick=\".*\">hello, world</span>/"), "div>span:nth-of-type(1)");
		// these spaces are bogus
//		runner.match(new HTMLMatcher.Element("/.*        /"), "div>span:nth-of-type(1)");
	}

	@Test(expected=NotMatched.class)
	@Ignore
	public void testRunnerThrowsIfTheRequestedElementIsNotThere() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Contents("irrelevant"), "div#missing");
	}

	@Test(expected=NotMatched.class)
	@Ignore
	public void testRunnerThrowsIfTheElementCountExpectsZeroButItIsThere() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Count("0"), "div>span:nth-of-type(1)");
	}

	@Test(expected=NotMatched.class)
	@Ignore
	public void testRunnerThrowsIfThereAreNoClassesButSomeExpected() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Class("bright"), "div>span:nth-of-type(1)");
	}

	@Test
	@Ignore
	public void testRunnerDoesNotThrowIfThereAreNoClassesAndNoneWereExpected() throws Exception {
		CommonTestRunner runner = prepareRunner();
		runner.createCardAs(cn, "q");
		runner.match(new HTMLMatcher.Class(""), "div>span:nth-of-type(1)");
	}
	
	// We cannot "directly" test that "send" happens.  There are two visible effects:
	//  * the state updates and produces a visible effect through the template
	//  * we get some kind of message out
	// Test both of these in turn
	@Test
	@Ignore
	public void testSendCausesTheShowTagToLightUp() throws Exception {
		CommonTestRunner runner = prepareRunner();
		String cardVar = "q";
		String contractName = "SetState";
		String methodName = "setOn";
		runner.createCardAs(cn, cardVar);
		runner.send(new IdempotentHandler() {}, cardVar, contractName, methodName, null);
		runner.match(new HTMLMatcher.Class("show"), "div>span:nth-of-type(1)");
	}

	@Test
	@Ignore
	public void testSendCanCauseAMessageToComeBack() throws Exception {
		testScope.define(errors, "arg1", function("arg1", new StringLiteral(loc, HELLO_STRING)));
		testScope.define(errors, "earg2", function("earg2", new StringLiteral(loc, HELLO_STRING)));
		CommonTestRunner runner = prepareRunner();
		String cardVar = "q";
		String contractName = "Echo";
		String methodName = "saySomething";
		List<Integer> args = new ArrayList<Integer>();
		args.add(1);
		List<Integer> eargs = new ArrayList<Integer>();
		eargs.add(2);
		runner.createCardAs(cn, cardVar);
		runner.expect(cardVar, pkg+"."+contractName, "echoIt", eargs);
		runner.send(new IdempotentHandler() {}, cardVar, contractName, methodName, args);
	}

	@Test
	@Ignore
	public void testWeCanClickOnAnAreaAndCauseAMessageToComeBack() throws Exception {
		testScope.define(errors, "earg1", function("earg1", new StringLiteral(loc, HELLO_CLICKED)));
		CommonTestRunner runner = prepareRunner();
		String cardVar = "q";
		String contractName = "Echo";
		List<Integer> eargs = new ArrayList<Integer>();
		eargs.add(1);
		runner.createCardAs(cn, cardVar);
		runner.expect(cardVar, pkg+"."+contractName, "echoIt", eargs);
		runner.click("div>span:nth-of-type(1)");
	}
	*/

	protected abstract String prefix();
	protected abstract CommonTestRunner prepareRunner(Configuration config, Repository repository) throws IOException, ErrorResultException;
	
//	protected FunctionCaseDefn function(String name, Object expr) {
//		FunctionCaseDefn defn = new FunctionCaseDefn(FunctionName.function(loc, new PackageName("test.runner.script"), name), new ArrayList<>(), expr);
////		defn.provideCaseName(0);
//		return defn;
//	}
}
