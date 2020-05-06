package org.flasck.flas.compiler.templates;

import java.util.Map;

import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;

public class EventBuilder extends LeafAdapter {
	private final Map<CardDefinition, EventTargetZones> eventMap;
	private EventPlacement currentETZ;
	private String templateId;
	private TemplateBinding currentBinding;

	public EventBuilder(StackVisitor stack, Map<CardDefinition, EventTargetZones> etz) {
		eventMap = etz;
		stack.push(this);
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		currentETZ = new EventPlacement();
		eventMap.put(cd, currentETZ);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		if (meth.isEvent()) {
			TypedPattern tp = (TypedPattern) meth.args().get(0);
			// TODO: I think we should traverse the event type hierarchy & add for all subclasses which are not already defined
			// Or have been defined by one of our base classes
			// That is, eventMap should be a complete map for all classes which have been defined and we should use the closest one
			this.eventMap.get(meth.getCard()).handler(tp.type.name(), meth.name());
		}
	}
	
	@Override
	public void visitTemplate(Template t, boolean isFirst) {
		templateId = t.defines.defn().id();
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		currentBinding = b;
	}
	
	@Override
	public void visitTemplateEvent(TemplateEvent te) {
		currentETZ.binding(templateId, currentBinding, te.handler);
	}
	
	@Override
	public void leaveTemplateBinding(TemplateBinding b) {
		currentBinding = null;
	}
	
	@Override
	public void leaveTemplate(Template t) {
		templateId = null;
	}
	
	@Override
	public void leaveCardDefn(CardDefinition s) {
		currentETZ = null;
	}
}
