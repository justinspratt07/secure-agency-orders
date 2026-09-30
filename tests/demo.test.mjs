import test from 'node:test';
import assert from 'node:assert/strict';
import { initialState, total, createOrder, cancelOrder, commitments } from '../demo/model.mjs';
test('create uses exact catalog cents and commits one audit event', () => {
  const state = initialState();
  const order = createOrder(state, 'AGENCY-A', 'BUYER', 'Materials', [2, 3]);
  assert.equal(total(order.items), 11020);
  assert.equal(state.events.length, 1);
  assert.deepEqual(commitments(state, 'AGENCY-A').map(x => [x.quantity, x.cents]), [[7,17570],[3,6000]]);
  assert.equal(commitments(state, 'AGENCY-B')[0].cents, 8000);
});
test('viewer cannot create or cancel and failed writes leave state unchanged', () => {
  const state = initialState(), before = structuredClone(state);
  assert.throws(() => createOrder(state, 'AGENCY-A', 'VIEWER', 'Materials', [2,3]));
  assert.throws(() => cancelOrder(state, 'AGENCY-A', 'VIEWER', 'ORDER-A100'));
  assert.deepEqual(state, before);
});
test('cross-agency and repeated cancellation are rejected', () => {
  const state = initialState();
  assert.throws(() => cancelOrder(state, 'AGENCY-A', 'BUYER', 'ORDER-B200'));
  cancelOrder(state, 'AGENCY-A', 'BUYER', 'ORDER-A100');
  assert.deepEqual(commitments(state, 'AGENCY-A'), []);
  assert.throws(() => cancelOrder(state, 'AGENCY-A', 'BUYER', 'ORDER-A100'));
  assert.equal(state.events.length, 1);
});
test('invalid baskets are rejected without partial orders or audit records', () => {
  const state = initialState(), before = structuredClone(state);
  for (const values of [[0,0],[-1,2],[1.5,2],[NaN,2],[1001,2],[Infinity,1]]) {
    assert.throws(() => createOrder(state, 'AGENCY-A', 'BUYER', 'Materials', values));
  }
  assert.throws(() => createOrder(state, 'AGENCY-A', 'BUYER', '  ', [1,1]));
  assert.throws(() => createOrder(state, 'AGENCY-A', 'BUYER', 'x'.repeat(121), [1,1]));
  assert.deepEqual(state, before);
});
test('description remains plain data and reset returns independent fixtures', () => {
  const state = initialState(), text = '<img src=x onerror=alert(1)>; DROP TABLE orders;';
  assert.equal(createOrder(state, 'AGENCY-A', 'BUYER', text, [1,0]).description, text);
  assert.equal(initialState().orders.length, 2);
  assert.equal(initialState().events.length, 0);
});
