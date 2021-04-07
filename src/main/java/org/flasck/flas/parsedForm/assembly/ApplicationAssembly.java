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

	public String getBaseUri() {
		return baseuri;
	}

	public String getTitle() {
		return title;
	}

	public ApplicationRouting routing() {
		return routing;
	}
}
