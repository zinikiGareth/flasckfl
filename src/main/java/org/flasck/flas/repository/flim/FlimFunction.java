package org.flasck.flas.repository.flim;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class FlimFunction implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final PendingFunction fn;

	public FlimFunction(ErrorReporter errors, Repository repository, PendingFunction fn) {
		this.errors = errors;
		this.repository = repository;
		this.fn = fn;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "arg": {
			PackageNameToken ty = PackageNameToken.from(toks);
			fn.arg(ty);
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		System.out.println("completed " + fn);
		fn.create(errors, repository);
	}
}
