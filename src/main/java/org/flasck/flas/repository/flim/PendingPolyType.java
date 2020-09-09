package org.flasck.flas.repository.flim;

import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.zinutils.exceptions.CantHappenException;

public class PendingPolyType implements PendingType {
	private final PackageNameToken pnt;

	public PendingPolyType(PackageNameToken ty) {
		this.pnt = ty;
	}

	public Type resolve(ErrorReporter errors, Repository repository, List<PolyType> polys) {
		for (PolyType pt : polys) {
			if (pt.shortName().equals(pnt.text))
				return pt;
		}
		throw new CantHappenException("could not find " + pnt.text + " in " + polys);
	}

	@Override
	public TypeReference resolveAsRef(ErrorReporter errors, Repository repository, List<PolyType> polys) {
		PolyType pt = (PolyType) resolve(errors, repository, polys);
		TypeReference tr = new TypeReference(pnt.location, pnt.text);
		tr.bind(pt);
		return tr;
	}

}
