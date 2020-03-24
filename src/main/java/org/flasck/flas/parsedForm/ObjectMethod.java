package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectMethod extends ObjectActionHandler implements HandlerHolder {
	private ObjectDefn od;
	private Implements impl;
	private ContractMethodDecl contractMethod;
	public final VarPattern handler;

	public ObjectMethod(InputPosition location, FunctionName name, List<Pattern> args, VarPattern handler) {
		super(location, name, args);
		this.handler = handler;
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
