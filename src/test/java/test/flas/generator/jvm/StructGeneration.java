package test.flas.generator.jvm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class StructGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void simpleEntityDeclarationCreatesAClass() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink eclz = context.mock(ByteCodeSink.class, "eclz");
		String ename = "test.repo.MyThing";
		IExpr n = context.mock(IExpr.class, "null");
		IExpr nas = context.mock(IExpr.class, "nullAsBackingDocument");
		MethodDefiner ctor = context.mock(MethodDefiner.class, "ctor");
		context.checking(new Expectations() {{
			oneOf(ctor).nextLocal(); will(returnValue(6));
			oneOf(ctor).nextLocal(); will(returnValue(7));
		}});
		Var cxt = new Var.AVar(ctor, "org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
		Var doc = new Var.AVar(ctor, "org.ziniki.ziwsh.model.BackingDocument", "doc");
		context.checking(new Expectations() {{
			oneOf(bce).newClass(ename); will(returnValue(eclz));
			allowing(eclz).getCreatedName(); will(returnValue(ename));
			allowing(eclz).generateAssociatedSourceFile();
			oneOf(eclz).superclass("org.flasck.jvm.fl.FLASEntity");
			
			oneOf(eclz).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt"); will(returnValue(cxt));
			oneOf(ctor).aNull(); will(returnValue(n));
			oneOf(ctor).as(n, "org.ziniki.ziwsh.model.BackingDocument"); will(returnValue(nas));
			oneOf(ctor).callSuper("void", "org.flasck.jvm.fl.FLASEntity", "<init>", cxt, nas);
			oneOf(ctor).returnVoid();

			oneOf(eclz).createMethod(false, "void", "<init>"); will(returnValue(ctor));
			oneOf(ctor).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt"); will(returnValue(cxt));
			oneOf(ctor).argument("org.ziniki.ziwsh.model.BackingDocument", "doc"); will(returnValue(doc));
			oneOf(ctor).callSuper("void", "org.flasck.jvm.fl.FLASEntity", "<init>", cxt, doc);
			oneOf(ctor).returnVoid();
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		StructDefn sd = new StructDefn(pos, FieldsType.ENTITY, "test.repo", "MyThing", true);
		new Traverser(gen).visitStructDefn(sd);
	}

	@Test
	public void entityDeclarationWithNoGenerationDoesNotCreateAClass() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		context.checking(new Expectations() {{
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		StructDefn sd = new StructDefn(pos, FieldsType.ENTITY, "test.repo", "MyThing", false);
		new Traverser(gen).visitStructDefn(sd);
	}
}
