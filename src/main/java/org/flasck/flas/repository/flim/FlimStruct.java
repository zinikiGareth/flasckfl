package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class FlimStruct implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final SolidName tn;
	private final List<PendingField> fields = new ArrayList<>();
	private final List<PolyType> polys = new ArrayList<>();
	private StructDefn sd;

	public FlimStruct(ErrorReporter errors, Repository repository, SolidName tn) {
		this.errors = errors;
		this.repository = repository;
		this.tn = tn;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "field": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingField f = new PendingField(errors, tok);
			fields.add(f);
			return f;
		}
		case "poly": {
			PolyTypeToken ta = PolyTypeToken.from(toks);
			polys.add(new PolyType(kw.location, new SolidName(tn, ta.text)));
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text);
		}
	}
	
	private void create(InputPosition pos) {
		sd = new StructDefn(pos, pos, FieldsType.STRUCT, tn, false, polys);
		repository.newStruct(errors, sd);
	}
	
	public void resolve() {
		for (PendingField pf : fields) {
			StructField sf = pf.resolve(repository, sd, polys);
			sf.fullName(new VarName(sf.loc, sd.name(), sf.name));
			sd.addField(sf);
			repository.newStructField(errors, sf);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

}
