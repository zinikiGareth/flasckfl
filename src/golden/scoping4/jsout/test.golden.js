if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Card = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Card';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
  this._contracts['test.golden.DataStore'] = test.golden.Card._C0.apply(this);
  this.ds = this._contracts['test.golden.DataStore'];
}

test.golden._Card.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  return msgs.curr;
}

test.golden.Card = function(v0) {
  "use strict";
  return new test.golden._Card(v0);
}

test.golden.Card.__C0 = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Card._C0';
  this._card = v0;
  this._special = 'contract';
  this._contract = 'test.golden.DataStore';
}

test.golden.Card._C0 = function() {
  "use strict";
  return new test.golden.Card.__C0(this);
}

if (typeof test.golden.Card === 'undefined') {
  test.golden.Card = function() {
  }
}

if (typeof test.golden.Card.ItemHandler === 'undefined') {
  test.golden.Card.ItemHandler = function() {
  }
}

test.golden.Card._ItemHandler = function(v0, v1) {
  "use strict";
  this._ctor = 'test.golden.Card.ItemHandler';
  this._card = v0;
  this._special = 'handler';
  this._contract = 'test.golden.KVUpdate';
  this.ck = v1;
}

test.golden.Card.ItemHandler = function(v0) {
  "use strict";
  return new test.golden.Card._ItemHandler(this, v0);
}

test.golden._Card.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Card._ItemHandler.prototype.update = function(v1) {
  "use strict";
  v1 = FLEval.head(v1);
  if (v1 instanceof FLError) {
    return v1;
  }
  if (FLEval.isInteger(v1)) {
    var v2 = FLEval.closure(test.golden.Card.ItemHandler.update.x, v1);
    var v3 = FLEval.closure(Cons, v2, Nil);
    var v4 = FLEval.closure(Send, this._card.ds, 'insert', v3);
    return FLEval.closure(Cons, v4, Nil);
  }
  return FLEval.error("test.golden.Card.ItemHandler.update: case not handled");
}

test.golden.Card.ItemHandler.update.x = function(s0) {
  "use strict";
  return FLEval.closure(FLEval.plus, this.ck, s0);
}

test.golden;
