package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class FlimContractMethod implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final SolidName cn;
	private final InputPosition loc;
	private final String name;
	private final boolean required;
	private final List<PendingContractArg> args = new ArrayList<>();
	ContractMethodDecl cmd;

	public FlimContractMethod(ErrorReporter errors, Repository repository, SolidName cn, InputPosition loc, String name, boolean required) {
		this.errors = errors;
		this.repository = repository;
		this.cn = cn;
		this.loc = loc;
		this.name = name;
		this.required = required;
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
		default:
			throw new NotImplementedException("cannot handle flim keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		List<TypedPattern> ta = new ArrayList<>();
		TypedPattern th = null;
		cmd = new ContractMethodDecl(loc, loc, loc, required, FunctionName.contractMethod(loc, cn, name), ta, th);
		repository.newContractMethod(errors, cmd);
	}

}
