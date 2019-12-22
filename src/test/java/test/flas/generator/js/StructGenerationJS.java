package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class StructGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	@Test
	public void aClassWithEvalIsAlwaysGenerated() {
		SolidName sn = new SolidName(pkg, "Struct");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSBlockCreator ctorBlock = context.mock(JSBlockCreator.class);
		JSMethodCreator eval = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.Struct"); will(returnValue(clz));
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).stateField();
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).argument("_cxt");
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		new Traverser(gen).visitStructDefn(sd);
	}

	@Test
	public void fieldsAreSavedInTheConstructorIfPresent() {
		SolidName sn = new SolidName(pkg, "Struct");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSBlockCreator ctorBlock = context.mock(JSBlockCreator.class);
		JSMethodCreator eval = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class, "obj");
		JSExpr parmS = context.mock(JSExpr.class, "s");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.Struct"); will(returnValue(clz));
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).stateField();
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).argument("_cxt");
			oneOf(eval).argument("s"); will(returnValue(parmS));
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).storeField(obj, "s", parmS);
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, false, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sd.addField(sf);
		new Traverser(gen).visitStructDefn(sd);
	}
	
	@Test
	public void fieldAccessorsAreCreatedAsNeeded() {
		SolidName sn = new SolidName(pkg, "Struct");
		JSMethodCreator sfacc = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class, "obj");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo.Struct");
			oneOf(jss).newFunction("test.repo", "test.repo.Struct", true, "_field_s"); will(returnValue(sfacc));
			oneOf(sfacc).argument("_cxt");
			oneOf(sfacc).loadField("s"); will(returnValue(obj));
			oneOf(sfacc).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, true, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sf.fullName(new VarName(pos, sn, "s"));
		sd.addField(sf);
		new Traverser(gen).withHSI().visitEntry(sf);
	}
	
}
