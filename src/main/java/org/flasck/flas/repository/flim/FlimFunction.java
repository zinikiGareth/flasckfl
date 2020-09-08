package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class FlimFunction implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final FunctionName fn;
	private final List<PendingArg> args = new ArrayList<>();
	private StateHolder holder;
	private FunctionDefinition fd;

	public FlimFunction(ErrorReporter errors, Repository repository, FunctionName fn) {
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
			args.add(new PendingArg(ty));
			return new NoNestingParser(errors);
		}
		default:
			throw new NotImplementedException("cannot handle flim field keyword " + kw.text);
		}
	}

	public void create(ErrorReporter errors, Repository repository) {
		fd = new FunctionDefinition(fn, args.size(), holder);
		fd.dontGenerate();
		repository.functionDefn(errors, fd);
	}

	public void bindType() {
		if (args.size() == 1) {
			fd.bindType(args.get(0).resolve(errors, repository));
		} else {
			List<Type> as = new ArrayList<>();
			for (PendingArg pa : args) {
				as.add(pa.resolve(errors, repository));
			}
			fd.bindType(new Apply(as));
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		create(errors, repository);
	}
}
