package test.flas.generator.js;

import java.util.ArrayList;
import java.util.HashMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.flasck.flas.parsedForm.EventHolder;
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
import org.zinutils.bytecode.JavaInfo.Access;

public class ObjectGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	@Test
	public void aClassWithEvalIsAlwaysGenerated() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSMethodCreator ctorBlock = context.mock(JSMethodCreator.class, "ctor");
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		JSVar cx = new JSVar("_cxt");
		JSVar v = new JSVar("_card");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).object(od);
			oneOf(jss).newClass("test.repo", new SolidName(new PackageName("test.repo"), "Obj")); will(returnValue(clz));
			oneOf(clz).inheritsFrom(with(any(PackageName.class)), with(J.FLOBJECT));
			oneOf(clz).implementsJava(J.AREYOUA);
			oneOf(clz).inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
			oneOf(clz).inheritsField(true, Access.PROTECTED, new PackageName(J.FLCARD), "_card");
			oneOf(clz).createMethod("_areYouA", true);
			oneOf(clz).createMethod("_updateDisplay", true);
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).argument(J.FLEVALCONTEXT, "_cxt"); will(returnValue(cx));
			oneOf(ctorBlock).argument("_incard"); will(returnValue(v));
			oneOf(ctorBlock).superArg(cx);
			oneOf(ctorBlock).superArg(v);
			oneOf(ctorBlock).arg(1); will(returnValue(v));
			oneOf(ctorBlock).setField(true, "_card", v);
			oneOf(ctorBlock).stateField(true);
			oneOf(ctorBlock).returnVoid();
			oneOf(jss).eventMap(sn, null);
			oneOf(jss).methodList(sn, new ArrayList<>());
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, new HashMap<EventHolder, EventTargetZones>());
		new Traverser(gen).visitObjectDefn(od);
	}

	@Test
	public void fieldsArePopulatedInTheConstructorIfPresent() {
		SolidName sn = new SolidName(pkg, "Obj");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSMethodCreator ctorBlock = context.mock(JSMethodCreator.class, "ctor");
		ObjectDefn od = new ObjectDefn(pos, pos, sn, true, new ArrayList<>());
		JSVar cx = new JSVar("_cxt");
		JSVar v = new JSVar("_card");
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).object(od);
			oneOf(jss).newClass("test.repo", new SolidName(new PackageName("test.repo"), "Obj")); will(returnValue(clz));
			oneOf(clz).inheritsFrom(with(any(PackageName.class)), with(J.FLOBJECT));
			oneOf(clz).implementsJava(J.AREYOUA);
			oneOf(clz).inheritsField(true, Access.PROTECTED, new PackageName(J.FIELDS_CONTAINER), "state");
			oneOf(clz).inheritsField(true, Access.PROTECTED, new PackageName(J.FLCARD), "_card");
			oneOf(clz).createMethod("_areYouA", true);
			oneOf(clz).createMethod("_updateDisplay", true);
			oneOf(ctorBlock).argument(J.FLEVALCONTEXT, "_cxt"); will(returnValue(cx));
			oneOf(ctorBlock).argument("_incard"); will(returnValue(v));
			oneOf(ctorBlock).superArg(cx);
			oneOf(ctorBlock).superArg(v);
			oneOf(ctorBlock).arg(1); will(returnValue(v));
			oneOf(ctorBlock).setField(true, "_card", v);
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).stateField(true);
			oneOf(ctorBlock).returnVoid();
			oneOf(jss).eventMap(sn, null);
			oneOf(jss).methodList(sn, new ArrayList<>());
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, new HashMap<EventHolder, EventTargetZones>());
		StateDefinition sd = new StateDefinition(pos);
		StructField sf = new StructField(pos, pos, sd, false, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sd.addField(sf);
		od.defineState(sd);
		new Traverser(gen).visitObjectDefn(od);
	}
}
