package org.flasck.flas.hsi;

import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryVisitor;

public interface HSIVisitor extends RepositoryVisitor {
	void hsiArgs(List<Slot> slots);
	void switchOn(Slot slot);
	void withConstructor(NameOfThing ctor);
	void constructorField(Slot parent, String field, Slot slot);
	void matchNumber(int i);
	void matchString(String s);
	void matchDefault();
	void defaultCase();
	void errorNoCase();
	void bind(Slot slot, String var);
	void endSwitch();
}
