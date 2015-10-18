package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class HandlerImplements extends Implements {
	public final String hiName;
	public final List<Object> boundVars;

	public HandlerImplements(InputPosition location, String named, String type, List<Object> lambdas) {
		super(location, WhatAmI.HANDLERIMPLEMENTS, type);
		this.hiName = named;
		this.boundVars = lambdas;
	}

}
