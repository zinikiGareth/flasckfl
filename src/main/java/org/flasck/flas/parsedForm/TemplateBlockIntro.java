package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateBlockIntro extends TemplateDiv implements Locatable {
	public final String forHole;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();


	public TemplateBlockIntro(InputPosition location, String forHole) {
		super(location, null, null, null, null, null, new ArrayList<>(), new ArrayList<>());
		this.forHole = forHole;
	}

}
