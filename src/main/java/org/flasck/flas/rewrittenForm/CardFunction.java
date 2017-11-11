package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class CardFunction implements ExternalRef {
	public final InputPosition location;
	public final CardName clzName;
	public final String function;
	public final boolean fromHandler;
	private final FunctionName fnName;

	public CardFunction(InputPosition location, CardName clzName, String function) {
		this.location = location;
		this.clzName = clzName;
		this.function = function;
		this.fromHandler = false;
		this.fnName = FunctionName.functionInCardContext(location, clzName, function);
	}
	
	public CardFunction(InputPosition location, CardFunction inner, boolean fromHandler) {
		this.location = location;
		this.clzName = inner.clzName;
		this.function = inner.function;
		this.fromHandler = fromHandler;
		this.fnName = FunctionName.functionInCardContext(location, clzName, function);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return this.clzName.uniqueName() + "." + this.function;
	}

	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		// TODO: replace this with something more specific
		return new PushExternal(location, this);
	}

	@Override
	public NameOfThing myName() {
		return fnName;
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.clzName.uniqueName() + "." + this.function;
	}

	@Override
	public int compareTo(Object o) {
		return this.uniqueName().compareTo(((ExternalRef)o).uniqueName());
	}
}
