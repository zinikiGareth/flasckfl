package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.NotImplementedException;

public class RequiresContract extends Implements implements RepositoryEntry {
	public final String referAsVar;
	public final InputPosition varLocation;

	public RequiresContract(InputPosition kw, InputPosition location, NamedType parent, TypeReference type, CSName csn, InputPosition vlocation, String referAsVar) {
		super(kw, location, parent, type, csn);
		this.varLocation = vlocation;
		this.referAsVar = referAsVar;
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return super.toString() + (referAsVar != null?(" " + referAsVar):"");
	}
}
