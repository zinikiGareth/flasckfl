package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.ziniki.server.NewConnectionHandler;
import org.ziniki.server.TDAServer;
import org.ziniki.server.di.DehydratedHandler;
import org.ziniki.server.di.Instantiator;
import org.ziniki.server.di.MakeAHandler;
import org.ziniki.server.grizzly.GrizzlyTDAServer;
import org.ziniki.server.grizzly.GrizzlyTDAWebSocketHandler;
import org.ziniki.server.path.PathTree;
import org.ziniki.server.path.SimplePathTree;
import org.ziniki.server.tda.Transport;
import org.ziniki.server.tda.WSReceiver;
import org.ziniki.servlet.tda.RequestProcessor;
import org.ziniki.ziwsh.intf.WSProcessor;

public class JSTestServer {
	private final File basePath;
	private final File flasckPath;
	private TDAServer server;
	private PathTree<RequestProcessor> tree;
	private PathTree<WSProcessor> wstree;

	public JSTestServer(int port, File root, File flasck) {
		this.basePath = root;
		this.flasckPath = flasck;
		server = new GrizzlyTDAServer(port);
		tree = new SimplePathTree<>();
		wstree = new SimplePathTree<>();
	}
	
	public TDAServer server() {
		return server;
	}
	
	public void addWebTree(String path, MakeAHandler<RequestProcessor> handler) {
		tree.add(path, handler);
	}
	
	public void addWSTree(String path, MakeAHandler<WSProcessor> handler) {
		wstree.add(path, handler);
	}
	
	public void configure() throws Exception {
		Map<String, Object> items = new TreeMap<>();
		{
			Map<String, Object> map = new TreeMap<>();
			map.put("class", RunTestHandler.class.getName());
			map.put("path", basePath);
			map.put("flasck", flasckPath);

			addWebTree("/test/*", new DehydratedHandler<>(new Instantiator("test", map), items));
		}
	}

	public void go() throws IOException {
		server.httpMappingTree(tree);
		NewConnectionHandler<? extends WSReceiver> handler = new NewConnectionHandler<WSReceiver>() {
			@Override
			public void newConnection(Transport transport, WSReceiver handler) {
				transport.addReceiver(handler);
			}
		};
		server.wsMappingTree(new GrizzlyTDAWebSocketHandler(), wstree, handler);
		server.start();
	}

	public void stop() {
		server.stop(1, TimeUnit.SECONDS);
	}

	public boolean isRunning() {
		return server.isAlive();
	}

}
