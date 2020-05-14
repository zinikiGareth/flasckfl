package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.TemplateName;
import org.ziniki.splitter.CardData;
import org.zinutils.exceptions.NotImplementedException;

public class TemplateReference implements Locatable {
	public final TemplateName name;
	private InputPosition location;
	private Template template;
	private List<Integer> bindPosns;

	public TemplateReference(InputPosition location, TemplateName name) {
		this.location = location;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void bindTo(Template template) {
		this.template = template;
	}
	
	public Template template() {
		return template;
	}
	
	public CardData defn() {
		if (template.webinfo() == null)
			throw new NotImplementedException("The card data has not been bound");
		return template.webinfo();
	}

	public void bindPosns(List<Integer> posns) {
		this.bindPosns = posns;
	}
	
	public List<Integer> contextPosns() {
		return bindPosns;
	}
}
