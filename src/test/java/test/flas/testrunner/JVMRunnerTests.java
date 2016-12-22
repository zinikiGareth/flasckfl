package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.flasck.jvm.ContractImpl;
import org.flasck.jvm.Wrapper;
import org.flasck.jvm.cards.CardDespatcher;
import org.flasck.jvm.cards.FlasckCard;
import org.flasck.jvm.display.DisplayEngine;
import org.junit.Before;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Remarker;
import org.zinutils.bytecode.StackMapFrame;
import org.zinutils.bytecode.Var;

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
			bcc.makeAbstract();
			bcc.superclass("org.flasck.jvm.ContractImpl");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.jvm.ContractImpl", "<init>").flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "setOff");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				ann.returns(JavaType.object_);
				MethodDefiner meth = ann.done();
				meth.setAccess(Access.PUBLICABSTRACT);
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "setOn");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				ann.returns(JavaType.object_);
				MethodDefiner meth = ann.done();
				meth.setAccess(Access.PUBLICABSTRACT);
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
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "setOff");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos1 = meth.avar("org.flasck.jvm.FLClosure", "clos1");
				meth.assign(clos1, meth.makeNew("org.flasck.jvm.FLClosure",
										meth.classConst("org.flasck.jvm.builtin.Assign"),
										meth.arrayOf("java.lang.Object", Arrays.asList(meth.getField("_card"), meth.stringConst("sayHello"), meth.as(meth.makeNew("java.lang.Boolean", meth.boolConst(false)), "java.lang.Object"))))).flush();
				meth.returnObject(meth.makeNew("org.flasck.jvm.FLClosure", 
										meth.classConst("org.flasck.jvm.builtin.Cons"),
										meth.arrayOf("java.lang.Object", Arrays.asList(clos1, meth.callStatic("org.flasck.jvm.builtin.Nil", "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<>())))))).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "setOn");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos1 = meth.avar("org.flasck.jvm.FLClosure", "clos1");
				meth.assign(clos1, meth.makeNew("org.flasck.jvm.FLClosure",
										meth.classConst("org.flasck.jvm.builtin.Assign"),
										meth.arrayOf("java.lang.Object", Arrays.asList(meth.getField("_card"), meth.stringConst("sayHello"), meth.as(meth.makeNew("java.lang.Boolean", meth.boolConst(true)), "java.lang.Object"))))).flush();
				meth.returnObject(meth.makeNew("org.flasck.jvm.FLClosure", 
										meth.classConst("org.flasck.jvm.builtin.Cons"),
										meth.arrayOf("java.lang.Object", Arrays.asList(clos1, meth.callStatic("org.flasck.jvm.builtin.Nil", "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<>())))))).flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card");
			String sup = FlasckCard.class.getName();
			bcc.superclass(sup);
			bcc.inheritsField(true, Access.PROTECTED, new JavaType(Wrapper.class.getName()), "_wrapper");
			bcc.inheritsField(true, Access.PROTECTED, new JavaType(DisplayEngine.class.getName()), "_display");
			bcc.defineField(false, Access.PROTECTED, JavaType.boolean_, "sayHello");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar despatcher = ann.argument(CardDespatcher.class.getName(), "despatcher");
				PendingVar display = ann.argument(DisplayEngine.class.getName(), "display");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", sup, "<init>", despatcher.getVar(), display.getVar()).flush();
                ctor.callVirtual("void", ctor.myThis(), "registerContract", ctor.stringConst("test.runner.SetState"), ctor.as(ctor.makeNew("test.runner.Card$_C0", ctor.myThis()), ContractImpl.class.getName())).flush();
                ctor.callSuper("void", sup, "ready").flush();
                ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "render");
				PendingVar into = ann.argument("java.lang.String", "into");
				ann.returns(JavaType.void_);
				NewMethodDefiner meth = ann.done();
				meth.makeNewVoid("test.runner.Card$B1", meth.myThis(), meth.makeNew("org.flasck.jvm.areas.CardArea", meth.getField("_wrapper"), meth.getField("_display"), into.getVar())).flush();
				meth.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "styleIf");
				PendingVar str = ann.argument("java.lang.Object", "str");
				PendingVar bool = ann.argument("java.lang.Object", "bool");
				ann.returns("java.lang.Object");
				NewMethodDefiner meth = ann.done();
				Remarker m1 = new Remarker();
				meth.ifBoolean(meth.unbox(meth.castTo(bool.getVar(), "java.lang.Boolean"), false), meth.returnObject(str.getVar()), meth.block(meth.markHere(m1), meth.returnObject(meth.stringConst("")))).flush();
				meth.addStackMapFrame(StackMapFrame.SAME_FRAME, m1.getMarker());
			}
			bcc.writeTo(new File("/Users/gareth/bcc.class"));
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$B1");
			bcc.superclass("org.flasck.jvm.areas.TextArea");
			bcc.inheritsField(true, Access.PROTECTED, new JavaType("org.flasck.jvm.Wrapper"), "_wrapper");
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument("test.runner.Card", "card");
				PendingVar parent = ann.argument("org.flasck.jvm.areas.CardArea", "parent");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.jvm.areas.TextArea", "<init>", ctor.as(parent.getVar(), "org.flasck.jvm.areas.Area"), ctor.as(ctor.aNull(), "java.lang.String")).flush();
				ctor.assign(ctor.getField("_card"), card.getVar()).flush();
				ctor.callVirtual("void", ctor.myThis(), "_setText", ctor.stringConst("hello, world")).flush();
				ctor.callVirtual("void", ctor.getField("_wrapper"), "onAssign", ctor.as(ctor.getField("_card"), "java.lang.Object"), ctor.stringConst("sayHello"), ctor.as(ctor.myThis(), "org.flasck.jvm.areas.Area"), ctor.stringConst("_setVariableFormats")).flush();
				ctor.voidExpr(ctor.callVirtual("java.lang.Object", ctor.myThis(), "_setVariableFormats")).flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "_setVariableFormats");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				meth.callSuper("void", "org.flasck.jvm.areas.TextArea", "_setCSSObj", meth.callVirtual("java.lang.Object", meth.myThis(), "formats_0")).flush();
				meth.returnObject(meth.aNull()).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "formats_0");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos1 = meth.avar("org.flasck.jvm.FLClosure", "clos1");
				meth.assign(clos1, meth.makeNew("org.flasck.jvm.FLClosure",
										meth.as(meth.getField("_card"), "java.lang.Object"),
										meth.classConst("test.runner.Card$styleIf"),
										meth.arrayOf("java.lang.Object", Arrays.asList(meth.stringConst("show"), meth.callStatic("java.lang.Boolean", "java.lang.Boolean", "valueOf", meth.getField(meth.getField("_card"), "sayHello")))))).flush();
				meth.returnObject(meth.makeNew("org.flasck.jvm.FLClosure", 
										meth.classConst("org.flasck.jvm.builtin.Cons"),
										meth.arrayOf("java.lang.Object", Arrays.asList(clos1, meth.callStatic("org.flasck.jvm.builtin.Nil", "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<>())))))).flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$styleIf");
			bcc.superclass("java.lang.Object");
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument("test.runner.Card", "card");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "java.lang.Object", "<init>").flush();
				ctor.assign(ctor.getField("_card"), card.getVar()).flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, true, "eval");
				PendingVar meP = ann.argument(new JavaType("java.lang.Object"), "me");
				PendingVar argsP = ann.argument(new JavaType("[java.lang.Object"), "args");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var me = meP.getVar();
				Var args = argsP.getVar();
				meth.returnObject(meth.callVirtual("java.lang.Object", meth.castTo(me, "test.runner.Card"), "styleIf", meth.arrayElt(args, meth.intConst(0)), meth.arrayElt(args, meth.intConst(1)))).flush();
			}
		}
	}
}
