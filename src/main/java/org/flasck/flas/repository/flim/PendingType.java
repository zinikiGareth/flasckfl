package org.flasck.flas.repository.flim;

import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Type;

public interface PendingType {
	Type resolve(ErrorReporter errors, Repository repository, List<PolyType> polys);
	TypeReference resolveAsRef(ErrorReporter errors, Repository repository, List<PolyType> polys);
}
