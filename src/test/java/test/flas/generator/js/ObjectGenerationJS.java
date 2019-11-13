package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.JSClassCreator;
import org.flasck.flas.compiler.jsgen.JSExpr;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.JSStorage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ObjectGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	@Test
	public void aClassWithEvalIsAlwaysGenerated() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSBlockCreator ctorBlock = context.mock(JSBlockCreator.class);
		JSMethodCreator eval = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.Obj"); will(returnValue(clz));
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).fieldObject("state", "FieldsContainer");
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		new Traverser(gen).visitObjectDefn(od);
	}

	@Test
	public void fieldsArePopulatedInTheConstructorIfPresent() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSBlockCreator ctorBlock = context.mock(JSBlockCreator.class);
		JSMethodCreator eval = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.Obj"); will(returnValue(clz));
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).fieldObject("state", "FieldsContainer");
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		StateDefinition sd = new StateDefinition(pos);
		StructField sf = new StructField(pos, false, LoadBuiltins.stringTR, "s");
		sd.addField(sf);
		od.defineState(sd);
		new Traverser(gen).visitObjectDefn(od);
	}
}
