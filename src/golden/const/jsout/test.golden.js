if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.x = function() {
  "use strict";
  return 32;
}

test.golden;
