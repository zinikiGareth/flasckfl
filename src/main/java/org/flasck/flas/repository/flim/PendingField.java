package org.flasck.flas.repository.flim;

import java.util.ArrayList;

import org.flasck.flas.parsedForm.FieldsHolder;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class PendingField {
	private final PackageNameToken ty;
	private final ValidIdentifierToken tok;
	private final ArrayList<TypeReference> ftpolys = new ArrayList<>();

	public PendingField(PackageNameToken ty, ValidIdentifierToken tok) {
		this.ty = ty;
		this.tok = tok;
	}

	public StructField resolve(FieldsHolder parent) {
		TypeReference tr = new TypeReference(ty.location, ty.text, ftpolys);
		return new StructField(tok.location, tok.location, parent, true, tr, tok.text, null);
	}

}
