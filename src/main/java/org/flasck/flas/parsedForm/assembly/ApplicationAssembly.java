package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;
import org.flasck.flas.parser.assembly.AssemblyDefinitionConsumer;

public class ApplicationAssembly extends Assembly implements ApplicationElementConsumer {
	private String baseuri = "";
	private String title;
	private ApplicationRouting routing;
	private AssemblyDefinitionConsumer consumer;
	private ApplicationZiwsh ziwshModel;

	public ApplicationAssembly(InputPosition loc, AssemblyName assemblyName, AssemblyDefinitionConsumer consumer) {
		super(loc, assemblyName);
		this.consumer = consumer;
	}

	@Override
	public void baseuri(String s) {
		this.baseuri = s;
	}
	
	@Override
	public void title(String s) {
		this.title = s;
	}
	
	@Override
	public void routes(ApplicationRouting routing) {
		this.routing = routing;
		consumer.routingTable(routing);
	}

	@Override
	public void ziwsh(ApplicationZiwsh ziwshModel) {
		this.ziwshModel = ziwshModel;
		consumer.ziwshModel(ziwshModel);
	}

	public String getBaseUri() {
		return baseuri;
	}

	public String getTitle() {
		return title;
	}

	public ApplicationRouting routing() {
		return routing;
	}
	
	public ApplicationZiwsh ziwsh() {
		return ziwshModel;
	}
	
	@Override
	public String toString() {
		return "AppAssembly[" + assemblyName.uniqueName() + "]";
	}
}
