package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.bytecode.ByteCodeCreator;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeEnvironment bce;

	public JVMGenerator(ByteCodeEnvironment bce) {
		this.bce = bce;
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		System.out.println("Yo! " + clzName);
		ByteCodeCreator clz = bce.newClass(clzName);
		GenericAnnotator ann = GenericAnnotator.newMethod(clz, true, "dotest");
		ann.argument("org.flasck.flas.testrunner.JVMRunner", "runner");
		ann.returns(JavaType.void_);
		NewMethodDefiner meth = ann.done();
		meth.returnVoid().flush();
		clz.generate();
	}
}
