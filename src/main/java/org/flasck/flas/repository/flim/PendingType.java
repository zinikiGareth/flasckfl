package org.flasck.flas.repository.flim;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Type;

public interface PendingType {
	Type resolve(ErrorReporter errors, Repository repository);
}
