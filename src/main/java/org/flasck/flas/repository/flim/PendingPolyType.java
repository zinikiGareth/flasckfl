package org.flasck.flas.repository.flim;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.PackageNameToken;

public class PendingPolyType implements PendingType {
	private final PackageNameToken pnt;

	public PendingPolyType(PackageNameToken ty) {
		this.pnt = ty;
	}

	public Type resolve(ErrorReporter errors, Repository repository) {
		return repository.get(this.pnt.text);
	}

	@Override
	public TypeReference resolveAsRef(ErrorReporter errors, Repository repository) {
		PolyType pt = (PolyType) resolve(errors, repository);
		TypeReference tr = new TypeReference(pnt.location, pnt.text);
		tr.bind(pt);
		return tr;
	}

}
