package test.flas.testrunner;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.junit.Before;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.golden.script", prior, testScope);
		JVMRunner jr = new JVMRunner(prior);
		jr.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		jr.prepareScript(sc, testScope);
		runner = jr;
	}
	
	@Before
	public void defineSupportingFunctions() {
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
				meth.returnObject(meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(X_VALUE))).flush();
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
}
