package test.flas.generator.js;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.JavaInfo.Access;

public class ContractGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	@Test
	public void simpleContractDeclaration() {
		JSClassCreator clz = context.mock(JSClassCreator.class, "clz");
		JSMethodCreator meth = context.mock(JSMethodCreator.class, "meth");
		JSExpr jsa = context.mock(JSExpr.class, "array");
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, cname);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newClass(new PackageName("test.repo"), new SolidName(new PackageName("test.repo"), "MyContract")); will(returnValue(clz));
			oneOf(clz).justAnInterface();
			exactly(2).of(clz).constructor();
			oneOf(jss).contract(cd);
			oneOf(clz).createMethod("name", true); will(returnValue(meth));
			oneOf(meth).noJVM();
			oneOf(meth).returnObject(with(any(JSString.class)));
			oneOf(clz).createMethod("_methods", true); will(returnValue(meth));
			oneOf(meth).jsArray(Arrays.asList()); will(returnValue(jsa));
			oneOf(meth).returnObject(jsa);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		new Traverser(gen).visitContractDecl(cd);
	}

	@Test
	public void contractMethodGetsGenerated() {
		JSClassCreator clz = context.mock(JSClassCreator.class, "clz");
		JSMethodCreator meth = context.mock(JSMethodCreator.class, "meth");
		JSString jse = new JSString("jse");
		JSExpr jsa = context.mock(JSExpr.class, "array");
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, cname);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists(new PackageName("test.repo"), "test.repo");
			oneOf(jss).newClass(new PackageName("test.repo"), new SolidName(new PackageName("test.repo"), "MyContract")); will(returnValue(clz));
			oneOf(clz).justAnInterface();
			exactly(2).of(clz).constructor();
			oneOf(jss).contract(cd);
			oneOf(clz).createMethod("name", true); will(returnValue(meth));
			oneOf(meth).returnObject(with(any(JSString.class)));
			oneOf(clz).createMethod("_methods", true); will(returnValue(meth));
			oneOf(meth).noJVM();
			oneOf(meth).string("m"); will(returnValue(jse));
			oneOf(meth).jsArray(Arrays.asList(jse)); will(returnValue(jsa));
			oneOf(meth).returnObject(jsa);
			oneOf(clz).field(true, Access.PUBLICSTATIC, new PackageName("int"), "_nf_m", 0);
			oneOf(clz).createMethod("m", true);
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(null, jss, gen, null);
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>(), null);
		cd.methods.add(cmd);
		new Traverser(gen).visitContractDecl(cd);
	}

	/*
	@Test
	public void contractMethodMayHaveArguments() {
//		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		context.checking(new Expectations() {{
//			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
//			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
//			oneOf(meth).argument("java.lang.Object", "hello");
//			oneOf(meth).argument("java.lang.Object", "_ih");
		}});
		JVMGenerator gen = JVMGenerator.forTests(null, null, bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.DOWN, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>());
		cmd.args.add(new VarPattern(pos, new VarName(pos, cname, "hello")));
		new Traverser(gen).visitContractMethod(cmd);
	}

	@Test
	public void contractMethodMayHaveArgumentsButDeclaredHandlerImpliesNoIH() {
//		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		context.checking(new Expectations() {{
//			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
//			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
//			oneOf(meth).argument("java.lang.Object", "handler");
		}});
		JVMGenerator gen = JVMGenerator.forTests(null, null, bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.DOWN, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>());
		TypeReference tr = new TypeReference(pos, "MyHandler");
		tr.bind(new ContractDecl(pos, pos, new SolidName(pkg, "AContract")));
		cmd.args.add(new TypedPattern(pos, tr, new VarName(pos, cname, "handler")));
		new Traverser(gen).visitContractMethod(cmd);
	}
	
	// I'm not sure this is a real case, but I'm also not sure it's not
	@Test
	public void contractMethodWithComplexPatternArgumentsJustGetBoringNames() {
//		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		context.checking(new Expectations() {{
//			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
//			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
//			oneOf(meth).argument("java.lang.Object", "a1");
//			oneOf(meth).argument("java.lang.Object", "_ih");
		}});
		JVMGenerator gen = JVMGenerator.forTests(null, null, bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.DOWN, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>());
		cmd.args.add(new TuplePattern(pos, new ArrayList<>()));
		new Traverser(gen).visitContractMethod(cmd);
	}
	*/
}
