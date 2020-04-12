package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class HandlerImplements extends Implements implements RepositoryEntry, NamedType, WithTypeSignature {
	public final String baseName;
	public final List<HandlerLambda> boundVars;
	public final boolean inCard;
	public final InputPosition typeLocation;
	public final HandlerName handlerName;

	public HandlerImplements(InputPosition kw, InputPosition location, InputPosition typeLocation, NamedType parent, HandlerName handlerName, TypeReference implementing, boolean inCard, List<HandlerLambda> lambdas) {
		super(kw, location, parent, implementing, handlerName);
		this.typeLocation = typeLocation;
		this.handlerName = handlerName;
		this.baseName = handlerName.baseName;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}

	@Override
	public String signature() {
		return handlerName.uniqueName();
	}

	@Override
	public int argCount() {
		return boundVars.size();
	}

	@Override
	public Type get(int pos) {
		if (pos == boundVars.size())
			return this;
		HandlerLambda p = boundVars.get(pos);
		if (p.patt instanceof TypedPattern)
			return ((TypedPattern)p.patt).type();
		else
			throw new NotImplementedException("Not handled: " + p.getClass());
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}
	
	@Override
	public Type type() {
		return this;
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
