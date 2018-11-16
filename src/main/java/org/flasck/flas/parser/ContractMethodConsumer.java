package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.ContractMethodDecl;

public interface ContractMethodConsumer {

	void addMethod(ContractMethodDecl method);

}
