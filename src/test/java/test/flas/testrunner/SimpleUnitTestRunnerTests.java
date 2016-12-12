package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.UnitTestResultHandler;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.types.Type;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

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
	
	@BeforeClass
	public static void prepareLogs() {
		Main.setLogLevels();
	}

	class Setup {
		Scope scope = new Scope(null);
		Setup() {
		}
		
	}

	private void go(Setup setup) {
		defineSupportingFunctions(bce);
		bce.dumpAll(true);
		prior = new CompileResult("test.golden", setup.scope, bce, tc);
		sc.includePrior(prior);
	}
	
	@Test
	public void testItCanTestASimpleValue() throws Exception {
		go(new Setup() {{
			scope.define("x", "x", null);
			tc.define("test.golden.x", Type.function(loc, Type.builtin(loc, "Number")));
		}});
		context.checking(new Expectations() {{
			oneOf(resultHandler).testPassed("a simple test");
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert x", "\t\t\t32");
	}

	@Test
	public void testItCanTestATrivialFunctionCall() throws Exception {
		go(new Setup() {{
			scope.define("id", "id", null);
			Type varA = Type.polyvar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(resultHandler).testPassed("a test of id");
		}});

		runTestScript("\ttest a test of id\n", "\t\tassert (id 'hello')", "\t\t\t'hello'");
	}

	@Test
	public void testItCanTestTwoCases() throws Exception {
		go(new Setup() {{
			scope.define("id", "id", null);
			Type varA = Type.polyvar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
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
			scope.define("x", "x", null);
			tc.define("test.golden.x", Type.function(loc, Type.builtin(loc, "Number")));
		}});
		context.checking(new Expectations() {{
			oneOf(resultHandler).testFailed("a simple test", 420, 32);
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert x", "\t\t\t420");
	}

	@Test
	public void testIfRunningTwoCasesOneCanPassWhileTheOtherFails() throws Exception {
		go(new Setup() {{
			scope.define("id", "id", null);
			Type varA = Type.polyvar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(resultHandler).testPassed("test id with a string");
			oneOf(resultHandler).testFailed("test id with a number", 32, 420);
		}});

		runTestScript(
			"\ttest test id with a string\n", "\t\tassert (id 'hello')", "\t\t\t'hello'",
			"\ttest test id with a number\n", "\t\tassert (id 420)", "\t\t\t32"
		);
	}


	private void runTestScript(String... lines) throws Exception {
		File f = createFile(lines);
		UnitTestRunner r = new UnitTestRunner(sc, prior);
		r.sendResultsTo(resultHandler);
		r.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		r.run(f);
	}

	private void defineSupportingFunctions(ByteCodeEnvironment bce) {
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS$x");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.golden.PACKAGEFUNCTIONS", "java.lang.Object", "x")).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS$id");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar args = ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.golden.PACKAGEFUNCTIONS", "java.lang.Object", "id", meth.arrayElt(args.getVar(), meth.intConst(0)))).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS");
			{
				GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "x");
				ga.returns("java.lang.Object");
				MethodDefiner meth = ga.done();
				meth.returnObject(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(32))).flush();
			}
			{
				GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "id");
				PendingVar val = ga.argument("java.lang.Object", "val");
				ga.returns("java.lang.Object");
				MethodDefiner meth = ga.done();
				meth.returnObject(val.getVar()).flush();
			}
		}
		System.out.println(bce.all());
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
