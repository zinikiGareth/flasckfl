package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class RWFunctionDefinition implements Locatable, Serializable {
	public final InputPosition location;
	public final CodeType mytype;
	public final String name;
	public final int nargs;
	public final Map<String, LocalVar> vars = new HashMap<>();
	public final List<RWFunctionCaseDefn> cases = new ArrayList<>();
	public final boolean generate;

	public RWFunctionDefinition(InputPosition location, CodeType mytype, String name, int nargs, boolean generate) {
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
	public String toString() {
		return name +"/" + nargs+"["+cases.size()+"]";
	}
}
