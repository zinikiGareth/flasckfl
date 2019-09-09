package org.flasck.flas.repository;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.tc3.Type;

public class BuiltinRepositoryEntry implements RepositoryEntry {
	public static class Op extends BuiltinRepositoryEntry implements WithTypeSignature {
		public Op(String name, int argCount) {
			super(name, argCount);
		}

		@Override
		public org.flasck.flas.tc3.Type type() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String signature() {
			return "Number";
		}
	}

	public static class Type extends BuiltinRepositoryEntry implements WithTypeSignature {
		public Type(String name, int argCount) {
			super(name, argCount);
		}

		@Override
		public org.flasck.flas.tc3.Type type() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String signature() {
			return name().uniqueName();
		}
	}

	private final SolidName name;
	private final int argCount;

	public BuiltinRepositoryEntry(String name) {
		this.name = new SolidName(null, name);
		this.argCount = 0;
	}

	public BuiltinRepositoryEntry(String string, int argCount) {
		this.argCount = argCount;
		this.name = new SolidName(null, string);
	}

	public SolidName name() {
		return name;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("Builtin[" + name + "]");
	}

	public void loadInto(Repository repository) {
		repository.addEntry(name, this);
	}

	public int argCount() {
		return argCount;
	}

}
