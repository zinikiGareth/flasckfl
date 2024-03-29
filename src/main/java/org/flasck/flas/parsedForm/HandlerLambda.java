package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;

public class HandlerLambda implements RepositoryEntry, Locatable {
	public final static Logger unilogger = LoggerFactory.getLogger("TCUnification");
	public final Pattern patt;
	public final boolean isNested;
	public final List<FunctionName> usedBy = new ArrayList<>();
	private UnifiableType ut;

	public HandlerLambda(Pattern patt, boolean nested) {
		this.patt = patt;
		this.isNested = nested;
	}

	@Override
	public InputPosition location() {
		return patt.location();
	}
	
	@Override
	public NameOfThing name() {
		return ((RepositoryEntry)patt).name();
	}
	
	public void usedBy(FunctionName n) {
		this.usedBy.add(n);
	}
	
	public void unifiableType(UnifiableType lt) {
		unilogger.info("    binding HL " + patt + " to " + lt);
		ut = lt;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("HandlerLambda[" + patt + "]");
	}

	public Type unifiableType() {
		if (ut == null)
			throw new CantHappenException("no unifiable type bound");
		return ut;
	}

	public Type type() {
		if (patt instanceof TypedPattern)
			return ((TypedPattern)patt).type();
		else if (isNested)
			return unifiableType();
		else
			throw new CantHappenException("there is no type defined for " + this);
	}

}
