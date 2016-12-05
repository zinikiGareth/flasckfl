if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Getter = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Getter';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.Getter = function(v0) {
  "use strict";
  return new test.golden._Getter(v0);
}

test.golden._Getter.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Getter.prototype.choose = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (typeof v0 === 'string') {
    var v1 = FLEval.closure(FLEval.field, this.strings, 'item');
    var v2 = FLEval.closure(v1, v0);
    var v3 = FLEval.closure(Assign, this, 'curr', v2);
    return FLEval.closure(Cons, v3, Nil);
  }
  return FLEval.error("test.golden.Getter.choose: case not handled");
}

test.golden;
