package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

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
	
	public boolean doesAssignment() {
		if (!conditionalBindings.isEmpty())
			return true;
		return defaultBinding != null && defaultBinding.expr != null;
	}
	
	public InputPosition location() {
		return assignsTo.location();
	}
	
	@Override
	public String toString() {
		return "Binding[" + assignsTo.text + "<-"+ defaultBinding.expr + "=>" + defaultBinding.sendsTo+"]";
	}
}
