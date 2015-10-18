package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class FunctionDefinition implements Locatable, Serializable {
	public final InputPosition location;
	public final CodeType mytype;
	public final String name;
	public final int nargs;
	public final List<FunctionCaseDefn> cases;

	public FunctionDefinition(InputPosition location, CodeType mytype, String name, int nargs, List<FunctionCaseDefn> list) {
		this.location = location;
		if (mytype == null)
			throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.name = name;
		this.nargs = nargs;
		this.cases = list;
	}

	public FunctionDefinition(InputPosition location, CodeType mytype, FunctionIntro intro, List<FunctionCaseDefn> list) {
		this(location, mytype, intro.name, intro.args.size(), list);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void dumpTo(Writer pw) throws Exception {
		pw.append(name + " {\n");
		for (FunctionCaseDefn fcd : cases)
			fcd.dumpTo(pw);
		pw.append("}\n");
		pw.flush();
	}
	
	@Override
	public String toString() {
		return name +"/" +nargs+"["+cases.size()+"]";
	}
}
