package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.HandlerName;
import org.flasck.flas.stories.FLASStory.State;

public class HandlerImplements extends Implements {
	public final String hiName;
	public final String baseName;
	public final List<Object> boundVars;
	public final boolean inCard;
	public final InputPosition typeLocation;

	public HandlerImplements(InputPosition kw, InputPosition location, InputPosition typeLocation, HandlerName handlerName, String type, boolean inCard, List<Object> lambdas) {
		super(kw, location, type);
		this.typeLocation = typeLocation;
		this.hiName = handlerName.jsName();
		this.baseName = State.simpleName(handlerName.jsName());
		this.inCard = inCard;
		this.boundVars = lambdas;
	}
}
