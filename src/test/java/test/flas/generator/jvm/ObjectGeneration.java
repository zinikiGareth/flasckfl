package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class ObjectGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void aClassWithAConstructorAndEvalIsAlwaysGenerated() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink eclz = context.mock(ByteCodeSink.class, "eclz");
		SolidName sn = new SolidName(pkg, "Obj");
		String ename = "test.repo.Obj";
		context.assertIsSatisfied();
		
		IExpr mknew = context.mock(IExpr.class, "mknew");
		IExpr ass = context.mock(IExpr.class, "ass");
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // clz
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			oneOf(eclz).superclass(J.OBJECT);
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
			oneOf(ctor).callSuper("void", J.OBJECT, "<init>");
			oneOf(ctor).returnVoid(); will(returnValue(doret));
			oneOf(doret).flush();
		}});

		MethodDefiner eval = context.mock(MethodDefiner.class, "eval");
		context.checking(new Expectations() {{
			oneOf(eval).nextLocal(); will(returnValue(6));
			oneOf(eval).nextLocal(); will(returnValue(7));
		}});
		Var ecxt = new Var.AVar(eval, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		Var eret = new Var.AVar(eval, ename, "ret");
		context.checking(new Expectations() {{ // eval
			oneOf(eclz).createMethod(true, J.OBJECT, "eval"); will(returnValue(eval));
			oneOf(eval).argument(J.FLEVALCONTEXT, "cxt"); will(returnValue(ecxt));
			oneOf(eval).avar(ename, "ret"); will(returnValue(eret));
			oneOf(eval).makeNew(ename, ecxt); will(returnValue(mknew));
			oneOf(eval).assign(eret, mknew); will(returnValue(ass));
			oneOf(ass).flush();
			oneOf(eval).returnObject(eret); will(returnValue(doret));
			oneOf(doret).flush();
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		new Traverser(gen).visitObjectDefn(od);
	}
}
