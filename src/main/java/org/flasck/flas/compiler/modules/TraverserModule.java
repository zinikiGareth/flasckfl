package org.flasck.flas.compiler.modules;

import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.Traverser;

public interface TraverserModule {

	boolean visitEntry(Traverser traverser, RepositoryVisitor visitor, RepositoryEntry e);
	boolean visitUnitTestStep(Traverser traverser, RepositoryVisitor visitor, UnitTestStep s);

}
