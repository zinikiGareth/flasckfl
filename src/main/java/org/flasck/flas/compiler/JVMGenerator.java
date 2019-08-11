package org.flasck.flas.compiler;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.jvm.J;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.exceptions.NotImplementedException;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeStorage bce;
	private MethodDefiner meth;
	private List<IExpr> stack = new ArrayList<IExpr>();
	private IExpr runner;
	private ByteCodeSink clz;
	private ByteCodeSink upClz;
	private ByteCodeSink downClz;

	public JVMGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}
	
	private JVMGenerator(MethodDefiner meth, IExpr runner) {
		this.bce = null;
		this.meth = meth;
		this.runner = runner;
	}

	private JVMGenerator(ByteCodeSink clz) {
		this.bce = null;
		this.clz = clz;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		this.clz = bce.newClass(fn.name().javaClassName());
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "eval");
		ann.returns(JavaType.object_);
		meth = ann.done();
	}
	
	// TODO: this should have been reduced to HSIE, which we should generate from
	// But I am hacking for now to get a walking skeleton up and running so we can E2E TDD
	// The actual traversal is done by the traverser ...

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (stack.size() != 1) {
			throw new RuntimeException("I was expecting a stack depth of 1, not " + stack.size());
		}
		meth.returnObject(stack.get(0)).flush();
	}
	
	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		Object val = expr.value();
		if (val instanceof Integer)
			stack.add(meth.makeNew(J.NUMBER, meth.box(meth.intConst((int) val)), meth.castTo(meth.aNull(), "java.lang.Double")));
		else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(meth.stringConst(expr.text));
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		FunctionDefinition defn = (FunctionDefinition)var.defn();
		if (defn == null)
			throw new RuntimeException("var " + var + " was still not resolved");
		stack.add(meth.callStatic(defn.name().javaClassName(), "java.lang.Object", "eval", new IExpr[0]));
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		clz = bce.newClass(clzName);
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		PendingVar runner = ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		ann.returns(JavaType.void_);
		meth = ann.done();
		this.runner = runner.getVar();
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
		meth.returnVoid().flush();
		clz.generate();
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (stack.size() != 2) {
			throw new RuntimeException("I was expecting a stack depth of 2, not " + stack.size());
		}
		IExpr lhs = meth.as(stack.get(0), J.OBJECT);
		IExpr rhs = meth.as(stack.get(1), J.OBJECT);
		meth.callVirtual("void", runner, "assertSameValue", lhs, rhs).flush();
		stack.clear();
	}
	
	@Override
	public void visitContractDecl(ContractDecl cd) {
		String topName = cd.nameAsName().javaName();
		String upName = topName + "$Up";
		String downName = topName + "$Down";
		clz = bce.newClass(topName);
		upClz = bce.newClass(upName);
		downClz = bce.newClass(downName);

		clz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Up");
		upClz.generateAssociatedSourceFile();
		upClz.makeInterface();
		upClz.addInnerClassReference(Access.PUBLICSTATICINTERFACE, clz.getCreatedName(), "Up");
		upClz.implementsInterface(J.UP_CONTRACT);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		meth = clz.createMethod(false, J.OBJECT, cmd.name.name);
	}
	
	public static JVMGenerator forTests(MethodDefiner meth, IExpr runner) {
		return new JVMGenerator(meth, runner);
	}

	public static JVMGenerator forTests(ByteCodeSink bcc) {
		return new JVMGenerator(bcc);
	}
}
