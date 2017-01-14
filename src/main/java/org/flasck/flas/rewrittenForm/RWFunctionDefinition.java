package org.flasck.flas.rewrittenForm;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class RWFunctionDefinition implements NamedThing, Comparable<RWFunctionDefinition> {
	public final InputPosition location;
	public final CodeType mytype;
	public final FunctionName fnName;
	public final int nargs;
	public final CardName inCard;
	public final List<RWFunctionCaseDefn> cases = new ArrayList<>();
	public final boolean generate;
	private Type type;
	public final Set<ScopedVar> scopedVars = new TreeSet<ScopedVar>();

	public RWFunctionDefinition(FunctionName name, int nargs, boolean generate) {
		this.inCard = name.containingCard();
		if (name.location == null)
			throw new UtilException("Null location");
		this.location = name.location;
		this.mytype = name.codeType;
		this.fnName = name;
		this.nargs = nargs;
		this.generate = generate;
		if (mytype == null)
			throw new UtilException("Null mytype");
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public CardName inCard() {
		return fnName.containingCard();
	}
	
	@Override
	public NameOfThing getName() {
		return fnName;
	}
	
	public String uniqueName() {
		return fnName.uniqueName();
	}
	
	public int nargs() {
		return nargs;
	}

	public int nextCase() {
		return cases.size();
	}
	
	public void addCase(RWFunctionCaseDefn fcase) {
		if (fcase == null)
			throw new UtilException("Cannot add null case to fn");
		cases.add(fcase);
	}

	public void addCases(List<RWFunctionCaseDefn> cases) {
		this.cases.addAll(cases);
	}

	public void gatherScopedVars() {
		for (RWFunctionCaseDefn c : cases) {
			c.gatherScopedVars(scopedVars);
		}
	}

	public void dumpTo(Writer pw) throws Exception {
		pw.append(mytype + " " + uniqueName() + "/" + nargs + " {\n");
		for (RWFunctionCaseDefn fcd : cases)
			fcd.dumpTo(pw);
		pw.append("}\n");
		pw.flush();
	}
	
	@Override
	public int compareTo(RWFunctionDefinition o) {
		return fnName.compareTo(o.fnName);
	}
	
	@Override
	public String toString() {
		return uniqueName() +"/" + nargs+"["+cases.size()+"]";
	}

	public void setType(Type ty) {
		this.type = ty;
	}

	public Type getType() {
		return type;
	}
}
