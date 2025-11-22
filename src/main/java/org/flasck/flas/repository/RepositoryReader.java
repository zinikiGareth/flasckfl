package org.flasck.flas.repository;

import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.tc3.Type;
import org.ziniki.splitter.CardData;
import org.ziniki.splitter.SplitMetaData;

public interface RepositoryReader extends UnionFinder {
	<T extends RepositoryEntry> T get(String string);
	void traverse(RepositoryVisitor visitor);
	void traverseWithHSI(HSIVisitor visitor);
	void traverseInGroups(RepositoryVisitor visitor, FunctionGroups groups);
	void traverseWithMemberFields(RepositoryVisitor visitor);
	void dump();
	List<UnionTypeDefn> unionsContaining(StructDefn obj);
	Type findUnionWith(ErrorReporter errors, InputPosition pos, Set<Type> ms, boolean needAll);
	SplitMetaData allWebs();
	CardData findWeb(String baseName);
	RepositoryEntry findNested(ErrorReporter errors, InputPosition loc, String scope, String tn);
}
