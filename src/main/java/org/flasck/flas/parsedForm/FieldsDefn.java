package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;

public abstract class FieldsDefn implements AsString, Locatable {
	public enum FieldsType { STRUCT, ENTITY, DEAL, OFFER, OBJECT }

	public final boolean generate;
	public final InputPosition kw;
	protected final InputPosition location;
	public final SolidName name;
	public final FieldsType type;
	protected final List<PolyType> polys;

	public FieldsDefn(InputPosition kw, InputPosition location, FieldsType structType, SolidName tn, boolean generate,	List<PolyType> polys) {
		this.kw = kw;
		this.location = location;
		this.type = structType;
		this.name = tn;
		this.generate = generate;
		this.polys = polys;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public SolidName name() {
		return name;
	}

	public List<PolyType> polys() {
		return polys;
	}

	@Override
	public String toString() {
		return asString();
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
}
