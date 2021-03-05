package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.Comparator;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class StructField implements Locatable, RepositoryEntry, FieldAccessor {
	public static Comparator<StructField> nameComparator = new Comparator<StructField>() {
		@Override
		public int compare(StructField o1, StructField o2) {
			return o1.name.compareTo(o2.name);
		}
	};
	public final InputPosition loc;
	public final InputPosition assOp;
	public final FieldsHolder container;
	public final boolean accessor;
	public final boolean generate;
	public final TypeReference type;
	public final String name;
	public final Expr init;
	private VarName myName;

	public StructField(InputPosition loc, FieldsHolder container, boolean accessor, boolean generate, TypeReference type, String name) {
		this(loc, null, container, accessor, generate, type, name, null);
	}

	public StructField(InputPosition loc, InputPosition assOp, FieldsHolder container, boolean accessor, boolean generate, TypeReference type, String name, Expr init) {
		this.loc = loc;
		this.assOp = assOp;
		this.container = container;
		this.accessor = accessor;
		this.generate = generate;
		this.type = type;
		this.name = name;
		this.init = init;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public Type type() {
		return type.defn();
	}

	@Override
	public Expr acor(Expr from) {
		return new MakeAcor(from.location(), FunctionName.function(loc, myName.scope, "_field_" + name), from, 0);
	}

	@Override
	public int acorArgCount() {
		return 0;
	}

	@Override
	public String toString() {
		if (type == null)
			return name + " (/" + loc.off + ")";
		else
			return type + " " + name + " (" + type.location().off + "/" + loc.off + ")";
	}

	public void fullName(VarName nameVar) {
		this.myName = nameVar;
	}
	
	public VarName name() {
		return myName;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("StructField[" + myName.uniqueName() + "]");
	}

	public FieldsHolder container() {
		return container;
	}
}
