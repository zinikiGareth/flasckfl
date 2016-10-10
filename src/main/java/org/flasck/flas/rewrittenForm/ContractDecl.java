package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class ContractDecl extends TypeWithMethods implements Serializable {
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();
	public final transient boolean generate;

	public ContractDecl(InputPosition kw, InputPosition location, String contractName) {
		super(kw, location, WhatAmI.CONTRACT, contractName, null);
		this.generate = true;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}
	
	@Override
	public boolean hasMethod(String named) {
		for (ContractMethodDecl m : methods)
			if (m.name.equals(named))
				return true;
		return false;
	}
	
	public boolean checkMethodDir(String named, String dir) {
		for (ContractMethodDecl m : methods)
			if (m.name.equals(named))
				return m.dir.equals(dir);
		return false;
	}
	
	public Type getMethodType(String named) {
		for (ContractMethodDecl m : methods)
			if (m.name.equals(named)) {
				return m.getType();
			}
		throw new UtilException("There is no method " + named);
	}
	
	@Override
	public String toString() {
		return "contract " + name();
	}
}
