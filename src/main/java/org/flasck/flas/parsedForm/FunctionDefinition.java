package org.flasck.flas.parsedForm;

import java.io.Writer;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.zinutils.exceptions.UtilException;

public class FunctionDefinition {
	public final Type mytype;
	public final String name;
	public final int nargs;
	public final List<FunctionCaseDefn> cases;

	public FunctionDefinition(Type mytype, String name, int nargs, List<FunctionCaseDefn> list) {
		if (mytype == null)
			throw new UtilException("Null mytype");
		this.mytype = mytype;
		this.name = name;
		this.nargs = nargs;
		this.cases = list;
	}

	public FunctionDefinition(Type mytype, FunctionIntro intro, List<FunctionCaseDefn> list) {
		this(mytype, intro.name, intro.args.size(), list);
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
