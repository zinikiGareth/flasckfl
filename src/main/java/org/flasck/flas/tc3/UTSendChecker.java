package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ContractImplementor;
import org.flasck.flas.parsedForm.ContractProvider;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class UTSendChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final UnitTestSend send;

	public UTSendChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, UnitTestSend s) {
		this.errors = errors;
		this.sv = sv;
		this.send = s;
		sv.push(this);
		// push the expression checker immediately to capture the unresolved var
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv));
	}

	@Override
	public void result(Object r) {
		PosType type = (PosType) r;
		Type ty = type.type;
		if (!(ty instanceof ContractImplementor) && !(ty instanceof ContractProvider)) {
			errors.message(send.card.location(), "cannot send contract messages to " + send.card.var);
			return;
		}
		boolean ok = false;
		NameOfThing cn = send.contract.defn().name();
		if (ty instanceof ContractImplementor && ((ContractImplementor)ty).implementsContract(cn))
			ok = true;
		if (ty instanceof ContractProvider && ((ContractProvider)ty).providesContract(cn))
			ok = true;
		if (!ok) {
			errors.message(send.card.location(), send.card.var + " does not offer contract " + cn);
			return;
		}
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		sv.result(null);
	}

}
