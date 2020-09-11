package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.Repository;

public class FlimContractMethod extends PendingMethod {
	private final InputPosition loc;
	private final boolean required;
	private ContractMethodDecl cmd;
	private FunctionName cmn;

	public FlimContractMethod(ErrorReporter errors, Repository repository, SolidName cn, InputPosition loc, String name, boolean required) {
		super(errors);
		this.loc = loc;
		this.required = required;
		this.cmn = FunctionName.contractMethod(loc, cn, name);
	}

	public ContractMethodDecl resolve(ErrorReporter errors, Repository repository) {
		List<TypedPattern> ta = new ArrayList<>();
		for (PendingContractArg a : args)
			ta.add(a.resolve(errors, repository, cmn));
		cmd = new ContractMethodDecl(loc, loc, loc, required, cmn, ta, handler == null ? null : handler.resolve(errors, repository, cmn));
		cmd.bindType();
		repository.newContractMethod(errors, cmd);
		return cmd;
	}
}
