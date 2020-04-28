package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateBinding extends TemplateCustomization {
	public final InputPosition slotLoc;
	public final String slot;
	public TemplateBindingOption defaultBinding;
	public final List<TemplateBindingOption> conditionalBindings = new ArrayList<>();

	public TemplateBinding(InputPosition slotLoc, String slot, TemplateBindingOption simple) {
		this.slotLoc = slotLoc;
		this.slot = slot;
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
		return "Binding[" + slot + "<-"+ defaultBinding.expr + "=>" + defaultBinding.sendsTo+"]";
	}
}
