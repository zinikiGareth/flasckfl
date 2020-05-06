package test.flas.generator.jvm;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.compiler.jvmgen.JVMGenerator;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.jvm.J;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;

public class ContractGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");
	private final MethodDefiner meth = context.mock(MethodDefiner.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(meth).lenientMode(with(any(Boolean.class)));
		}});
	}

	@Test
	public void simpleContractDeclaration() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink parent = context.mock(ByteCodeSink.class, "parent");
		String pname = "test.repo.MyContract";
		context.checking(new Expectations() {{
			oneOf(bce).newClass(pname); will(returnValue(parent));
			allowing(parent).getCreatedName(); will(returnValue(pname));
			oneOf(parent).makeInterface();
			oneOf(parent).generateAssociatedSourceFile();
			oneOf(parent).implementsInterface(J.DOWN_CONTRACT);
		}});
		StackVisitor sv = new StackVisitor();
		new JVMGenerator(bce, sv, null);
		new JVMGenerator(bce, new StackVisitor(), null);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, cname);
		new Traverser(sv).visitContractDecl(cd);
	}

	@Test
	public void contractMethodGetsGenerated() {
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		IFieldInfo fi = context.mock(IFieldInfo.class);
		context.checking(new Expectations() {{
			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "_cxt");
			oneOf(meth).argument("java.lang.Object", "_ih");
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "_nf_m"); will(returnValue(fi));
			oneOf(fi).constValue(0);
		}});
		JVMGenerator gen = JVMGenerator.forTests(bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>(), null);
		new Traverser(gen).visitContractMethod(cmd);
	}

	@Test
	public void contractMethodMayHaveArguments() {
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		IFieldInfo fi = context.mock(IFieldInfo.class);
		context.checking(new Expectations() {{
			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "_cxt");
			oneOf(meth).argument("java.lang.Object", "hello");
			oneOf(meth).argument("java.lang.Object", "_ih");
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "_nf_m"); will(returnValue(fi));
			oneOf(fi).constValue(1);
		}});
		JVMGenerator gen = JVMGenerator.forTests(bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>(), null);
		cmd.args.add(new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, cname, "hello")));
		new Traverser(gen).visitContractMethod(cmd);
	}

	@Test
	public void contractMethodMayHaveArgumentsButDeclaredHandlerImpliesNoIH() {
		ByteCodeSink bcc = context.mock(ByteCodeSink.class);
		IFieldInfo fi = context.mock(IFieldInfo.class);
		context.checking(new Expectations() {{
			oneOf(bcc).createMethod(false, J.OBJECT, "m"); will(returnValue(meth));
			oneOf(meth).argument(J.FLEVALCONTEXT, "_cxt");
			oneOf(meth).argument("java.lang.Object", "handler");
			oneOf(bcc).defineField(true, Access.PUBLICSTATIC, JavaType.int_, "_nf_m"); will(returnValue(fi));
			oneOf(fi).constValue(1);
		}});
		JVMGenerator gen = JVMGenerator.forTests(bcc);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>(), null);
		TypeReference tr = new TypeReference(pos, "MyHandler");
		tr.bind(new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "AContract")));
		cmd.args.add(new TypedPattern(pos, tr, new VarName(pos, cname, "handler")));
		new Traverser(gen).visitContractMethod(cmd);
	}
}
