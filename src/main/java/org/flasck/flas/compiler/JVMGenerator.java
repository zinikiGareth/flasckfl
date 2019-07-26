package org.flasck.flas.compiler;

import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.bytecode.ByteCodeEnvironment;

public class JVMGenerator extends LeafAdapter {
	private final ByteCodeEnvironment bce;

	public JVMGenerator(ByteCodeEnvironment bce) {
		this.bce = bce;
	}
	
	@Override
	public void visitUnitTest(UnitTestCase e) {
		String clzName = e.name.javaName();
		System.out.println("Yo! " + clzName);
		bce.newClass(clzName);
	}
}
