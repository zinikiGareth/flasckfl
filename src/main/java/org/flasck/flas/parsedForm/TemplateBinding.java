package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.ziniki.splitter.FieldType;

public class TemplateBinding extends TemplateCustomization {
	public final InputPosition slotLoc;
	public final String slot;
	public TemplateBindingOption defaultBinding;
	public final List<TemplateBindingOption> conditionalBindings = new ArrayList<>();
	private FieldType fieldType;

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
	
	public void fieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}
	
	public FieldType fieldType() {
		return this.fieldType;
	}
	
	@Override
	public String toString() {
		return "Binding[" + slot + "<-"+ defaultBinding.expr + "=>" + defaultBinding.sendsTo+"]";
	}
}
