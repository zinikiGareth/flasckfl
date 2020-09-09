package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PackageNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class FlimTop implements TDAParsing {
	private final ErrorReporter errors;
	private final Repository repository;
	private final String pkg;
	private final List<FlimFunction> functions = new ArrayList<>();
	private final List<FlimStruct> structs = new ArrayList<>();

	public FlimTop(ErrorReporter errors, Repository repository, String pkg) {
		this.errors = errors;
		this.repository = repository;
		this.pkg = pkg;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		KeywordToken kw = KeywordToken.from(toks);
		NameOfThing container;
		if (pkg == null) {
			container = new PackageName(null);
		} else {
			PackageNameToken inpkg = PackageNameToken.from(toks);
			if (pkg == null && "null".equals(inpkg))
				container = new PackageName(null);
			else if (inpkg.text.equals(pkg)) {
				container = new PackageName(pkg);
			} else {
				errors.message(pos, "invalid package name");
				return new IgnoreNestedParser();
			}
		}
		switch (kw.text) {
		case "struct": {
			TypeNameToken tnt = TypeNameToken.unqualified(toks);
			SolidName tn = new SolidName(container, tnt.text);
			FlimStruct fs = new FlimStruct(errors, repository, tn);
			structs.add(fs);
			return fs;
		}
		case "function": {
			ValidIdentifierToken ft = VarNameToken.from(toks);
			FlimFunction pf = new FlimFunction(errors, repository, FunctionName.function(pos, container, ft.text));
			functions.add(pf);
			return pf;
		}
		default:
			throw new NotImplementedException("cannot handle flim keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		for (FlimStruct ps : structs)
			ps.resolve();
		for (FlimFunction pf : functions)
			pf.bindType();
	}
}
