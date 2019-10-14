package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.TypeWithMethods;
import org.flasck.flas.types.TypeWithName;
import org.zinutils.exceptions.UtilException;

public class RWContractDecl extends TypeWithName implements TypeWithMethods {
	public final List<RWContractMethodDecl> methods = new ArrayList<RWContractMethodDecl>();
	public final transient boolean generate;

	public RWContractDecl(InputPosition kw, InputPosition location, SolidName name, boolean g) {
		super(kw, location, name);
		this.generate = g;
	}

	public void addMethod(RWContractMethodDecl md) {
		methods.add(md);
	}
	
	@Override
	public boolean hasMethod(String named) {
		for (RWContractMethodDecl m : methods)
			if (m.name.equals(named))
				return true;
		return false;
	}
	
	public boolean checkMethodDir(String named, ContractMethodDir dir) {
		for (RWContractMethodDecl m : methods)
			if (m.name.equals(named))
				return m.dir.equals(dir);
		return false;
	}
	
	public FunctionType getMethodType(String named) {
		for (RWContractMethodDecl m : methods)
			if (m.name.equals(named)) {
				return m.getType();
			}
		throw new UtilException("There is no method " + named);
	}
	
	@Override
	public String toString() {
		return "contract " + nameAsString();
	}
}
