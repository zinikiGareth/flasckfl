package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
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
import org.zinutils.bytecode.JavaInfo.Access;
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
		
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // clz
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			oneOf(eclz).superclass(J.FLOBJECT);
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
			oneOf(ctor).callSuper("void", J.FLOBJECT, "<init>", ccxt);
			oneOf(ctor).returnVoid(); will(returnValue(doret));
			oneOf(doret).flush();
			oneOf(eclz).inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen, null);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		new Traverser(gen).visitObjectDefn(od);
	}

	@Test
	public void fieldsArePopulatedInTheConstructorIfPresent() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink eclz = context.mock(ByteCodeSink.class, "eclz");
		SolidName sn = new SolidName(pkg, "Obj");
		String ename = "test.repo.Obj";
		
		IExpr doret = context.mock(IExpr.class, "doret");
		context.checking(new Expectations() {{ // clz
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			oneOf(eclz).superclass(J.FLOBJECT);
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
			oneOf(ctor).callSuper("void", J.FLOBJECT, "<init>", ccxt);
			oneOf(ctor).returnVoid(); will(returnValue(doret));
			oneOf(doret).flush();
			oneOf(eclz).inheritsField(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		}});
		StackVisitor gen = new StackVisitor();
		new JVMGenerator(bce, gen, null);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		StateDefinition sd = new StateDefinition(pos);
		StructField sf = new StructField(pos, pos, sd, false, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sd.addField(sf);
		od.defineState(sd);
		new Traverser(gen).visitObjectDefn(od);
	}
}
