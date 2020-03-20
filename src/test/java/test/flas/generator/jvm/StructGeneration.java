package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class StructGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);

	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aClassWithAConstructorAndEvalIsAlwaysGenerated() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink eclz = context.mock(ByteCodeSink.class, "eclz");
		SolidName sn = new SolidName(pkg, "Struct");
		String ename = "test.repo.Struct";
		
		IExpr mknew = context.mock(IExpr.class, "mknew");
		IExpr ass = context.mock(IExpr.class, "ass");
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // clz
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			oneOf(eclz).superclass(J.JVM_FIELDS_CONTAINER_WRAPPER);
			allowing(eclz).generateAssociatedSourceFile();
			allowing(eclz).getCreatedName(); will(returnValue(ename));
		}});

		MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
		context.checking(new Expectations() {{
			oneOf(ctor).nextLocal(); will(returnValue(3));
		}});
		Var ccxt = new Var.AVar(ctor, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		context.checking(new Expectations() {{ // ctor
			oneOf(eclz).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(ccxt));
			oneOf(ctor).callSuper("void", J.JVM_FIELDS_CONTAINER_WRAPPER, "<init>", ccxt);
			oneOf(ctor).returnVoid(); will(returnValue(doret));
			oneOf(doret).flush();
		}});

		MethodDefiner eval = context.mock(MethodDefiner.class, "eval");
		context.checking(new Expectations() {{
			oneOf(eval).nextLocal(); will(returnValue(6));
			oneOf(eval).nextLocal(); will(returnValue(7));
			oneOf(eval).nextLocal(); will(returnValue(8));
		}});
		Var ecxt = new Var.AVar(eval, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		Var eret = new Var.AVar(eval, ename, "ret");
		Var args = new Var.AVar(eval, "[" + J.OBJECT, "args");
		context.checking(new Expectations() {{ // eval
			oneOf(eclz).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(eclz).inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
			oneOf(eclz).createMethod(true, J.OBJECT, "eval"); will(returnValue(eval));
			oneOf(eval).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(ecxt));
			oneOf(eval).argument("[" + J.OBJECT, "args"); will(returnValue(args));
			oneOf(eval).avar(ename, "ret"); will(returnValue(eret));
			oneOf(eval).makeNew(ename, ecxt); will(returnValue(mknew));
			oneOf(eval).assign(eret, mknew); will(returnValue(ass));
			oneOf(ass).flush();
			oneOf(eval).returnObject(eret); will(returnValue(doret));
			oneOf(doret).flush();
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		new Traverser(gen).visitStructDefn(sd);
	}

	@Test
	public void fieldsArePopulatedInTheConstructorIfPresent() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink eclz = context.mock(ByteCodeSink.class, "eclz");
		SolidName sn = new SolidName(pkg, "Struct");
		String ename = "test.repo.Struct";
		
		IExpr mknew = context.mock(IExpr.class, "mknew");
		IExpr ass = context.mock(IExpr.class, "ass");
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // clz
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			oneOf(eclz).superclass(J.JVM_FIELDS_CONTAINER_WRAPPER);
			allowing(eclz).generateAssociatedSourceFile();
			allowing(eclz).getCreatedName(); will(returnValue(ename));
		}});

		MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
		context.checking(new Expectations() {{
			oneOf(ctor).nextLocal(); will(returnValue(3));
		}});
		Var ccxt = new Var.AVar(ctor, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		context.checking(new Expectations() {{ // ctor
			oneOf(eclz).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(ccxt));
			oneOf(ctor).callSuper("void", J.JVM_FIELDS_CONTAINER_WRAPPER, "<init>", ccxt);
			oneOf(ctor).returnVoid(); will(returnValue(doret));
			oneOf(doret).flush();
		}});

		MethodDefiner eval = context.mock(MethodDefiner.class, "eval");
		context.checking(new Expectations() {{
			oneOf(eval).nextLocal(); will(returnValue(6));
			oneOf(eval).nextLocal(); will(returnValue(7));
			oneOf(eval).nextLocal(); will(returnValue(8));
		}});
		Var ecxt = new Var.AVar(eval, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		Var eret = new Var.AVar(eval, ename, "ret");
		Var args = new Var.AVar(eval, "[" + J.OBJECT, "args");
		IExpr sarg = context.mock(IExpr.class, "sarg");
		IExpr state = context.mock(IExpr.class, "state");
		IExpr setField = context.mock(IExpr.class, "setField");
		IExpr sh = context.mock(IExpr.class, "hello");
		IExpr val = context.mock(IExpr.class, "val");
		Sequence flushes = context.sequence("flushes");
		context.checking(new Expectations() {{ // eval
			oneOf(eclz).inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
			oneOf(eclz).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "nfargs");
			oneOf(eclz).createMethod(true, J.OBJECT, "eval"); will(returnValue(eval));
			oneOf(eval).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(ecxt));
			oneOf(eval).argument("[" + J.OBJECT, "args"); will(returnValue(args));
			oneOf(eval).avar(ename, "ret"); will(returnValue(eret));
			oneOf(eval).makeNew(ename, ecxt); will(returnValue(mknew));
			oneOf(eval).assign(eret, mknew); will(returnValue(ass));
			oneOf(ass).flush(); inSequence(flushes);
			oneOf(eval).stringConst("hello"); will(returnValue(sh));
			oneOf(eval).as(sh, J.OBJECT); will(returnValue(val));
			oneOf(eval).getField(eret, "state"); will(returnValue(state));
			oneOf(eval).stringConst("s"); will(returnValue(sarg));
			oneOf(eval).callInterface("void", state, "set", sarg, val); will(returnValue(setField));
			oneOf(setField).flush(); inSequence(flushes);
			oneOf(eval).returnObject(eret); will(returnValue(doret));
			oneOf(doret).flush(); inSequence(flushes);
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, false, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sd.addField(sf);
		new Traverser(gen).visitStructDefn(sd);
	}

	@Test
	public void fieldAccessorsAreCreatedAsNeeded() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		SolidName sn = new SolidName(pkg, "Struct");
		String ename = "test.repo.Struct";
		ByteCodeSink bcc = context.mock(ByteCodeSink.class, "bcc");
		MethodDefiner acc = context.mock(MethodDefiner.class, "acc");
		IExpr state = context.mock(IExpr.class, "state");
		IExpr sarg = context.mock(IExpr.class, "sarg");
		IExpr getField = context.mock(IExpr.class, "getField");
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // eval
			oneOf(bce).get(ename); will(returnValue(bcc));
			oneOf(bcc).createMethod(false, J.OBJECT, "_field_s"); will(returnValue(acc));
			oneOf(acc).argument(J.FLEVALCONTEXT, "cxt");
			oneOf(acc).argument("[" + J.OBJECT, "args");
			oneOf(acc).getField("state"); will(returnValue(state));
			oneOf(acc).stringConst("s"); will(returnValue(sarg));
			oneOf(acc).callInterface(J.OBJECT, state, "get", sarg); will(returnValue(getField));
			oneOf(acc).returnObject(getField); will(returnValue(doret));
			oneOf(doret).flush();
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, true, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sf.fullName(new VarName(pos, sn, "s"));
		sd.addField(sf);
		new Traverser(gen).withHSI().visitEntry(sf);
	}
}
