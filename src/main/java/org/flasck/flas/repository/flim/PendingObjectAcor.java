package org.flasck.flas.repository.flim;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.repository.Repository;

public class PendingObjectAcor extends FlimTypeReader {
	private PendingType type;

	public PendingObjectAcor(ErrorReporter errors) {
		super(errors);
	}

	@Override
	public void collect(PendingType ty) {
		this.type = ty;
	}
	
	public ObjectCtor resolve(ErrorReporter errors, Repository repository) {
		return null;
	}
}
