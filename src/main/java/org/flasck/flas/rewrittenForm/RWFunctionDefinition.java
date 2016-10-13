package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class RWFunctionDefinition implements Locatable, Serializable {
	public final InputPosition location;
	public final CodeType mytype;
	public final RWFunctionIntro intro;
	public final List<RWFunctionCaseDefn> cases;
	public final boolean generate;

	public RWFunctionDefinition(InputPosition location, CodeType mytype, RWFunctionIntro intro, List<RWFunctionCaseDefn> list, boolean generate) {
		this.location = location;
		this.mytype = mytype;
		this.intro = intro;
		this.generate = generate;
		if (mytype == null)
			throw new UtilException("Null mytype");
		this.cases = list;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public String name() {
		return intro.name;
	}
	
	public int nargs() {
		return intro.args.size();
	}

	public void dumpTo(Writer pw) throws Exception {
		pw.append(intro.name + " {\n");
		for (RWFunctionCaseDefn fcd : cases)
			fcd.dumpTo(pw);
		pw.append("}\n");
		pw.flush();
	}
	
	@Override
	public String toString() {
		return intro.name +"/" +intro.args.size()+"["+cases.size()+"]";
	}
}
