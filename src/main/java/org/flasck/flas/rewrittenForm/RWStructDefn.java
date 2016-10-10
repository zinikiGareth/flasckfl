package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class RWStructDefn extends Type implements AsString, Serializable, ExternalRef {
	public final List<RWStructField> fields = new ArrayList<RWStructField>();
	public final transient boolean generate;

	public RWStructDefn(InputPosition location, String tn, boolean generate, Type... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	public RWStructDefn(InputPosition location, String tn, boolean generate, List<Type> polys) {
		super(null, location, WhatAmI.STRUCT, tn, polys);
		this.generate = generate;
	}

	public RWStructDefn addField(RWStructField sf) {
		// TODO: validate that any poly fields here are defined in the provided list of polys
		fields.add(sf);
		return this;
	}

	public RWStructField findField(String var) {
		for (RWStructField sf : fields)
			if (sf.name.equals(var))
				return sf;
		return null;
	}

	public String toString() {
		return asString();
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder(asString());
		if (!fields.isEmpty()) {
			sb.append("{");
			String sep = "";
			for (RWStructField f : fields) {
				sb.append(sep);
				sep = ",";
				sb.append(f.type.name());
				sb.append(" ");
				sb.append(f.name);
			}
			sb.append("}");
		}
		return sb.toString();
	}

	public String asString() {
		StringBuilder sb = new StringBuilder(name());
		if (!polys().isEmpty()) {
			sb.append(polys());
		}
		return sb.toString();
	}

	@Override
	public int compareTo(Object o) {
		return this.name().compareTo(((ExternalRef)o).uniqueName());
	}

	@Override
	public String uniqueName() {
		return this.name();
	}

	@Override
	public boolean fromHandler() {
		throw new UtilException("How would I know?");
	}
}
