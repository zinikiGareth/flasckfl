package org.flasck.flas.commonBase.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;

public class TemplateIntro {
	public final InputPosition kw;
	public final InputPosition location;
	public final String name;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();

	public TemplateIntro(InputPosition kw, InputPosition location, String name) {
		this.kw = kw;
		this.location = location;
		this.name = name;
	}
}
