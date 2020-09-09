package org.flasck.flas.repository.flim;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
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
		String pkn = figurePackageName(s.name().container());
		if (pkn != null) {
			iw.println("struct " + pkn + " " + s.name.baseName());
			sfw = iw.indent();
			for (PolyType v : s.polys())
				sfw.println("poly " + v);
		}
	}
	
	@Override
	public void leaveStructDefn(StructDefn s) {
		sfw = null;
	}
	
	@Override
	public void visitStructField(StructField sf) {
		if (sfw != null) {
			sfw.println("field " + sf.name);
			showType(sfw.indent(), sf.type());
		}
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		String pkn = figurePackageName(fn.name().container());
		if (pkn != null) {
			iw.println("function " + pkn + " " + fn.name().baseName());
			IndentWriter aiw = iw.indent();
			showType(aiw, fn.type());
		}
	}

	private String figurePackageName(NameOfThing container) {
		if (container == null || !(container instanceof PackageName))
			return null;
		PackageName pn = (PackageName) container;
		if (pn.uniqueName() == null && pkg != null)
			return null;
		else if (pn.uniqueName() != null && pkg == null)
			return null;
		else if (pkg != null && !pkg.equals(pn.uniqueName()))
			return null;
		if (pkg == null)
			return "null";
		else
			return pn.uniqueName();
	}

	private void showType(IndentWriter aiw, Type type) {
		if (type instanceof PolyType) {
			aiw.println("poly " + type);
		} else if (type instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) type;
			aiw.println("instance");
			IndentWriter piw = aiw.indent();
			showType(piw, pi.struct());
			for (Type pt : pi.polys())
				showType(piw, pt);
		} else if (type instanceof NamedType)
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
