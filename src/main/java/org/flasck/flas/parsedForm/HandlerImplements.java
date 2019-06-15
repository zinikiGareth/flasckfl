package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.repository.RepositoryEntry;

public class HandlerImplements extends Implements implements RepositoryEntry {
	public final String baseName;
	public final List<Object> boundVars;
	public final boolean inCard;
	public final InputPosition typeLocation;
	public final HandlerName handlerName;

	public HandlerImplements(InputPosition kw, InputPosition location, InputPosition typeLocation, HandlerName handlerName, String type, boolean inCard, List<Object> lambdas) {
		super(kw, location, type);
		this.typeLocation = typeLocation;
		this.handlerName = handlerName;
		this.baseName = handlerName.baseName;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}
	
	public HandlerName getRealName() {
		return (HandlerName) realName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	@Override
	public String toString() {
		return "HandlerImplements[" + handlerName.uniqueName() + "]";
	}
}
