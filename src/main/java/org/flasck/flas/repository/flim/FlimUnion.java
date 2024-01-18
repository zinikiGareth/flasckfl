package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class FlimUnion implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final SolidName tn;
	private final List<PendingType> members = new ArrayList<>();
	private final List<PolyType> polys = new ArrayList<>();
	private UnionTypeDefn ud;

	public FlimUnion(ErrorReporter errors, Repository repository, SolidName tn) {
		this.errors = errors;
		this.repository = repository;
		this.tn = tn;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		switch (kw.text) {
		case "member": {
			return new FlimTypeReader(errors) {
				@Override
				public void collect(PendingType ty) {
					members.add(ty);
				}
			};
		}
		case "poly": {
			PolyTypeToken ta = PolyTypeToken.from(errors, toks);
			polys.add(new PolyType(kw.location, new SolidName(tn, ta.text)));
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text + " at " + kw.location.file + " : " + kw.location.lineNo);
		}
	}
	
	private void create(InputPosition pos) {
		ud = new UnionTypeDefn(pos, false, tn, polys);
		repository.newUnion(errors, ud);
	}
	
	public void resolve() {
		for (PendingType um : members) {
			ud.cases.add(um.resolveAsRef(errors, repository, polys));
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

}
