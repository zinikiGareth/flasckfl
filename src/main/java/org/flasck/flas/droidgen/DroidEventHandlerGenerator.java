package org.flasck.flas.droidgen;

import org.flasck.flas.template.EventHandlerGenerator;
import org.zinutils.bytecode.MethodDefiner;

public class DroidEventHandlerGenerator implements EventHandlerGenerator {

	public DroidEventHandlerGenerator(MethodDefiner meth) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handle(boolean giveDistinguishedName, String action, String callFn) {
		// TODO Auto-generated method stub

		// needs to be something like
		// super.addEventHandler(action, classConst(callFn));
		// and "Area" needs to have that method and delegate the work to (JDK/Droid)DisplayEngine
	}

}
