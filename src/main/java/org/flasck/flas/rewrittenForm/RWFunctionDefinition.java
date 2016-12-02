package org.flasck.flas.rewrittenForm;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class RWFunctionDefinition implements Locatable, Comparable<RWFunctionDefinition> {
	public final InputPosition location;
	public final CodeType mytype;
	public final String name;
	public final int nargs;
	public final List<RWFunctionCaseDefn> cases = new ArrayList<>();
	public final boolean generate;
	private Type type;

	public RWFunctionDefinition(InputPosition location, CodeType mytype, String name, int nargs, boolean generate) {
		if (location == null)
			throw new UtilException("Null location");
		this.location = location;
		this.mytype = mytype;
		this.name = name;
		this.nargs = nargs;
		this.generate = generate;
		if (mytype == null)
			throw new UtilException("Null mytype");
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public String name() {
		return name;
	}
	
	public int nargs() {
		return nargs;
	}

	public void dumpTo(Writer pw) throws Exception {
		pw.append(mytype + " " + name + "/" + nargs + " {\n");
		for (RWFunctionCaseDefn fcd : cases)
			fcd.dumpTo(pw);
		pw.append("}\n");
		pw.flush();
	}
	
	@Override
	public int compareTo(RWFunctionDefinition o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name +"/" + nargs+"["+cases.size()+"]";
	}

	public void setType(Type ty) {
		this.type = ty;
	}

	public Type getType() {
		return type;
	}
}
