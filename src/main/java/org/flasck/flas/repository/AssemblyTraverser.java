package org.flasck.flas.repository;

import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.parsedForm.assembly.Assembly;
import org.flasck.jvm.FLEvalContext;
import org.zinutils.exceptions.NotImplementedException;

public class AssemblyTraverser implements AssemblyVisitor {
	private final AssemblyVisitor v;

	public AssemblyTraverser(JSEnvironment jse, AssemblyVisitor v) {
		this.v = v;
	}

	public void doTraversal(Repository repository) {
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(e);
		traversalDone();
	}

	private void visitEntry(RepositoryEntry e) {
		if (e instanceof Assembly)
			visitAssembly((Assembly) e);
	}

	@Override
	public void visitAssembly(Assembly a) {
		v.visitAssembly(a);
	}

	@Override
	public void traversalDone() {
		v.traversalDone();
	}

	@Override
	public FLEvalContext getCreationContext() {
		throw new NotImplementedException();
	}
}
