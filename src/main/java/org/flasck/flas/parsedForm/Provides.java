package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;

public class Provides extends Implements implements RepositoryEntry {
	public final String referAsVar;
	private InputPosition varLocation;

	public Provides(InputPosition kw, InputPosition location, NamedType consumer, TypeReference type, CSName csn, InputPosition varloc, String varname) {
		super(kw, location, consumer, type, csn);
		this.varLocation = varloc;
		if (varname != null)
			this.referAsVar = varname;
		else
			this.referAsVar = "_" + type.name();
	}

	public NameOfThing varName() {
		return new VarName(varLocation, this.name().container(), referAsVar);
	}
	
	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print("Provides[" + this.referAsVar + ":" + super.implementsType().name() + "]");
	}
}
