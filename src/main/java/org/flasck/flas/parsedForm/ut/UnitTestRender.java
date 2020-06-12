package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestRender implements UnitTestStep {
	public final UnresolvedVar card;
	public final TemplateReference template;
	
	public UnitTestRender(UnresolvedVar card, TemplateReference template) {
		this.card = card;
		this.template = template;
	}
}
