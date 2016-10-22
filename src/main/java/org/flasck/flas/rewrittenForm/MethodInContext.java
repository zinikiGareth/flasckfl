package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewriter.Rewriter.NamingContext;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class MethodInContext implements Locatable {
	public static final int DOWN = 1;
	public static final int UP = 2;
	public static final int EVENT = 3;
	public static final int OBJECT = 4;
	public static final int STANDALONE = 5;
	public final String fromContract;
	public final InputPosition contractLocation;
	public final int direction;
	public final String name;
	public final CodeType type;
	public final RWMethodDefinition method;
	public final List<Object> enclosingPatterns = new ArrayList<Object>();

	public MethodInContext(Rewriter rw, NamingContext cx, int dir, InputPosition cloc, String fromContract, String name, CodeType type, RWMethodDefinition method, List<Object> enclosing) {
		this.direction = dir;
		this.contractLocation = cloc;
		this.fromContract = fromContract;
		this.name = name;
		this.type = type;
		this.method = method;
		this.enclosingPatterns.addAll(enclosing);
	}

	@Override
	public InputPosition location() {
		return method.location();
	}
}
