package org.flasck.flas.method;

import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class ConvertRepositoryMethods extends LeafAdapter {
	private NestedVisitor sv;

	public ConvertRepositoryMethods(NestedVisitor sv) {
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitObjectMethod(ObjectMethod e) {
		sv.push(new MethodConvertor(sv));
	}
}
