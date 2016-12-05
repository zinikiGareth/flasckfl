if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Container = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Container';
  if (v0) {
    if (v0.title) {
      this.title = v0.title;
    }
  }
  else {
  }
}

test.golden.Container = function(v0) {
  "use strict";
  return new test.golden._Container({title: v0});
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
  new test.golden._MyCard.B1(new CardArea(parent, wrapper, this));
}

test.golden._MyCard.B1 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._onAssign(this._card, 'c', test.golden._MyCard.B1.prototype._contentExpr);
  this._onAssign(this._card.c, 'title', test.golden._MyCard.B1.prototype._contentExpr);
  test.golden._MyCard.B1.prototype._contentExpr.call(this);
  this._editable(test.golden._MyCard.B1._rules);
  this._mydiv.className = 'flasck-editable';
}

test.golden._MyCard.B1.prototype = new TextArea();

test.golden._MyCard.B1.prototype.constructor = test.golden._MyCard.B1;

test.golden._MyCard.B1.prototype._contentExpr = function() {
  this._assignToText(this.contents_0());
}

test.golden._MyCard.B1._rules = {
  save: function(wrapper, text) {
    var containingObject = test.golden._MyCard.B1.editcontainer_1();
    containingObject.title = text;
    wrapper.saveObject(containingObject);
  }
}

test.golden._MyCard.B1.prototype.contents_0 = function() {
  "use strict";
  return FLEval.closure(FLEval.field, this._card.c, 'title');
}

test.golden._MyCard.B1.prototype.editcontainer_1 = function() {
  "use strict";
  return this._card.c;
}

test.golden;
