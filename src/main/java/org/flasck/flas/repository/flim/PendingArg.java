package org.flasck.flas.repository.flim;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.PackageNameToken;

public class PendingArg {
	private final PackageNameToken ty;

	public PendingArg(PackageNameToken ty) {
		this.ty = ty;
	}

	public Type resolve(ErrorReporter errors, Repository repository) {
		RepositoryEntry ret = repository.get(ty.text);
		if (ret == null) {
			errors.message(ty.location, "no such type " + ty.text);
			return new ErrorType();
		}
		if (!(ret instanceof Type)) {
			errors.message(ty.location, ty.text + " is not a type name");
			return new ErrorType();
		}
		return (Type) ret;
	}

}
