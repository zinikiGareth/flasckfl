package org.flasck.flas.repository.flim;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.Repository;

public class PendingContractArg {
	private final PendingType ty;
	private final InputPosition location;
	private final String text;

	public PendingContractArg(PendingType ty, InputPosition location, String text) {
		this.ty = ty;
		this.location = location;
		this.text = text;
	}

	public TypedPattern resolve(ErrorReporter errors, Repository repository, NameOfThing cmn) {
		return new TypedPattern(location, ty.resolveAsRef(errors, repository, new ArrayList<>()), new VarName(location, cmn, text));
	}

}
