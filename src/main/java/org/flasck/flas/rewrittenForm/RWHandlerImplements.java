package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class RWHandlerImplements extends RWImplements {
	public final String hiName;
	public final List<RWHandlerLambda> boundVars;
	public final boolean inCard;

	public RWHandlerImplements(InputPosition kw, InputPosition location, String named, String type, boolean inCard, List<RWHandlerLambda> lambdas) {
		super(kw, location, WhatAmI.HANDLERIMPLEMENTS, type);
		this.hiName = named;
		this.inCard = inCard;
		this.boundVars = lambdas;
	}

	public void addScoped(RWHandlerLambda hl, ScopedVar ret) {
		hl.scopedFrom = ret;
		int pos=0;
		for (Object o : boundVars) {
			RWHandlerLambda ohl = (RWHandlerLambda)o;
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
