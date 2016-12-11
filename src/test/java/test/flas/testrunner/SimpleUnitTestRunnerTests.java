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
import org.zinutils.utils.LinePrinter;
import org.zinutils.utils.MultiTextEmitter;

public class SimpleUnitTestRunnerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition loc = new InputPosition("-", 1, 0, null);
	FLASCompiler sc = new FLASCompiler();
	ErrorResult errors = new ErrorResult();
	Rewriter rw = new Rewriter(errors, null, null);
	TypeChecker2 tc = new TypeChecker2(errors, rw);
	ByteCodeEnvironment bce = new ByteCodeEnvironment();
	CompileResult prior;
	// This is the wrong thing to mock.  We should have a "results" interface
	LinePrinter writer = context.mock(LinePrinter.class);
	
	@BeforeClass
	public static void prepareLogs() {
		Main.setLogLevels();
	}

	class Setup {
		Setup() {
			System.out.println("hello");
		}
		
	}

	private void go(Setup setup) {
		defineSupportingFunctions(bce);
		bce.dumpAll(true);
		Scope scope = new Scope(null);
		scope.define("x", "x", null);
		prior = new CompileResult("test.golden", scope, bce, tc);
		sc.includePrior(prior);
	}
	
	@Test
	public void testItCanTestASimpleValue() throws Exception {
		go(new Setup() {{
			tc.define("test.golden.x", Type.function(loc, Type.builtin(loc, "Number")));
		}});
		context.checking(new Expectations() {{
			oneOf(writer).print("PASSED");
			oneOf(writer).println(":\tvalue x");
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert x", "\t\t\t32");
	}

	@Test
	public void testItCanTestATrivialFunctionCall() throws Exception {
		go(new Setup() {{
			Type varA = Type.polyvar(loc, "A");
			tc.define("test.golden.id", Type.function(loc, varA, varA));
		}});
		context.checking(new Expectations() {{
			oneOf(writer).print("PASSED");
			oneOf(writer).println(":\tvalue x");
		}});

		runTestScript("\ttest a simple test\n", "\t\tassert (id 'hello')", "\t\t\t'hello'");
	}

	private void runTestScript(String... lines) throws IOException {
		File f = createFile(lines);
		UnitTestRunner r = new UnitTestRunner(new MultiTextEmitter(writer), sc, prior, f);
		r.run();
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
