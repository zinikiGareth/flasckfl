package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class PendingState implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final List<PendingField> fields = new ArrayList<>();
	private final List<PolyType> polys;
	private StateDefinition sd;

	public PendingState(ErrorReporter errors, Repository repository, List<PolyType> polys) {
		this.errors = errors;
		this.repository = repository;
		this.polys = polys;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "member": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingField f = new PendingField(errors, tok);
			fields.add(f);
			return f;
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text);
		}
	}
	
	private void create(InputPosition pos) {
		sd = new StateDefinition(pos);
	}
	
	public StateDefinition resolve() {
		for (PendingField pf : fields) {
			StructField sf = pf.resolve(repository, sd, polys);
			sf.fullName(new VarName(sf.loc, sd.name(), sf.name));
			sd.addField(sf);
		}
		return sd;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

}
