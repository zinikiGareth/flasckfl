package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public abstract class PendingMethod implements TDAParsing {
	private final ErrorReporter errors;
	protected final List<PendingContractArg> args = new ArrayList<>();
	protected PendingContractArg handler;

	public PendingMethod(ErrorReporter errors) {
		this.errors = errors;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "arg": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			return new FlimTypeReader(errors) {
				@Override
				public void collect(PendingType ty) {
					args.add(new PendingContractArg(ty, tok.location, tok.text));
				}
			};
		}
		case "handler": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			return new FlimTypeReader(errors) {
				@Override
				public void collect(PendingType ty) {
					handler = new PendingContractArg(ty, tok.location, tok.text);
				}
			};
		}
		default:
			throw new NotImplementedException("cannot handle flim keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
