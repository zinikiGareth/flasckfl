package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.ut.UnitTestStep;

public interface TraverserModule {

	boolean visitUnitTestStep(Traverser traverser, RepositoryVisitor visitor, UnitTestStep s);

}
