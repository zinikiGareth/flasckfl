package org.flasck.flas.repository.flim;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public abstract class FlimTypeReader implements TDAParsing {
	protected final ErrorReporter errors;

	public FlimTypeReader(ErrorReporter errors) {
		this.errors = errors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		return tryWith(toks, kw);
	}

	protected TDAParsing tryWith(Tokenizable toks, KeywordToken kw) {
		switch (kw.text) {
		case "apply": {
			return new FlimApplyReader(errors, this);
		}
		case "instance": {
			return new FlimInstanceReader(errors, this);
		}
		case "named": {
			PackageNameToken ty = PackageNameToken.from(toks);
			collect(new PendingNamedType(ty));
			return new NoNestingParser(errors);
		}
		case "poly": {
			PackageNameToken ty = PackageNameToken.from(toks);
			collect(new PendingPolyType(ty));
			return new NoNestingParser(errors);
		}
		case "var": {
			PolyTypeToken ty = PolyTypeToken.from(toks);
			definePolyVar(ty.location, ty.text);
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text);
		}
	}
	
	protected void definePolyVar(InputPosition location, String text) {
		throw new NotImplementedException("not defined here but in subclass");
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
	
	public abstract void collect(PendingType ty);
}
