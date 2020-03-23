package org.flasck.flas.parsedForm;

@Deprecated
public enum ContractMethodDir {
	UP {
		@Override
		public String javaSubclass() {
			return "$Up";
		}
	}, DOWN {
		@Override
		public String javaSubclass() {
			return "";
		}
	}, NONE {
		@Override
		public String javaSubclass() {
			return "";
		}
	};

	public abstract String javaSubclass();
}
