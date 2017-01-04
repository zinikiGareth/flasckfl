package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.AssertFailed;
import org.flasck.flas.testrunner.TestRunner;
import org.flasck.flas.testrunner.UnitTestResultHandler;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class SimpleUnitTestRunnerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	FLASCompiler sc = new FLASCompiler();
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null, null);
	TypeChecker2 tc = new TypeChecker2(errors, rw);
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	CompileResult prior;
	private UnitTestResultHandler resultHandler = context.mock(UnitTestResultHandler.class);
	private TestRunner runner = context.mock(TestRunner.class);
	
	@BeforeClass
	public static void prepareLogs() {
		Main.setLogLevels();
	}

	class Setup {
		Scope scope = Scope.topScope("test.golden.script");
	}

	private void go(Setup setup) {
		prior = new CompileResult(setup.scope, bce, tc);
		sc.includePrior(prior);
	}
	
	@Test
	public void testItCanTestASimpleValue() throws Exception {
		go(new Setup() {{
			scope.define("x", null);
			tc.define("test.golden.x", Type.function(loc, new PrimitiveType(loc, new StructName(null, "Number"))));
		}});
		context.checking(new Expectations() {{
			oneOf(runner).prepareScript(with(any(FLASCompiler.class)), with(any(Scope.class)));
			oneOf(runner).prepareCase();
			oneOf(runner).assertCorrectValue(1);
			oneOf(resultHandler).testPassed("a simple test");
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert x", "\t\t\t32");
	}

	@Test
	public void testItCanTestATrivialFunctionCall() throws Exception {
		go(new Setup() {{
			scope.define("id", null);
			Type varA = new PolyVar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(runner).prepareScript(with(any(FLASCompiler.class)), with(any(Scope.class)));
			oneOf(runner).prepareCase();
			oneOf(runner).assertCorrectValue(1);
			oneOf(resultHandler).testPassed("a test of id");
		}});

		runTestScript("\ttest a test of id\n", "\t\tassert (id 'hello')", "\t\t\t'hello'");
	}

	@Test
	public void testItCanTestTwoCases() throws Exception {
		go(new Setup() {{
			scope.define("id", null);
			Type varA = new PolyVar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(runner).prepareScript(with(any(FLASCompiler.class)), with(any(Scope.class)));
			exactly(2).of(runner).prepareCase();
			oneOf(runner).assertCorrectValue(1);
			oneOf(runner).assertCorrectValue(2);
			oneOf(resultHandler).testPassed("test id with a string");
			oneOf(resultHandler).testPassed("test id with a number");
		}});

		runTestScript(
			"\ttest test id with a string\n", "\t\tassert (id 'hello')", "\t\t\t'hello'",
			"\ttest test id with a number\n", "\t\tassert (id 420)", "\t\t\t420"
		);
	}

	@Test
	public void testItFailsWhenGivenTheWrongValue() throws Exception {
		go(new Setup() {{
			scope.define("x", null);
			tc.define("test.golden.x", Type.function(loc, new PrimitiveType(loc, new StructName(null, "Number"))));
		}});
		context.checking(new Expectations() {{
			oneOf(runner).prepareScript(with(any(FLASCompiler.class)), with(any(Scope.class)));
			oneOf(runner).prepareCase();
			oneOf(runner).assertCorrectValue(1); will(throwException(new AssertFailed(420, 32)));
			oneOf(resultHandler).testFailed("a simple test", 420, 32);
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert x", "\t\t\t420");
	}

	@Test
	public void testIfRunningTwoCasesOneCanPassWhileTheOtherFails() throws Exception {
		go(new Setup() {{
			scope.define("id", null);
			Type varA = new PolyVar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(runner).prepareScript(with(any(FLASCompiler.class)), with(any(Scope.class)));
			exactly(2).of(runner).prepareCase();
			oneOf(runner).assertCorrectValue(1);
			oneOf(runner).assertCorrectValue(2); will(throwException(new AssertFailed(420, 32)));
			oneOf(resultHandler).testPassed("test id with a string");
			oneOf(resultHandler).testFailed("test id with a number", 420, 32);
		}});

		runTestScript(
			"\ttest test id with a string\n", "\t\tassert (id 'hello')", "\t\t\t'hello'",
			"\ttest test id with a number\n", "\t\tassert (id 32)", "\t\t\t420"
		);
	}

	private void runTestScript(String... lines) throws Exception {
		File f = createFile(lines);
		UnitTestRunner r = new UnitTestRunner(errors, sc, prior);
		r.sendResultsTo(resultHandler);
		r.run(f, runner);
	}

	private File createFile(String... lines) throws IOException {
		File ret = File.createTempFile("testFor", ".ut");
		PrintWriter pw = new PrintWriter(ret);
		for (String s : lines)
			pw.println(s);
		pw.close();
		ret.deleteOnExit();
		return ret;
	}
}
