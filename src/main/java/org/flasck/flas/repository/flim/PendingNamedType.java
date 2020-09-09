package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.PackageNameToken;

public class PendingNamedType implements PendingType {
	private final PackageNameToken pnt;

	public PendingNamedType(PackageNameToken ty) {
		this.pnt = ty;
	}

	public Type resolve(ErrorReporter errors, Repository repository) {
		RepositoryEntry ret = repository.get(pnt.text);
		if (ret == null) {
			errors.message(pnt.location, "no such type " + pnt.text);
			return new ErrorType();
		}
		if (!(ret instanceof Type)) {
			errors.message(pnt.location, pnt.text + " is not a type name");
			return new ErrorType();
		}
		return (Type) ret;
	}

	@Override
	public TypeReference resolveAsRef(ErrorReporter errors, Repository repository) {
		NamedType ty = (NamedType) resolve(errors, repository);
		List<TypeReference> ftpolys = new ArrayList<>();
		TypeReference tr = new TypeReference(this.pnt.location, ty.name().baseName(), ftpolys);
		return tr;
	}

}
