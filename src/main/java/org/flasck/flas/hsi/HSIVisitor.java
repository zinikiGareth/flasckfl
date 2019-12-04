package org.flasck.flas.hsi;

import java.util.List;

import org.flasck.flas.repository.Repository.Visitor;

public interface HSIVisitor extends Visitor {
	void hsiArgs(List<Slot> slots);
	void switchOn(Slot slot);
	void withConstructor(String string);
	void constructorField(Slot parent, String field, Slot slot);
	void matchNumber(int i);
	void matchString(String s);
	void matchDefault();
	void defaultCase();
	void errorNoCase();
	void bind(Slot slot, String var);
	void endSwitch();
}
