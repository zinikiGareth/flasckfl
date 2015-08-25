package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.CollectionUtils;

@SuppressWarnings("serial")
public class StructDefn extends Type implements AsString, Serializable {
	public final List<StructField> fields = new ArrayList<StructField>();
	public final transient boolean generate;

	public StructDefn(InputPosition location, String tn, boolean generate, Type... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	public StructDefn(InputPosition location, String tn, boolean generate, List<Type> polys) {
		super(location, WhatAmI.STRUCT, tn, polys);
		this.generate = generate;
	}

	public StructDefn addField(StructField sf) {
		// TODO: validate that any poly fields here are defined in the provided list of polys
		fields.add(sf);
		return this;
	}

	public String toString() {
		return asString();
	}
	
	public String dump() {
		StringBuilder sb = new StringBuilder(asString());
		if (!fields.isEmpty()) {
			sb.append("{");
			String sep = "";
			for (StructField f : fields) {
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
}
