package org.flasck.flas.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessRestrictions;

public class UTOnlyRestriction implements AccessRestrictions {

	private final String name;

	public UTOnlyRestriction(String name) {
		this.name = name;
	}
	
	@Override
	public void check(ErrorReporter errors, InputPosition pos, NameOfThing inContext) {
		NameOfThing check = inContext;
		while (check != null) {
			if (check instanceof UnitTestFileName)
				return;
			check = check.container();
		}
		
		errors.message(pos, "cannot use " + name + " in package '" + inContext.uniqueName() + "'");
	}
}
