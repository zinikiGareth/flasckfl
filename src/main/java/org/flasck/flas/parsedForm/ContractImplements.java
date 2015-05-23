package org.flasck.flas.parsedForm;

public class ContractImplements extends Implements {
	public final String referAsVar;

	public ContractImplements(String type, String referAsVar) {
		super(type);
		this.referAsVar = referAsVar;
	}
}
