package test.flas.testrunner;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.flasck.flas.Main;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.newtypechecker.TypeChecker2;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.testrunner.UnitTestRunner;
import org.flasck.flas.types.Type;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.utils.MultiTextEmitter;

public class SimpleUnitTestRunnerTests {

	@BeforeClass
	public static void prepareLogs() {
		Main.setLogLevels();
	}
	
	@Test
	public void testItCanTestASimpleValue() throws Exception {
		File f = createFile("\tvalue x", "\t\t32");
		StringWriter sw = new StringWriter();
		FLASCompiler sc = new FLASCompiler();
		ErrorResult errors = new ErrorResult();
		Rewriter rw = new Rewriter(errors, null, null);
		TypeChecker2 tc = new TypeChecker2(errors, rw);
		InputPosition loc = new InputPosition("-", 1, 0, null);
		tc.define("test.golden.x", Type.function(loc, Type.builtin(loc, "Number")));
		ByteCodeEnvironment bce = new ByteCodeEnvironment();
		defineX(bce);
		bce.dumpAll(true);
		CompileResult prior = new CompileResult("test.golden", bce, tc);
		sc.includePrior(prior);
		UnitTestRunner r = new UnitTestRunner(new MultiTextEmitter(sw), sc, prior.bce, prior.getPackage(), f);
		r.run();
		assertEquals("PASSED:\tvalue x\n", sw.toString());
	}

	private void defineX(ByteCodeEnvironment bce) {
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS$x");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.golden.PACKAGEFUNCTIONS", "java.lang.Object", "x")).flush();;
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.golden.PACKAGEFUNCTIONS");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "x");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(32))).flush();;
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
