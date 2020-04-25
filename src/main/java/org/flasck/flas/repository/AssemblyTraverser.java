package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.assembly.Assembly;

public class AssemblyTraverser {
	private final AssemblyVisitor v;

	public AssemblyTraverser(AssemblyVisitor v) {
		this.v = v;
	}

	public void doTraversal(Repository repository) {
		for (RepositoryEntry e : repository.dict.values())
			visitEntry(e);
		v.traversalDone();
	}

	private void visitEntry(RepositoryEntry e) {
		if (e instanceof Assembly)
			v.visitAssembly((Assembly) e);
	}

}
