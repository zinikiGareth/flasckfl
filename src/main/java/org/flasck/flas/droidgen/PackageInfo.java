package org.flasck.flas.droidgen;

public class PackageInfo {
	final String local;
	final String remote;
	final int version;

	public PackageInfo(String local, String remote, int version) {
		this.local = local;
		this.remote = remote;
		this.version = version;
	}
	
	@Override
	public String toString() {
		return local + "=" + remote + ":" + version;
	}
}
