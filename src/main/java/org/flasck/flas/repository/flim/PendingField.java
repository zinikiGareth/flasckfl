package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsHolder;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.zinutils.exceptions.CantHappenException;

public class PendingField extends FlimTypeReader {
	private final ValidIdentifierToken tok;
	private final List<PendingType> tys = new ArrayList<>();

	public PendingField(ErrorReporter errors, ValidIdentifierToken tok) {
		super(errors);
		this.tok = tok;
	}

	@Override
	public void collect(PendingType ty) {
		tys.add(ty);
	}

	public StructField resolve(Repository repository, FieldsHolder parent) {
		if (tys.size() != 1)
			throw new CantHappenException("there should be exactly one type here");
		TypeReference tr = tys.get(0).resolveAsRef(errors, repository);
		return new StructField(tok.location, tok.location, parent, true, tr, tok.text, null);
	}
}
