package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
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
	private final List<FlimContract> contracts = new ArrayList<>();
	private final List<FlimFunction> functions = new ArrayList<>();
	private final List<FlimStruct> structs = new ArrayList<>();
	private final List<FlimUnion> unions = new ArrayList<>();
	private final List<FlimObject> objects = new ArrayList<>();
	public final Set<String> uses = new TreeSet<>();
	private final Iterable<ReadFlimModule> modules;

	public FlimTop(ErrorReporter errors, Repository repository, String pkg) {
		this.modules = ServiceLoader.load(ReadFlimModule.class);
		this.errors = errors;
		this.repository = repository;
		this.pkg = pkg;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		KeywordToken kw = KeywordToken.from(errors, toks);
		NameOfThing container;
		PackageNameToken inpkg = PackageNameToken.from(errors, toks);
		if (kw.text.equals("usespackage")){
			uses.add(inpkg.text);
			return new NoNestingParser(errors);
		}
		if (pkg.equals("root.package") && "null".equals(inpkg.text))
			container = new PackageName(false);
		else if (inpkg.text.equals(pkg)) {
			container = new PackageName(pkg);
		} else {
			errors.message(inpkg.location, "invalid package name");
			return new IgnoreNestedParser(errors);
		}
		switch (kw.text) {
		case "contract": {
			ContractType ct = ContractType.valueOf(VarNameToken.from(errors, toks).text.toUpperCase());
			TypeNameToken tnt = TypeNameToken.unqualified(errors, toks);
			SolidName cn = new SolidName(container, tnt.text);
			FlimContract fc = new FlimContract(errors, repository, cn, ct);
			contracts.add(fc);
			return fc;
		}
		case "struct": {
			TypeNameToken tnt = TypeNameToken.unqualified(errors, toks);
			SolidName tn = new SolidName(container, tnt.text);
			FlimStruct fs = new FlimStruct(errors, repository, tn);
			structs.add(fs);
			return fs;
		}
		case "union": {
			TypeNameToken tnt = TypeNameToken.unqualified(errors, toks);
			SolidName tn = new SolidName(container, tnt.text);
			FlimUnion fs = new FlimUnion(errors, repository, tn);
			unions.add(fs);
			return fs;
		}
		case "function": {
			ValidIdentifierToken ft = VarNameToken.from(errors, toks);
			FlimFunction pf = new FlimFunction(errors, repository, FunctionName.function(pos, container, ft.text));
			functions.add(pf);
			return pf;
		}
		case "object": {
			TypeNameToken tnt = TypeNameToken.unqualified(errors, toks);
			ObjectName on = new ObjectName(container, tnt.text);
			FlimObject fs = new FlimObject(errors, repository, on);
			objects.add(fs);
			return fs;
		}
		default:
			for (ReadFlimModule m : modules) {
				TDAParsing inner = null;
				if ((inner = m.readLine(errors, repository, kw, container, toks)) != null)
					return inner;
			}
			throw new NotImplementedException("cannot handle flim keyword " + kw.text);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
	
	public void resolve() {
		for (FlimStruct ps : structs)
			ps.resolve();
		for (FlimUnion ps : unions)
			ps.resolve();
		for (FlimFunction pf : functions)
			pf.bindType();
		for (FlimContract fc : contracts)
			fc.resolveMethods();
		for (FlimObject fc : objects)
			fc.resolve();
	}

	public String pkgName() {
		return pkg;
	}
}
