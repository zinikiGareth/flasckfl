package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.PackageNameToken;

public class PendingFunction {
	private final FunctionName fn;
	private final List<PendingArg> args = new ArrayList<>();
	private StateHolder holder;
	private FunctionDefinition fd;

	public PendingFunction(FunctionName fn) {
		this.fn = fn;
	}

	public void create(ErrorReporter errors, Repository repository) {
		fd = new FunctionDefinition(fn, args.size(), holder);
		fd.dontGenerate();
		repository.functionDefn(errors, fd);
	}
	
	public void bindType(ErrorReporter errors, Repository repository) {
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
	
	public void arg(PackageNameToken ty) {
		args.add(new PendingArg(ty));
	}
	
	@Override
	public String toString() {
		return "Function[" + fn.uniqueName() + "]";
	}
}
