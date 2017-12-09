test = function() {
}

test.hello = function() {
}

test.hello._Hello = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.hello.Hello';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.hello.Hello = function(v0) {
  "use strict";
  return new test.hello._Hello(v0);
}

test.hello._Hello.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.hello._Hello.B1(new CardArea(parent, wrapper, this));
}

test.hello._Hello.B1 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('hello, world');
}

test.hello._Hello.B1.prototype = new TextArea();

test.hello._Hello.B1.prototype.constructor = test.hello._Hello.B1;

test.hello._Hello.prototype.toString = function() {
  return "Hello Card";
}
window.test = test;
