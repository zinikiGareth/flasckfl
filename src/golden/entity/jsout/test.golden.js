if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Storable = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Storable';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
    if (v0.value) {
      this.value = v0.value;
    }
  }
  else {
  }
}

test.golden.Storable = function(v0) {
  "use strict";
  return new test.golden._Storable({value: v0});
}

test.golden;
