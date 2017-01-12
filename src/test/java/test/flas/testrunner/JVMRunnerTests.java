package test.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.droidgen.J;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.testrunner.JVMRunner;
import org.junit.Before;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
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
		jr.prepareCase();
		runner = jr;
	}
	
	@Before
	public void defineSupportingFunctions() {
		ByteCodeCreator cardBcc = new ByteCodeCreator(bce, "test.runner.Card");
		{
			String sup = J.FLASCK_CARD;
			cardBcc.superclass(sup);
			cardBcc.inheritsField(true, Access.PROTECTED, new JavaType(J.WRAPPER), "_wrapper");
			cardBcc.inheritsField(true, Access.PROTECTED, new JavaType(J.DISPLAY_ENGINE), "_display");
			cardBcc.defineField(false, Access.PROTECTED, JavaType.boolean_, "sayHello");
			cardBcc.defineField(false, Access.PROTECTED, "test.runner.Card$_C1", "e");
		}
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
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Echo");
			bcc.makeAbstract();
			bcc.superclass("org.flasck.jvm.ContractImpl");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "org.flasck.jvm.ContractImpl", "<init>").flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "saySomething");
				ann.argument(J.DELIVERY_ADDRESS, "from");
				ann.argument(J.OBJECT, "msg");
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
				Var clos1 = meth.avar(J.FLCLOSURE, "clos1");
				meth.assign(clos1, (Expr)meth.makeNew(J.FLCLOSURE,
										meth.classConst("org.flasck.jvm.builtin.Assign"),
										meth.arrayOf(J.OBJECT, Arrays.asList(meth.getField("_card"), meth.stringConst("sayHello"), (Expr)meth.as(meth.makeNew("java.lang.Boolean", meth.boolConst(false)), "java.lang.Object"))))).flush();
				meth.returnObject(meth.makeNew(J.FLCLOSURE, 
										meth.classConst("org.flasck.jvm.builtin.Cons"),
										meth.arrayOf(J.OBJECT, Arrays.asList(clos1, meth.callStatic(J.NIL, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())))))).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "setOn");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos1 = meth.avar(J.FLCLOSURE, "clos1");
				meth.assign(clos1, (Expr)meth.makeNew(J.FLCLOSURE,
										meth.classConst("org.flasck.jvm.builtin.Assign"),
										meth.arrayOf(J.OBJECT, Arrays.asList(meth.getField("_card"), meth.stringConst("sayHello"), (Expr)meth.as(meth.makeNew("java.lang.Boolean", meth.boolConst(true)), "java.lang.Object"))))).flush();
				meth.returnObject(meth.makeNew(J.FLCLOSURE, 
										meth.classConst("org.flasck.jvm.builtin.Cons"),
										meth.arrayOf(J.OBJECT, Arrays.asList(clos1, meth.callStatic(J.NIL, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())))))).flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$_C1");
			bcc.superclass("test.runner.Echo");
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument("test.runner.Card", "card");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "test.runner.Echo", "<init>").flush();
				ctor.assign(ctor.getField("_card"), card.getVar()).flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "saySomething");
				ann.argument("org.flasck.jvm.post.DeliveryAddress", "from");
				PendingVar pv = ann.argument("java.lang.Object", "msg");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var msg = pv.getVar();
				meth.assign(msg, meth.callStatic(J.FLEVAL, J.OBJECT, "head", msg)).flush();
				meth.ifBoolean(meth.instanceOf(msg, J.FLERROR), meth.returnObject(msg), null).flush();
				IExpr nil = meth.callStatic(J.NIL, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>()));
				Var clos1 = meth.avar("org.flasck.jvm.FLClosure", "clos1");
				Var clos2 = meth.avar("org.flasck.jvm.FLClosure", "clos2");
				meth.ifBoolean(meth.instanceOf(msg, J.STRING),
					meth.block(
						meth.assign(clos1, 
								(Expr)meth.makeNew(J.FLCLOSURE, meth.classConst(J.CONS), meth.arrayOf(J.OBJECT, Arrays.asList( 
									msg,
									nil)))),
						meth.assign(clos2, 
								(Expr)meth.makeNew(J.FLCLOSURE, meth.classConst(J.SEND), meth.arrayOf(J.OBJECT, Arrays.asList( 
									meth.getField(meth.getField("_card"), "e"),
									meth.stringConst("echoIt"),
									clos1)))),
						meth.returnObject(meth.makeNew("org.flasck.jvm.FLClosure", 
								meth.classConst("org.flasck.jvm.builtin.Cons"),
								meth.arrayOf("java.lang.Object", Arrays.asList(clos2, meth.callStatic(J.NIL, "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<>()))))))
					),
					meth.returnObject(meth.makeNew(J.FLERROR, meth.stringConst("saySomething: case not handled")))).flush();
			}
		}
		{
			// Methods on the card (itself created above)
			String sup = J.FLASCK_CARD;
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(cardBcc, false);
				PendingVar despatcher = ann.argument(J.CARD_DESPATCHER, "despatcher");
				PendingVar display = ann.argument(J.DISPLAY_ENGINE, "display");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", sup, "<init>", despatcher.getVar(), display.getVar()).flush();
                ctor.callVirtual("void", ctor.myThis(), "registerContract", ctor.stringConst("test.runner.SetState"), (Expr)ctor.as(ctor.makeNew("test.runner.Card$_C0", ctor.myThis()), J.CONTRACT_IMPL)).flush();
                IExpr e = ctor.getField("e");
                ctor.assign(e, ctor.makeNew("test.runner.Card$_C1", ctor.myThis())).flush();
                ctor.callVirtual("void", ctor.myThis(), "registerContract", ctor.stringConst("test.runner.Echo"), (Expr)ctor.as(e, J.CONTRACT_IMPL)).flush();
                ctor.callSuper("void", sup, "ready").flush();
                ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(cardBcc, false, "render");
				PendingVar into = ann.argument("java.lang.String", "into");
				ann.returns(JavaType.void_);
				NewMethodDefiner meth = ann.done();
				meth.makeNewVoid("test.runner.Card$B1", meth.myThis(), meth.as(meth.makeNew(J.CARD_AREA, meth.getField("_wrapper"), meth.getField("_display"), into.getVar()), J.AREA)).flush();
				meth.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(cardBcc, false, "echoHello");
				ann.argument(J.OBJECT, "ev");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos1 = meth.avar("org.flasck.jvm.FLClosure", "clos1");
				Var clos2 = meth.avar("org.flasck.jvm.FLClosure", "clos2");
				IExpr nil = meth.callStatic(J.NIL, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>()));
				meth.assign(clos1, 
						(Expr)meth.makeNew(J.FLCLOSURE, meth.classConst(J.CONS), meth.arrayOf(J.OBJECT, Arrays.asList( 
							meth.stringConst("hello clicked"),
							nil)))).flush();
				meth.assign(clos2, 
						(Expr)meth.makeNew(J.FLCLOSURE, meth.classConst(J.SEND), meth.arrayOf(J.OBJECT, Arrays.asList( 
							meth.getField(meth.myThis(), "e"),
							meth.stringConst("echoIt"),
							clos1)))).flush();
				meth.returnObject(meth.makeNew("org.flasck.jvm.FLClosure", 
						meth.classConst("org.flasck.jvm.builtin.Cons"),
						meth.arrayOf("java.lang.Object", Arrays.asList(clos2, nil)))).flush();
				meth.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(cardBcc, false, "styleIf");
				PendingVar str = ann.argument("java.lang.Object", "str");
				PendingVar bool = ann.argument("java.lang.Object", "bool");
				ann.returns("java.lang.Object");
				NewMethodDefiner meth = ann.done();
				Remarker m1 = new Remarker();
//				meth.ifBoolean(meth.unbox(meth.castTo(bool.getVar(), "java.lang.Boolean"), false), meth.returnObject(str.getVar()), meth.block(meth.markHere(m1), meth.returnObject(meth.stringConst("")))).flush();
				meth.ifBoolean(meth.callStatic(J.FLEVAL, J.BOOLEANP, "isTruthy", bool.getVar()), meth.returnObject(str.getVar()), meth.block(meth.markHere(m1), meth.returnObject(meth.stringConst("")))).flush();
				meth.addStackMapFrame(StackMapFrame.SAME_FRAME, m1.getMarker());
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$B1");
			bcc.superclass(J.TEXT_AREA);
			bcc.inheritsField(true, Access.PROTECTED, new JavaType("org.flasck.jvm.Wrapper"), "_wrapper");
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument("test.runner.Card", "card");
				PendingVar parent = ann.argument(J.AREA, "parent");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", J.TEXT_AREA, "<init>", ctor.as(parent.getVar(), J.AREA), ctor.as(ctor.aNull(), "java.lang.String")).flush();
				ctor.assign(ctor.getField("_card"), card.getVar()).flush();
				ctor.callVirtual("void", ctor.myThis(), "_setText", ctor.stringConst("hello, world")).flush();
				ctor.callVirtual("void", ctor.getField("_wrapper"), "onAssign", ctor.as(ctor.getField("_card"), "java.lang.Object"), ctor.stringConst("sayHello"), ctor.as(ctor.myThis(), J.IAREA), ctor.stringConst("_setVariableFormats")).flush();
				ctor.voidExpr(ctor.callVirtual("java.lang.Object", ctor.myThis(), "_setVariableFormats")).flush();
				ctor.voidExpr(ctor.callVirtual("java.lang.Object", ctor.myThis(), "_add_handlers")).flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "_setVariableFormats");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				meth.callSuper("void", J.TEXT_AREA, "_setCSSObj", meth.callVirtual("java.lang.Object", meth.myThis(), "formats_0")).flush();
				meth.returnObject(meth.aNull()).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "_add_handlers");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				meth.callSuper(JavaType.void_.getActual(), J.AREA, "addEventHandler", meth.boolConst(false), meth.stringConst("click"), meth.classConst("test.runner.Card$handlers_1")).flush();
				meth.returnObject(meth.aNull()).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "formats_0");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var clos2 = meth.avar(J.FLCURRY, "clos2");
				meth.assign(clos2, (Expr)meth.makeNew(J.FLCURRY,
						meth.as(meth.getField("_card"), "java.lang.Object"),
						meth.classConst("test.runner.Card$styleIf"),
						meth.intConst(2))).flush();
				Var clos0 = meth.avar(J.FLCLOSURE, "clos0");
				meth.assign(clos0, (Expr)meth.makeNew(J.FLCLOSURE,
						meth.as(clos2, J.OBJECT),
						meth.arrayOf(J.OBJECT, Arrays.asList(meth.stringConst("show"), meth.callStatic("java.lang.Boolean", "java.lang.Boolean", "valueOf", meth.getField(meth.getField("_card"), "sayHello")))))).flush();
				meth.returnObject(meth.makeNew(J.FLCLOSURE, 
						meth.classConst("org.flasck.jvm.builtin.Cons"),
						meth.arrayOf(J.OBJECT, Arrays.asList(clos0, meth.callStatic(J.NIL, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<>())))))).flush();
			}
		}
		{
			ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$handlers_1");
			bcc.superclass("java.lang.Object");
			bcc.implementsInterface(J.HANDLER);
			bcc.defineField(true, Access.PROTECTED, "test.runner.Card", "_card");
			{
				GenericAnnotator ann = GenericAnnotator.newConstructor(bcc, false);
				PendingVar card = ann.argument(J.OBJECT, "card");
				MethodDefiner ctor = ann.done();
				ctor.callSuper("void", "java.lang.Object", "<init>").flush();
				ctor.assign(ctor.getField("_card"), ctor.castTo(card.getVar(), "test.runner.Card")).flush();
				ctor.returnVoid().flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "handle");
				PendingVar evP = ann.argument(new JavaType(J.OBJECT), "ev");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				meth.returnObject(meth.makeNew(J.FLCLOSURE, meth.as(meth.getField("_card"), J.OBJECT), meth.callVirtual(J.CLASS, meth.myThis(), "getHandler"), meth.arrayOf(J.OBJECT, Arrays.asList(evP.getVar())))).flush();
			}
			{
				GenericAnnotator ann = GenericAnnotator.newMethod(bcc, false, "getHandler");
				ann.returns(J.CLASS);
				NewMethodDefiner meth = ann.done();
				meth.returnObject(meth.classConst("test.runner.Card$echoHello")).flush();
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
				PendingVar meP = ann.argument(new JavaType(J.OBJECT), "me");
				PendingVar argsP = ann.argument(new JavaType("["+J.OBJECT), "args");
				ann.returns(JavaType.object_);
				NewMethodDefiner meth = ann.done();
				Var me = meP.getVar();
				Var args = argsP.getVar();
				meth.returnObject(meth.callVirtual(J.OBJECT, meth.castTo(me, "test.runner.Card"), "styleIf", meth.arrayElt(args, meth.intConst(0)), meth.arrayElt(args, meth.intConst(1)))).flush();
			}
		}
	}
	{
		ByteCodeCreator bcc = new ByteCodeCreator(bce, "test.runner.Card$echoHello");
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
			PendingVar meP = ann.argument(new JavaType(J.OBJECT), "me");
			PendingVar argsP = ann.argument(new JavaType("["+J.OBJECT), "args");
			ann.returns(JavaType.object_);
			NewMethodDefiner meth = ann.done();
			Var me = meP.getVar();
			Var args = argsP.getVar();
			meth.returnObject(meth.callVirtual(J.OBJECT, meth.castTo(me, "test.runner.Card"), "echoHello", meth.arrayElt(args, meth.intConst(0)))).flush();
		}
	}
}
