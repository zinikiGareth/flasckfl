package org.flasck.flas.compiler.modules;

import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.Traverser;

public interface TraverserModule {

	boolean visitUnitTestStep(Traverser traverser, RepositoryVisitor visitor, UnitTestStep s);

}
