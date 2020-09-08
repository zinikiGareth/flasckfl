package org.flasck.flas.repository.flim;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.repository.LeafAdapter;
import org.zinutils.bytecode.mock.IndentWriter;

public class FlimVisitor extends LeafAdapter {
	private final String pkg;
	private final IndentWriter iw;

	public FlimVisitor(String pkg, IndentWriter iw) {
		this.pkg = pkg;
		this.iw = iw;
	}
	
	@Override
	public void visitStructDefn(StructDefn s) {
		if (s.name.uniqueName().startsWith(pkg))
			iw.println("struct " + s.name.uniqueName());
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.name().uniqueName().startsWith(pkg))
			iw.println("function " + fn.name().uniqueName());
	}
}
