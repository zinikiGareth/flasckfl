package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
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
	private PendingState state;
	private final List<PendingObjectCtor> ctors = new ArrayList<>();
	private final List<PendingObjectAcor> acors = new ArrayList<>();
	private final List<PendingObjectMethod> methods = new ArrayList<>();
	private final List<PolyType> polys = new ArrayList<>();
	private ObjectDefn od;

	public FlimObject(ErrorReporter errors, Repository repository, SolidName tn) {
		this.errors = errors;
		this.repository = repository;
		this.on = tn;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		switch (kw.text) {
		case "state": {
			state = new PendingState(errors, repository, polys);
			return state;
		}
		case "ctor": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingObjectCtor ctor = new PendingObjectCtor(errors, kw.location, FunctionName.objectCtor(kw.location, on, tok.text));
			ctors.add(ctor);
			return ctor;
		}
		case "acor": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingObjectAcor acor = new PendingObjectAcor(errors, FunctionName.function(kw.location, on, tok.text));
			acors.add(acor);
			return acor;
		}
		case "method": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			PendingObjectMethod meth = new PendingObjectMethod(errors, kw.location, FunctionName.objectMethod(kw.location, on, tok.text));
			methods.add(meth);
			return meth;
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
		if (state != null) {
			od.defineState(state.resolve());
		}
		for (PendingObjectCtor c : ctors) {
			ObjectCtor oc = c.resolve(errors, repository, od);
			od.addConstructor(oc);
		}
		for (PendingObjectAcor a : acors) {
			ObjectAccessor oa = a.resolve(errors, repository, od, polys);
			od.addAccessor(oa);
		}
		for (PendingObjectMethod m : methods) {
			ObjectMethod oc = m.resolve(errors, repository, od);
			od.addMethod(oc);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

}
