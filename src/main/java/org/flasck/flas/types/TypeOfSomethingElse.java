package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;

/** The idea here is that during the early stages of compilation (i.e. before typechecking)
 * you may not know WHAT the type is, but you have an algorithm for finding out, i.e.
 * it's the same as the type of something else.
 * 
 * With the name of the other item, you can assume that typechecking can follow the inference.
 *
 * <p>
 * &copy; 2015 Ziniki Infrastructure Software, LLC.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class TypeOfSomethingElse extends Type {

	public TypeOfSomethingElse(InputPosition loc, String name) {
		super(null, loc, WhatAmI.SOMETHINGELSE, name, null);
	}
	
	public String other() {
		return name;
	}

}
