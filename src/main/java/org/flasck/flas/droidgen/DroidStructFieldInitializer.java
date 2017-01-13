package org.flasck.flas.droidgen;

import java.util.Map;

import org.flasck.flas.rewrittenForm.FieldVisitor;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.NewMethodDefiner;

public class DroidStructFieldInitializer implements FieldVisitor {
	private final NewMethodDefiner dfe;
	final Map<String, IFieldInfo> fields;

	public DroidStructFieldInitializer(NewMethodDefiner dfe, Map<String, IFieldInfo> fields) {
		this.dfe = dfe;
		this.fields = fields;
	}

	@Override
	public void visit(RWStructField sf) {
		IExpr fe = fields.get(sf.name).asExpr(dfe);
		dfe.assign(fe, dfe.callVirtual(J.OBJECT, dfe.myThis(), "_fullOf", dfe.as(fe, J.OBJECT))).flush();
	}
}
