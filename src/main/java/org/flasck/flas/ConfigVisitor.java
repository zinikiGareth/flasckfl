package org.flasck.flas;

import java.io.File;

import org.flasck.flas.compiler.PhaseTo;

public interface ConfigVisitor {

	void dumpTypes(boolean d);

	void searchIn(File file);

	void useWebZip(String called);

	void webZipDir(File file);

	void unitjvm(boolean b);

	void unitjs(boolean b);

	void writeTestReportsTo(File file);

	void writeJSTo(File file);

	void trackTC(File file);

	void writeHSIETo(File file);

	void writeDependsTo(File file);

	void writeFlimTo(File file);

	void writeRWTo(File file);

	void writeJVMTo(File file);

	void writeDroidTo(File file, boolean andBuild);

	void phaseTo(PhaseTo upto);

	void dumpRepoTo(File dumprepo);

}
