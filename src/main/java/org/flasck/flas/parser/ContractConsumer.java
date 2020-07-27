package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;

public interface ContractConsumer {
	void newContractMethod(ErrorReporter errors, ContractMethodDecl decl);
}
