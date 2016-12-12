package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class RWMethodDefinition implements Locatable {
	public static final int STANDALONE = 5;
	public static final int OBJECT = 4;
	public static final int EVENT = 3;
	public static final int UP = 2;
	public static final int DOWN = 1;
	public final String inCard;
	public final InputPosition contractLocation;
	public final String fromContract;
	public final CodeType type;
	public final int dir;
	private final InputPosition location;
	private final String name;
	private final int nargs;
	public final List<RWMethodCaseDefn> cases = new ArrayList<>();
	public final Set<ScopedVar> scopedVars = new TreeSet<ScopedVar>();
	
	public RWMethodDefinition(CardName cardNameIfAny, InputPosition cloc, String contractName, CodeType type, int dir, InputPosition location, String name, int nargs) {
		this.inCard = cardNameIfAny.jsName();
		this.contractLocation = cloc;
		this.fromContract = contractName;
		this.type = type;
		this.dir = dir;
		this.location = location;
		this.name = name;
		this.nargs = nargs;
	}
	
	public RWMethodDefinition(CardName cardNameIfAny, InputPosition cloc, String contractName, CodeType type, int dir, InputPosition location, FunctionName name, int nargs) {
		this.inCard = cardNameIfAny.jsName();
		this.contractLocation = cloc;
		this.fromContract = contractName;
		this.type = type;
		this.dir = dir;
		this.location = location;
		this.name = name.jsName();
		this.nargs = nargs;
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

	public void gatherScopedVars() {
		for (RWMethodCaseDefn c : cases) {
			c.gatherScopedVars(scopedVars);
		}
	}
	
	@Override
	public String toString() {
		return name + "/" + nargs;
	}
}
