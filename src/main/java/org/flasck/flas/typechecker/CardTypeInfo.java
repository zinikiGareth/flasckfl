package org.flasck.flas.typechecker;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.StructDefn;

public class CardTypeInfo extends TypeHolder {
	public final StructDefn struct;
	public final Set<TypeHolder> contracts = new TreeSet<TypeHolder>();
	public final Set<TypeHolder> handlers = new TreeSet<TypeHolder>();

	public CardTypeInfo(CardGrouping cg) {
		super(cg.struct.typename);
		this.struct = cg.struct;
	}
}
