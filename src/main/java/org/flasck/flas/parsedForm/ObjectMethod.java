package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectMethod extends ObjectActionHandler implements HandlerHolder {
	private ObjectDefn od;
	private Implements impl;
	private ContractMethodDecl contractMethod;
	public final VarPattern handler;
	private CardDefinition eventCard;
	private List<Template> eventSources = new ArrayList<>();
	private List<Type> eventSourceTypes = new ArrayList<>();

	public ObjectMethod(InputPosition location, FunctionName name, List<Pattern> args, VarPattern handler) {
		super(location, name, args);
		this.handler = handler;
	}

	public void eventFor(CardDefinition card) {
		this.eventCard = card;
	}
	
	public void eventSource(Template t) {
		this.eventSources.add(t);
	}
	
	public List<Template> eventSourceExprs() {
		return eventSources;
	}
	
	public void bindEventSource(Type t) {
		this.eventSourceTypes.add(t);
	}
	
	public List<Type> sources() {
		if (eventSources.size() != eventSourceTypes.size())
			throw new NotImplementedException("I don't think the event sources have been typechecked");
		return eventSourceTypes;
	}

	public void bindToObject(ObjectDefn od) {
		this.od = od;
	}

	public void bindToImplements(Implements implements1) {
		this.impl = implements1;
	}

	public void bindFromContract(ContractMethodDecl cm) {
		this.contractMethod = cm;
	}

	public boolean hasObject() {
		return od != null;
	}

	public ObjectDefn getObject() {
		if (od == null)
			throw new NotImplementedException("There is no object definition bound here");
		return od;
	}

	public boolean hasImplements() {
		return impl != null;
	}
	
	public Implements getImplements() {
		if (impl == null)
			throw new NotImplementedException("There is no impl definition bound here");
		return impl;
	}

	@Override
	public boolean isEvent() {
		return eventCard != null;
	}

	@Override
	public CardDefinition getCard() {
		return eventCard;
	}

	@Override
	public VarPattern handler() {
		return handler;
	}

	@Override
	public String toString() {
		return name().uniqueName() + "/" + args().size();
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ObjectMethod[" + toString() + "]");
	}

	public ContractMethodDecl contractMethod() {
		return contractMethod;
	}
}
