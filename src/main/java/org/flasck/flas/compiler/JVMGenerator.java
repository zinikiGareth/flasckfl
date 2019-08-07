package org.flasck.flas.compiler;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeStorage bce;
	private MethodDefiner meth;
	private List<IExpr> stack = new ArrayList<IExpr>();
	private Var runner;

	public JVMGenerator(ByteCodeStorage bce) {
		this.bce = bce;
	}
	
	private JVMGenerator(MethodDefiner meth) {
		this.bce = null;
		this.meth = meth;
	}

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		Object val = expr.value();
		if (val instanceof Integer)
			stack.add(meth.intConst((int) val));
		else
			throw new NotImplementedException();
	}
	
	@Override
	public void visitStringLiteral(StringLiteral expr) {
		stack.add(meth.stringConst(expr.text));
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		ByteCodeSink clz = bce.newClass(clzName);
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		PendingVar runner = ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		ann.returns(JavaType.void_);
		meth = ann.done();
		this.runner = runner.getVar();
		
		// This needs to move to postUnitTest
		meth.returnVoid().flush();
		clz.generate();
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		if (stack.size() != 2) {
			throw new RuntimeException("I was expecting a stack depth of 2, not " + stack.size());
		}
		meth.callVirtual("void", runner, "assertSameValue", stack.toArray(new IExpr[2]));
		stack.clear();
	}
	
	public static JVMGenerator forTests(MethodDefiner meth) {
		return new JVMGenerator(meth);
	}
}
