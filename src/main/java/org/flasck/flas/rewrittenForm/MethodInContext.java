package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;

public class MethodInContext implements Locatable {
	public final String fromContract;
	public final InputPosition contractLocation;
	public final int direction;
	public final String inCard;
	public final String name;
	public final CodeType type;
	public final RWMethodDefinition method;
	public final List<Object> enclosingPatterns = new ArrayList<Object>();

	public MethodInContext(String cardNameIfAny, InputPosition cloc, String fromContract, CodeType type, int dir, List<Object> enclosing, String name, RWMethodDefinition method) {
		this.direction = dir;
		this.contractLocation = cloc;
		this.fromContract = fromContract;
		this.inCard = cardNameIfAny;
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
