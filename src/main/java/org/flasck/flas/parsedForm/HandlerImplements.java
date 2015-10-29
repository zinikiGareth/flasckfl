package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class HandlerImplements extends Implements {
	public final String hiName;
	public final List<Object> boundVars;
	public final boolean inCard;

	public HandlerImplements(InputPosition location, String named, String type, boolean inCard, List<Object> lambdas) {
		super(location, WhatAmI.HANDLERIMPLEMENTS, type);
		this.hiName = named;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}

	public void addScoped(HandlerLambda hl, ScopedVar ret) {
		hl.scopedFrom = ret;
		int pos=0;
		for (Object o : boundVars) {
			HandlerLambda ohl = (HandlerLambda)o;
			if (ohl.scopedFrom == null)
				break;
			else if (ohl.scopedFrom.id.compareTo(ret.id) > 0)
				break;
			else
				pos++;
		}
		boundVars.add(pos, hl);
	}
}
