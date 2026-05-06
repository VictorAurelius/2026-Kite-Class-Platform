import '@testing-library/jest-dom';

// jsdom doesn't implement scrollIntoView etc.; minimal shims for our tests.
if (typeof Element !== 'undefined') {
  Element.prototype.scrollIntoView = () => {};
}
