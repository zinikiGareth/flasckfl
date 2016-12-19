package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.HandlerName;

public class RWHandlerImplements extends RWImplements {
	public final String hiName;
	public final List<HandlerLambda> boundVars;
	public final boolean inCard;

	public RWHandlerImplements(InputPosition kw, InputPosition location, HandlerName handlerName, String type, boolean inCard, List<HandlerLambda> lambdas) {
		super(kw, location, WhatAmI.HANDLERIMPLEMENTS, type);
		this.hiName = handlerName.jsName();
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
			else if (ohl.scopedFrom.myId.compareTo(ret.myId) > 0)
				break;
			else
				pos++;
		}
		boundVars.add(pos, hl);
	}
}
