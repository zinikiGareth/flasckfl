package org.flasck.flas.parsedForm;

import java.io.Writer;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;

public class FunctionDefinition {
	public final Type mytype;
	public final String name;
	public final int nargs;
	public final List<FunctionCaseDefn> cases;
//	private final Scope scope;

	public FunctionDefinition(Type mytype, String name, int nargs, List<FunctionCaseDefn> list) {
		this.mytype = mytype;
		this.name = name;
		this.nargs = nargs;
		this.cases = list;
//		for (int i=0;i<list.size()-1;i++)
//			if (list.get(i).innerScope().size() > 0)
//				throw new UtilException("Can only attach nested definitions to last case");
//		this.scope = list.get(list.size()-1).innerScope();
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
