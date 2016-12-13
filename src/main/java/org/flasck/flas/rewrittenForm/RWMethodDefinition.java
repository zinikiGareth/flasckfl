package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class RWMethodDefinition implements Locatable {
	public static final int STANDALONE = 5;
	public static final int OBJECT = 4;
	public static final int EVENT = 3;
	public static final int UP = 2;
	public static final int DOWN = 1;
	public final InputPosition contractLocation;
	public final String fromContract;
	public final CodeType type;
	public final int dir;
	private final InputPosition location;
	private final int nargs;
	public final List<RWMethodCaseDefn> cases = new ArrayList<>();
	public final Set<ScopedVar> scopedVars = new TreeSet<ScopedVar>();
	private FunctionName fnName;
	
	public RWMethodDefinition(InputPosition cloc, String contractName, CodeType type, int dir, InputPosition location, FunctionName name, int nargs) {
		this.fnName = name;
		this.contractLocation = cloc;
		this.fromContract = contractName;
		this.type = type;
		this.dir = dir;
		this.location = location;
		this.nargs = nargs;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public FunctionName name() {
		return fnName;
	}
	
	public int nargs() {
		return nargs;
	}

	public void gatherScopedVars() {
		for (RWMethodCaseDefn c : cases) {
			c.gatherScopedVars(scopedVars);
		}
	}
	
	@Override
	public String toString() {
		return fnName.jsName() + "/" + nargs;
	}
}
