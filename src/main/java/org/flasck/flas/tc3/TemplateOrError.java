package org.flasck.flas.tc3;

import org.flasck.flas.errors.FLASError;
import org.flasck.flas.parsedForm.Template;
import org.ziniki.splitter.CardData;
import org.zinutils.exceptions.CantHappenException;

public class TemplateOrError {
	private final Template template;
	private final CardData webInfo;
	private final FLASError err;
	private final Type type;

	public TemplateOrError(Template template, CardData webInfo) {
		this.template = template;
		this.webInfo = webInfo;
		this.err = null;
		this.type = null;
	}

	public TemplateOrError(FLASError flasError, Type type) {
		this.template = null;
		this.webInfo = null;
		this.err = flasError;
		this.type = type;
	}

	public boolean hasError() {
		return err != null;
	}
	
	public FLASError error() {
		return err;
	}
	
	public Type forType() {
		if (type == null)
			throw new CantHappenException("you should have checked for the error case first");
		return type;
	}
	
	public Template template() {
		if (template == null)
			throw new CantHappenException("you should have checked if this was a valid template first");
		return template;
	}

	public CardData webInfo() {
		if (webInfo == null)
			throw new CantHappenException("you should have checked if this was a valid template first");
		return webInfo;
	}

}
