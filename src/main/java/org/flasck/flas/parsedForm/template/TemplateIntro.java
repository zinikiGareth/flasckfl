package org.flasck.flas.parsedForm.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;

public class TemplateIntro {
	public final String name;
	public final InputPosition location;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();

	public TemplateIntro(InputPosition location, String name) {
		this.location = location;
		this.name = name;
	}
}
