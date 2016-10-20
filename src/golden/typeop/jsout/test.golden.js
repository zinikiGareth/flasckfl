if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._MyCard = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.MyCard';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.MyCard = function(v0) {
  "use strict";
  return new test.golden._MyCard(v0);
}

test.golden._MyCard.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.x = function() {
  "use strict";
  return Number;
}

test.golden.y = function() {
  "use strict";
  return test.golden.MyCard;
}

test.golden;
