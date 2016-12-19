package test.flas.testrunner;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.junit.Before;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;

public class JVMRunnerTests extends BaseRunnerTests {
	
	protected void prepareRunner() throws IOException, ErrorResultException {
		sc.includePrior(prior);
		sc.createJVM("test.runner.script", prior, testScope);
		JVMRunner jr = new JVMRunner(prior);
		jr.considerResource(new File("/Users/gareth/Ziniki/ThirdParty/flasjvm/jvm/bin/classes"));
		jr.prepareScript(sc, testScope);
		runner = jr;
	}
	
	@Before
	public void defineSupportingFunctions() {
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.PACKAGEFUNCTIONS$x");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.runner.PACKAGEFUNCTIONS", "java.lang.Object", "x")).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.PACKAGEFUNCTIONS$id");
			GenericAnnotator ga = GenericAnnotator.newMethod(bcc, true, "eval");
			PendingVar args = ga.argument("[java.lang.Object", "args");
			ga.returns("java.lang.Object");
			MethodDefiner meth = ga.done();
			meth.returnObject(meth.callStatic("test.runner.PACKAGEFUNCTIONS", "java.lang.Object", "id", meth.arrayElt(args.getVar(), meth.intConst(0)))).flush();
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.PACKAGEFUNCTIONS");
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
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.SetState");
			bcc.superclass("org.flasck.jvm.ContractImpl");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.jvm.ContractImpl", "<init>").flush();
				ctor.returnVoid().flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$_C0");
			bcc.superclass("test.runner.SetState");
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument("test.runner.Card", "card");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "test.runner.SetState", "<init>").flush();
				ctor.assign(ctor.getField("_card"), card.getVar()).flush();
				ctor.returnVoid().flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$B1");
			bcc.superclass("org.flasck.jvm.areas.TextArea");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				ann.argument("test.runner.Card", "card");
				PendingVar parent = ann.argument("org.flasck.jvm.areas.CardArea", "_parent");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.jvm.areas.TextArea", "<init>", ctor.as(parent.getVar(), "org.flasck.jvm.areas.Area"), ctor.as(ctor.aNull(), "java.lang.String")).flush();
				ctor.callVirtual("void", ctor.myThis(), "_setText", ctor.stringConst("hello, world")).flush();
				ctor.returnVoid().flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card");
			bcc.inheritsField(true, Access.PROTECTED, new JavaType("org.flasck.jvm.Wrapper"), "_wrapper");
			bcc.inheritsField(true, Access.PROTECTED, new JavaType("org.flasck.jdk.display.JDKDisplay"), "_display");
			bcc.superclass("org.flasck.android.FlasckActivity");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.android.FlasckActivity", "<init>").flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "onCreate");
				// TODO: should this have preserved state?
//				PendingVar into = ann.argument("java.lang.String", "into");
				ann.returns(JavaType.void_);
				NewMethodDefiner meth = ann.done();
				meth.callSuper("void", "org.flasck.android.FlasckActivity", "onCreate").flush();
				meth.callVirtual("void", meth.myThis(), "registerContract", meth.stringConst("test.runner.SetState"), meth.as(meth.makeNew("test.runner.Card$_C0", meth.myThis()), "org.flasck.jvm.ContractImpl")).flush();
				meth.callSuper("void", "org.flasck.android.FlasckActivity", "ready").flush();
				meth.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "render");
				PendingVar into = ann.argument("java.lang.String", "into");
				ann.returns(JavaType.void_);
				NewMethodDefiner meth = ann.done();
				meth.makeNewVoid("test.runner.Card$B1", meth.myThis(), meth.makeNew("org.flasck.jvm.areas.CardArea", meth.getField("_wrapper"), meth.as(meth.getField("_display"), "org.flasck.jvm.display.DisplayEngine"), into.getVar())).flush();
				meth.returnVoid().flush();
			}
		}
	}
}
