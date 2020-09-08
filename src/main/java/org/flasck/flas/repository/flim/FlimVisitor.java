package org.flasck.flas.repository.flim;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.tc3.Apply;
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
		if (s.name.container() instanceof PackageName && s.name.container().uniqueName().equals(pkg)) {
			iw.println("struct " + s.name.container().uniqueName() + " " + s.name.baseName());
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
		if (fn.name().container() != null && fn.name().container() instanceof PackageName) {
			PackageName pn = (PackageName) fn.name().container();
			if (pn.uniqueName() == null && pkg != null)
				return;
			else if (pn.uniqueName() != null && pkg == null)
				return;
			else if (pkg != null && !pkg.equals(pn.uniqueName()))
				return;
			iw.println("function " + fn.name().container().uniqueName() + " " + fn.name().baseName());
			IndentWriter aiw = iw.indent();
			showType(aiw, fn.type());
		}
	}

	private void showType(IndentWriter aiw, Type type) {
		if (type instanceof NamedType)
			aiw.println("named " + ((NamedType)type).signature());
		else if (type instanceof Apply) {
			aiw.println("apply");
			IndentWriter iiw = aiw.indent();
			Apply ty = (Apply) type;
			for (int i=0;i<=ty.argCount();i++)
				showType(iiw, ty.get(i));
		} else
			throw new NotImplementedException("cannot handle " + type);
	}
}
