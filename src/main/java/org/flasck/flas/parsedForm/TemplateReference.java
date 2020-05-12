package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.TemplateName;
import org.ziniki.splitter.CardData;

public class TemplateReference implements Locatable {
	public final TemplateName name;
	private InputPosition location;
	private CardData webInfo;
	private Template template;

	public TemplateReference(InputPosition location, TemplateName name) {
		this.location = location;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void bindTo(Template template, CardData webInfo) {
		this.template = template;
		this.webInfo = webInfo;
	}
	
	public Template template() {
		return template;
	}
	
	public CardData defn() {
		return webInfo;
	}
}
