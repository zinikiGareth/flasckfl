package test.flas.generator.js;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ContractGenerationJS {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final JSStorage jss = context.mock(JSStorage.class);

	// TODO: contract generation is a work in progress
	@Test
	public void simpleContractDeclarationForcesThreeClassesToBeGenerated() {
//		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
//		ByteCodeSink parent = context.mock(ByteCodeSink.class, "parent");
//		ByteCodeSink up = context.mock(ByteCodeSink.class, "up");
//		ByteCodeSink down = context.mock(ByteCodeSink.class, "down");
//		String pname = "test.repo.MyContract";
		JSClassCreator clz = context.mock(JSClassCreator.class);
		JSMethodCreator meth = context.mock(JSMethodCreator.class);
		context.checking(new Expectations() {{
			oneOf(jss).ensurePackageExists("test.repo", "test.repo");
			oneOf(jss).newClass("test.repo", "test.repo.MyContract"); will(returnValue(clz));
			oneOf(clz).createMethod("name", true); will(returnValue(meth));
			oneOf(meth).returnObject(with(any(JSString.class)));
//			oneOf(bce).newClass(pname); will(returnValue(parent));
//			allowing(parent).getCreatedName(); will(returnValue(pname));
//
//			oneOf(bce).newClass("test.repo.MyContract$Up"); will(returnValue(up));
//			allowing(up).generateAssociatedSourceFile();
//			allowing(down).generateAssociatedSourceFile();
//
//			oneOf(bce).newClass("test.repo.MyContract$Down"); will(returnValue(down));
//			
//			oneOf(parent).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Up");
//			oneOf(up).makeInterface();
//			oneOf(up).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Up");
//			oneOf(up).implementsInterface("org.ziniki.ziwsh.UpContract");
//			
//			oneOf(parent).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Down");
//			oneOf(down).makeInterface();
//			oneOf(down).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Down");
//			oneOf(down).implementsInterface("org.ziniki.ziwsh.UpContract");
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, cname);
		new Traverser(gen).visitContractDecl(cd);
	}

	@Test
	public void contractMethodGetsGenerated() {
//		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		context.checking(new Expectations() {{
//			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
//			oneOf(meth).argument("org.ziniki.ziwsh.json.FLEvalContext", "_cxt");
//			oneOf(meth).argument("java.lang.Object", "_ih");
		}});
		StackVisitor gen = new StackVisitor();
		new JSGenerator(jss, gen);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.DOWN, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>());
		new Traverser(gen).visitContractMethod(cmd);
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
