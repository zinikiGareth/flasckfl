package org.flasck.flas.compiler.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.EventHolder;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.StackVisitor;

public class EventBuilder extends LeafAdapter {
	private final Map<EventHolder, EventTargetZones> eventMap;
	private EventPlacement currentETZ;
	private String templateId;
	private TemplateBinding currentBinding;
	private int option = 0;
	private int cond = 0;
	private List<Integer> conds = new ArrayList<Integer>();

	public EventBuilder(StackVisitor stack, Map<EventHolder, EventTargetZones> etz) {
		eventMap = etz;
		stack.push(this);
	}
	
	@Override
	public void visitCardDefn(CardDefinition cd) {
		currentETZ = new EventPlacement();
		eventMap.put(cd, currentETZ);
	}
	
	@Override
	public void visitObjectDefn(ObjectDefn od) {
		currentETZ = new EventPlacement();
		eventMap.put(od, currentETZ);
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
		templateId = t.webinfo().id();
		this.cond = 0;
	}
	
	@Override
	public void visitTemplateBinding(TemplateBinding b) {
		currentBinding = b;
		option = 0;
		this.cond = 0;
	}
	
	@Override
	public void visitTemplateBindingOption(TemplateBindingOption tbo) {
		this.option++;
	}
	
	@Override
	public void visitTemplateStyling(TemplateStylingOption tso) {
		if (tso.cond != null) {
			this.conds.add(cond);
			cond++;
		}
	}
	
	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
		if (tso.cond != null) {
			this.conds.remove(this.conds.size()-1);
		}
	}
	
	@Override
	public void visitTemplateEvent(TemplateEvent te) {
		Integer cond = null;
		if (!conds.isEmpty())
			cond = conds.get(conds.size()-1);
		currentETZ.binding(templateId, currentBinding, option, te.handler, cond);
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
	
	@Override
	public void leaveObjectDefn(ObjectDefn od) {
		currentETZ = null;
	}
}
