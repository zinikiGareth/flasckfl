package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;

public abstract class FieldsDefn implements AsString, Locatable, PolyHolder {
	public enum FieldsType { 
		STRUCT(0),
		ENTITY(1),
		DEAL(420), // 420 signifies I haven't thought this through or tested it yet
		OFFER(420),
		STATE(420),
		ENVELOPE(420),
		WRAPS(420);
		
		private final int ignore;

		FieldsType(int ignore) {
			this.ignore = ignore;
		}
		
		public int ignoreCount() {
			return ignore;
		}
	}

	public final boolean generate;
	public final InputPosition kw;
	protected final InputPosition location;
	public final SolidName name;
	public final FieldsType type;
	protected final List<PolyType> polys;
	public final List<StructField> fields = new ArrayList<StructField>();

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

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<PolyType> polys() {
		return polys;
	}

	public void addField(StructField sf) {
		// TODO: validate that any poly fields here are defined in the provided list of polys
		fields.add(sf);
	}

	public StructField findField(String var) {
		for (StructField sf : fields)
			if (sf.name.equals(var))
				return sf;
		return null;
	}

	@Override
	public String toString() {
		return asString();
	}
}
