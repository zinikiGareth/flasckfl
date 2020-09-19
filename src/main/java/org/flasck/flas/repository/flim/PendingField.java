package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.zinutils.exceptions.CantHappenException;

public class PendingField extends FlimTypeReader {
	private final ValidIdentifierToken tok;
	private final List<PendingType> tys = new ArrayList<>();
	private boolean hasInit;

	public PendingField(ErrorReporter errors, ValidIdentifierToken tok) {
		super(errors);
		this.tok = tok;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if ("init".equals(kw.text)) {
			KeywordToken iw = KeywordToken.from(toks);
			if (iw != null)
				hasInit = Boolean.parseBoolean(iw.text);
			return new NoNestingParser(errors);
		} else
			return tryWith(toks, kw);
	}

	@Override
	public void collect(PendingType ty) {
		tys.add(ty);
	}

	public StructField resolve(Repository repository, FieldsHolder parent, List<PolyType> polys) {
		if (tys.size() != 1)
			throw new CantHappenException("there should be exactly one type here");
		TypeReference tr = tys.get(0).resolveAsRef(errors, repository, polys);
		return new StructField(tok.location, tok.location, parent, true, tr, tok.text, hasInit?new StringLiteral(tok.location, "initialized"):null);
	}
}
