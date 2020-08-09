package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSCompare;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
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
		JSMethodCreator ctorBlock = context.mock(JSMethodCreator.class, "ctor");
		JSMethodCreator eval = context.mock(JSMethodCreator.class, "eval");
		JSMethodCreator aya = context.mock(JSMethodCreator.class, "areYouA");
		JSExpr obj = context.mock(JSExpr.class);
		JSExpr str = context.mock(JSExpr.class, "str");
		JSIfCreator ie = context.mock(JSIfCreator.class, "ie");
		RepositoryReader repo = context.mock(RepositoryReader.class);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", new SolidName(new PackageName("test.repo"), "Struct")); will(returnValue(clz));
			oneOf(clz).inheritsFrom(null, J.JVM_FIELDS_CONTAINER_WRAPPER);
			oneOf(clz).implementsJava(J.AREYOUA);
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).stateField();
			oneOf(ctorBlock).string("test.repo.Struct"); will(returnValue(str));
			oneOf(ctorBlock).storeField(true, null, "_type", str);
			oneOf(clz).createMethod("_areYouA", true); will(returnValue(aya));
			oneOf(aya).argument(J.EVALCONTEXT, "_cxt");
			oneOf(aya).argument(J.STRING, "ty");
			oneOf(aya).returnsType("boolean");
			oneOf(aya).arg(1);
			oneOf(aya).string("test.repo.Struct");
			oneOf(repo).unionsContaining(sd); will(returnValue(new ArrayList<>()));
			oneOf(aya).ifTrue(with(any(JSCompare.class))); will(returnValue(ie));
			oneOf(ie).trueCase();
			oneOf(ie).trueCase();
			oneOf(ie).falseCase();
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).argument(J.FLEVALCONTEXT, "_cxt");
			oneOf(eval).argumentList();
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(repo, jss, gen, null);
		new Traverser(gen).visitStructDefn(sd);
	}

	@Test
	public void fieldsAreSavedInTheConstructorIfPresent() {
		SolidName sn = new SolidName(pkg, "Struct");
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSMethodCreator ctorBlock = context.mock(JSMethodCreator.class, "ctor");
		JSMethodCreator eval = context.mock(JSMethodCreator.class);
		JSExpr obj = context.mock(JSExpr.class, "obj");
		JSExpr str = context.mock(JSExpr.class, "str");
		JSExpr strS = context.mock(JSExpr.class, "s");
		RepositoryReader repo = context.mock(RepositoryReader.class);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", new SolidName(new PackageName("test.repo"), "Struct")); will(returnValue(clz));
			oneOf(clz).inheritsFrom(null, J.JVM_FIELDS_CONTAINER_WRAPPER);
			oneOf(clz).implementsJava(J.AREYOUA);
			oneOf(clz).constructor(); will(returnValue(ctorBlock));
			oneOf(ctorBlock).stateField();
			oneOf(ctorBlock).string("test.repo.Struct"); will(returnValue(str));
			oneOf(repo).unionsContaining(sd); will(returnValue(new ArrayList<>()));
			oneOf(ctorBlock).storeField(true, null, "_type", str);
			oneOf(clz).createMethod("_areYouA", true);
			oneOf(clz).createMethod("eval", false); will(returnValue(eval));
			oneOf(eval).argument(J.FLEVALCONTEXT, "_cxt");
			oneOf(eval).argumentList();
			oneOf(eval).newOf(sn); will(returnValue(obj));
			oneOf(eval).string("hello"); will(returnValue(strS));
			oneOf(eval).storeField(false, obj, "s", strS);
			oneOf(eval).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(repo, jss, gen, null);
		StructField sf = new StructField(pos, pos, sd, false, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
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
			oneOf(jss).newFunction(null, "test.repo", new SolidName(new PackageName("test.repo"), "Struct"), true, "_field_s"); will(returnValue(sfacc));
			oneOf(sfacc).argument("_cxt");
			oneOf(sfacc).argumentList();
			oneOf(sfacc).loadField(with(any(JSThis.class)), with("s")); will(returnValue(obj));
			oneOf(sfacc).returnObject(obj);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, sn, true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, sd, true, LoadBuiltins.stringTR, "s", new StringLiteral(pos, "hello"));
		sf.fullName(new VarName(pos, sn, "s"));
		sd.addField(sf);
		new Traverser(gen).withHSI().visitEntry(sf);
	}
	
}
