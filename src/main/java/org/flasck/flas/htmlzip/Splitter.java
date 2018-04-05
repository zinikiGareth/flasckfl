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

public class Splitter {
	private final Sink sink;

	public Splitter(Sink sink) {
		this.sink = sink;
	}
	
	public void extract(ZipFile zf, ZipEntry ze) throws IOException {
		try {
			InputStream str = zf.getInputStream(ze);
			final String name = ze.getName();
			if (name.endsWith(".html")) {
//				System.out.println("Processing " + ze.getName());
				extract(name, str);
			}
			str.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void extract(final String name, InputStream stream) throws IOException {
		sink.beginFile(name);
		Document doc = Jsoup.parse(stream, "UTF-8", "/");
		dealWithHTMLNodes(name, doc.children(), 0);
		sink.fileEnd();
	}
	
	/** So I think we have three basic cases we want to handle:
	 * data-flas-card (was block): define a card, which means that we want to build a new function/class which has
	 *   the code to generate a string and put it inside a div (passed in as a parameter)
	 * id: just for doing things like formatting and adding events.  The id needs to be replaced with a document-unique
	 *   one, but needs to be able to be accessed from the object by the original name.
	 * data-flas-hole: this needs two ranges, one for the "id" (which goes in the place of the data-flas-hole attribute)
	 *   and one for the actual hole, which needs to be removed.  It's the (generated, unique) id that will be used
	 *   to populate it again with other cards or with text.
	 * @param fileName
	 * @param nodes
	 * @param depth
	 * @throws FileNotFoundException
	 */

	private void dealWithHTMLNodes(String fileName, List<Element> nodes, int depth) {
		for (Element elt : nodes) {
//			if (elt.tagName().equals("body")) {
//				sink.block("body-" + fileName, elt.range().from(), elt.range().to());
//			}
			for (Attribute a : elt.attributes()) {
				if (a.getKey().equals("data-flas-card")) {
					sink.card(a.getValue(), elt.range().from(), elt.range().to());
					sink.dodgyAttr(a.range().from(), a.range().to());
				}
				if (a.getKey().equals("data-flas-hole")) {
					sink.hole(a.getValue(), elt.innerRange().from(), elt.innerRange().to());
					sink.dodgyAttr(a.range().from(), a.range().to());
				}
				if (a.getKey().equals("id")) {
					sink.identifyElement(a.getValue(), elt.innerRange().from(), elt.innerRange().to());
					sink.dodgyAttr(a.range().from(), a.range().to());
				}
				if (a.getKey().equals("data-flas-remove")) {
					String val = a.getValue();
					sink.dodgyAttr(a.range().from(), a.range().to());
					if (val != null && val.length() > 0) {
						String[] ras = val.split("\\s");
						for (String s : ras) {
							for (Attribute as : elt.attributes()) {
								if (as.getKey().equals(s))
									sink.dodgyAttr(as.range().from(), as.range().to());
							}
						}
					}
				}
			}
			dealWithHTMLNodes(fileName, elt.children(), depth + 1);
		}
	}
}
