package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parser.assembly.ApplicationElementConsumer;
import org.flasck.flas.parser.assembly.AssemblyDefinitionConsumer;

public class ApplicationAssembly extends Assembly implements ApplicationElementConsumer {
	private String title;
	private TypeReference mainCard;
	private ApplicationRouting routing;
	private AssemblyDefinitionConsumer consumer;

	public ApplicationAssembly(InputPosition loc, AssemblyName assemblyName, AssemblyDefinitionConsumer consumer) {
		super(loc, assemblyName);
		this.consumer = consumer;
	}

	@Override
	public void title(String s) {
		this.title = s;
	}
	
	@Override
	public void mainCard(TypeReference main) {
		this.mainCard = main;
	}
	
	@Override
	public void routes(ApplicationRouting routing) {
		this.routing = routing;
		consumer.routingTable(routing);
	}

	public String getTitle() {
		return title;
	}

	public CardName mainCard() {
		return (CardName) mainCard.defn().name();
	}

	public ApplicationRouting routing() {
		return routing;
	}
}
