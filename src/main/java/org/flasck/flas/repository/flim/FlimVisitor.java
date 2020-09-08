package org.flasck.flas.repository.flim;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class FlimVisitor extends LeafAdapter {
	private final String pkg;
	private final IndentWriter iw;
	private IndentWriter sfw;

	public FlimVisitor(String pkg, IndentWriter iw) {
		this.pkg = pkg;
		this.iw = iw;
	}
	
	@Override
	public void visitStructDefn(StructDefn s) {
		if (s.name.uniqueName().startsWith(pkg)) {
			iw.println("struct " + s.name.uniqueName());
			sfw = iw.indent();
		}
	}
	
	@Override
	public void leaveStructDefn(StructDefn s) {
		sfw = null;
	}
	
	@Override
	public void visitStructField(StructField sf) {
		if (sfw != null) {
			sfw.println("field " + sf.type.defn().name().uniqueName() + " " + sf.name);
		}
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.name().uniqueName().startsWith(pkg)) {
			iw.println("function " + fn.name().uniqueName());
			IndentWriter aiw = iw.indent();
			for (int i=0;i<=fn.argCount();i++) {
				aiw.print("arg ");
				showType(aiw, fn.type().get(i));
				aiw.println("");
			}
		}
	}

	private void showType(IndentWriter aiw, Type type) {
		if (type instanceof NamedType)
			aiw.print(((NamedType)type).signature());
		else
			throw new NotImplementedException("cannot handle " + type);
	}
}
