package org.flasck.flas.compiler.jsgen;

import java.util.List;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class DontGenerateJSServices extends LeafAdapter implements HSIVisitor{
	private NestedVisitor sv;

	public DontGenerateJSServices(NestedVisitor sv) {
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void leaveServiceDefn(ServiceDefinition s) {
		sv.result(null);
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		sv.result(null);
	}
	
	@Override
	public void leaveUnitTest(UnitTestCase e) {
		sv.result(null);
	}

	@Override
	public void hsiArgs(List<Slot> slots) {
	}

	@Override
	public void switchOn(Slot slot) {
	}

	@Override
	public void withConstructor(String string) {
	}

	@Override
	public void constructorField(Slot parent, String field, Slot slot) {
	}

	@Override
	public void matchNumber(int i) {
	}

	@Override
	public void matchString(String s) {
	}

	@Override
	public void matchDefault() {
	}

	@Override
	public void defaultCase() {
	}

	@Override
	public void errorNoCase() {
	}

	@Override
	public void bind(Slot slot, String var) {
	}

	@Override
	public void endSwitch() {
	}
}
