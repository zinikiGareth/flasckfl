package org.flasck.flas.htmlzip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

class Splitter {
	private final Sink sink;

	public Splitter(Sink sink) {
		this.sink = sink;
	}
	
	public void extract(ZipFile zf, ZipEntry ze) throws IOException {
		try {
			InputStream str = zf.getInputStream(ze);
			if (ze.getName().endsWith(".html")) {
				System.out.println("Processing " + ze.getName());
				Document doc = Jsoup.parse(str, "UTF-8", "/");
				dealWithHTMLNodes(doc.children(), 0);
			}
			str.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void dealWithHTMLNodes(List<Element> nodes, int depth) throws FileNotFoundException {
		for (Element elt : nodes) {
//			if (elt.tagName().equals("body")) {
//				sink.block(elt.tagName(), elt.range().from(), elt.range().to());
//			}
			for (Attribute a : elt.attributes()) {
				if (a.getKey().equals("data-splitter-container")) {
					sink.hole(elt.innerRange().from(), elt.innerRange().to());
				}
				if (a.getKey().equals("data-splitter-template")) {
					sink.block(a.getValue(), elt.range().from(), elt.range().to());
				}
				if (a.getKey().equals("id")) {
					sink.dodgyAttr("id", a.range().from(), a.range().to());
				}
			}
			dealWithHTMLNodes(elt.children(), depth + 1);
		}
	}
}
