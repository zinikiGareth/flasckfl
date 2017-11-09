package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.template.EventHandlerGenerator;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;

public class DroidEventHandlerGenerator implements EventHandlerGenerator {
	private final MethodDefiner ahMeth;

	public DroidEventHandlerGenerator(MethodDefiner ahMeth) {
		this.ahMeth = ahMeth;
	}

	@Override
	public void handle(boolean giveDistinguishedName, String action, SolidName clz) {
		ahMeth.callSuper(JavaType.void_.getActual(), J.AREA, "addEventHandler", ahMeth.boolConst(giveDistinguishedName), ahMeth.stringConst(action), ahMeth.classConst(clz.javaClassName())).flush();
	}

	@Override
	public void done() {
		ahMeth.returnObject(ahMeth.aNull()).flush();
	}
}
