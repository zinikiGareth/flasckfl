package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class FlimObject implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final SolidName on;
	private final List<PendingObjectCtor> ctors = new ArrayList<>();
	private final List<PendingObjectAcor> acors = new ArrayList<>();
	private final List<PolyType> polys = new ArrayList<>();
	private ObjectDefn od;

	public FlimObject(ErrorReporter errors, Repository repository, SolidName tn) {
		this.errors = errors;
		this.repository = repository;
		this.on = tn;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "ctor": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingObjectCtor ctor = new PendingObjectCtor(errors, kw.location, FunctionName.objectCtor(kw.location, on, tok.text));
			ctors.add(ctor);
			return ctor;
		}
		case "acor": {
			PendingObjectAcor acor = new PendingObjectAcor(errors);
			acors.add(acor);
			return acor;
		}
		case "poly": {
			PolyTypeToken ta = PolyTypeToken.from(toks);
			polys.add(new PolyType(kw.location, new SolidName(on, ta.text)));
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text + " at " + kw.location.file + " : " + kw.location.lineNo);
		}
	}
	
	private void create(InputPosition pos) {
		od = new ObjectDefn(pos, pos, on, false, polys);
		repository.newObject(errors, od);
	}
	
	public void resolve() {
		for (PendingObjectCtor c : ctors) {
			ObjectCtor oc = c.resolve(errors, repository, od);
			od.addConstructor(oc);
		}
		for (PendingObjectAcor a : acors) {
			a.resolve(errors, repository);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

}
