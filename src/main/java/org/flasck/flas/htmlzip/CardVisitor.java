package org.flasck.flas.htmlzip;

public interface CardVisitor {

	void consider(String file);

	void render(int from, int to);

	void done();

}
