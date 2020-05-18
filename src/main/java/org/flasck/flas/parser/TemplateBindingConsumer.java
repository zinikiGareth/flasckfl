package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateStylingOption;

public interface TemplateBindingConsumer {
	void addBinding(TemplateBinding binding);
	void addStyling(TemplateStylingOption x);
}
