// Browser-only simulation by Justin Spratt. This is not an authorization boundary.
export const products = [
  { id: 'PROD-PAINT', name: 'Classroom paint set', cents: 2510 },
  { id: 'PROD-PAPER', name: 'Workshop paper pack', cents: 2000 }
];
export const agencies = { 'AGENCY-A': 'Cedar School District', 'AGENCY-B': 'Maple Public Library' };
export const money = cents => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(cents / 100);
export function initialState() {
  return { sequence: 300, events: [], orders: [
    { id: 'ORDER-A100', agency: 'AGENCY-A', description: 'Classroom art supplies', status: 'OPEN', items: [{ product: 'PROD-PAINT', quantity: 5, cents: 2510 }] },
    { id: 'ORDER-B200', agency: 'AGENCY-B', description: 'Community workshop supplies', status: 'OPEN', items: [{ product: 'PROD-PAPER', quantity: 4, cents: 2000 }] }
  ] };
}
export const total = items => items.reduce((sum, item) => sum + item.quantity * item.cents, 0);
function requireBuyer(agency, role) {
  if (!Object.hasOwn(agencies, agency)) throw new Error('Choose a valid agency.');
  if (role !== 'BUYER') throw new Error('Viewers can read orders. Switch to Buyer to make changes.');
}
export function createOrder(state, agency, role, description, quantities) {
  requireBuyer(agency, role);
  description = description.trim();
  if (!description || description.length > 120) throw new Error('Enter a description between 1 and 120 characters.');
  const items = products.map((product, index) => {
    const quantity = quantities[index];
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 1000) throw new Error('Use whole quantities from 0 to 1,000.');
    return { product: product.id, quantity, cents: product.cents };
  }).filter(item => item.quantity > 0);
  if (!items.length) throw new Error('Add at least one product.');
  const order = { id: `ORDER-${++state.sequence}`, agency, description, items, status: 'OPEN' };
  state.orders.unshift(order);
  state.events.unshift({ agency, order: order.id, action: 'Created', time: new Date().toISOString() });
  return order;
}
export function cancelOrder(state, agency, role, id) {
  requireBuyer(agency, role);
  const order = state.orders.find(order => order.id === id && order.agency === agency);
  if (!order || order.status !== 'OPEN') throw new Error('This agency has no open order with that ID.');
  order.status = 'CANCELLED';
  state.events.unshift({ agency, order: id, action: 'Cancelled', time: new Date().toISOString() });
}
export function commitments(state, agency) {
  return products.map(product => {
    const items = state.orders.filter(order => order.agency === agency && order.status === 'OPEN')
      .flatMap(order => order.items).filter(item => item.product === product.id);
    return { name: product.name, quantity: items.reduce((sum, item) => sum + item.quantity, 0), cents: total(items) };
  }).filter(item => item.quantity > 0);
}
