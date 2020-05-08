package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TemplateBinding extends TemplateCustomization {
	public final TemplateField assignsTo;
	public final List<TemplateBindingOption> conditionalBindings = new ArrayList<>();
	public TemplateBindingOption defaultBinding;

	public TemplateBinding(TemplateField assignsTo, TemplateBindingOption simple) {
		this.assignsTo = assignsTo;
		this.defaultBinding = simple;
	}
	
	public Iterable<TemplateBindingOption> conditional() {
		return conditionalBindings;
	}
	
	public TemplateBindingOption defaultBinding() {
		return defaultBinding;
	}
	
	@Override
	public String toString() {
		return "Binding[" + assignsTo.text + "<-"+ defaultBinding.expr + "=>" + defaultBinding.sendsTo+"]";
	}
}
