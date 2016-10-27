package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class HandlerImplements extends Implements {
	public final String hiName;
	public final List<Object> boundVars;
	public final boolean inCard;
	public final InputPosition typeLocation;

	public HandlerImplements(InputPosition kw, InputPosition location, InputPosition typeLocation, String named, String type, boolean inCard, List<Object> lambdas) {
		super(kw, location, type);
		this.typeLocation = typeLocation;
		this.hiName = named;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}
}
