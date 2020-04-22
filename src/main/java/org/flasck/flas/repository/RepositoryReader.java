package org.flasck.flas.repository;

import java.util.Set;

import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Type;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.SplitMetaData;

public interface RepositoryReader {
	<T extends RepositoryEntry> T get(String string);
	void traverse(Visitor visitor);
	void traverseWithHSI(HSIVisitor visitor);
	void traverseInGroups(Visitor visitor, FunctionGroups groups);
	void traverseWithMemberFields(Visitor visitor);
	void dump();
	Type findUnionWith(Set<Type> tys);
	Iterable<SplitMetaData> allWebs();
	CardData findWeb(String baseName);
}
