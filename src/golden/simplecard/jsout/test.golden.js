if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Simple = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Simple';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden._Simple.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  this.hello = FLEval.full(test.golden._Simple.prototype.inits_hello.apply(this, [msgs]));
  return msgs.curr;
}

test.golden.Simple = function(v0) {
  "use strict";
  return new test.golden._Simple(v0);
}

test.golden._Simple.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Simple.B1(new CardArea(parent, wrapper, this));
}

test.golden._Simple.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._Simple.B2(this);
}

test.golden._Simple.B1.prototype = new DivArea();

test.golden._Simple.B1.prototype.constructor = test.golden._Simple.B1;

test.golden._Simple.B2 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'hello', test.golden._Simple.B2.prototype._contentExpr);
  test.golden._Simple.B2.prototype._contentExpr.call(this);
}

test.golden._Simple.B2.prototype = new TextArea();

test.golden._Simple.B2.prototype.constructor = test.golden._Simple.B2;

test.golden._Simple.B2.prototype._contentExpr = function() {
  this._assignToText(this.contents_0());
}

test.golden._Simple.B2.prototype.contents_0 = function() {
  "use strict";
  return this._card.hello;
}

test.golden.Simple.prototype.f = function() {
  "use strict";
  return 3;
}

test.golden._Simple.prototype.inits_hello = function(msgs) {
  "use strict";
  return 'hello, world';
}

test.golden;
