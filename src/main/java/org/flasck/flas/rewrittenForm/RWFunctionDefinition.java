package org.flasck.flas.rewrittenForm;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class RWFunctionDefinition implements Locatable, Comparable<RWFunctionDefinition> {
	public final InputPosition location;
	public final CodeType mytype;
	private final FunctionName fnName;
	public final String name;
	public final int nargs;
	public final String inCard;
	public final List<RWFunctionCaseDefn> cases = new ArrayList<>();
	public final boolean generate;
	private Type type;
	public final Set<ScopedVar> scopedVars = new TreeSet<ScopedVar>();

	public RWFunctionDefinition(FunctionName name, int nargs, boolean generate) {
		this.inCard = name.inCard == null ? null : name.inCard.jsName();
		if (name.location == null)
			throw new UtilException("Null location");
		this.location = name.location;
		this.mytype = name.codeType;
		this.fnName = name;
		this.name = name.jsName();
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
	
	public String name() {
		return name;
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
