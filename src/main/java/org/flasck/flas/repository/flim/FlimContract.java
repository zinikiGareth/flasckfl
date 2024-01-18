package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class FlimContract implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final SolidName name;
	private final ContractType type;
	private final List<FlimContractMethod> methods = new ArrayList<>();
	private ContractDecl cd;

	public FlimContract(ErrorReporter errors, Repository repository, SolidName name, ContractType type) {
		this.errors = errors;
		this.repository = repository;
		this.name = name;
		this.type = type;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		switch (kw.text) {
		case "method": {
			ValidIdentifierToken tok = VarNameToken.from(errors, toks);
			ValidIdentifierToken reqd = VarNameToken.from(errors, toks);
			FlimContractMethod ret = new FlimContractMethod(errors, repository, name, tok.location, tok.text, Boolean.parseBoolean(reqd.text));
			methods.add(ret);
			return ret;
		}
		default:
			throw new NotImplementedException("cannot handle flim keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(location);
	}

	private void create(InputPosition location) {
		cd = new ContractDecl(location, location, type, name, false);
		repository.newContract(errors, cd);
	}

	public void resolveMethods() {
		for (FlimContractMethod fcm : methods) {
			cd.addMethod(fcm.resolve(errors, repository));
		}
	}

}
