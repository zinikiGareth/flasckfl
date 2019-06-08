package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TemplateBinding {
	public final String slot;
	public TemplateBindingOption defaultBinding;
	public final List<TemplateBindingOption> conditionalBindings = new ArrayList<>();

	public TemplateBinding(String slot, TemplateBindingOption simple) {
		this.slot = slot;
		this.defaultBinding = simple;
	}
	
	@Override
	public String toString() {
		return "Binding[" + slot + "<-"+ defaultBinding.expr + "=>" + defaultBinding.sendsTo+"]";
	}
}
