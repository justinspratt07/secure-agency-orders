import { initialState, products, money, total, createOrder, cancelOrder, commitments } from './model.mjs';
const $ = id => document.getElementById(id);
let state = initialState();
const node = (tag, text, className) => {
  const element = document.createElement(tag);
  if (text !== undefined) element.textContent = text;
  if (className) element.className = className;
  return element;
};
const quantities = () => [$('paint'), $('paper')].map(input => input.value === '' ? NaN : Number(input.value));
function updateTotal() {
  const values = quantities();
  $('total').textContent = values.every(value => Number.isInteger(value) && value >= 0 && value <= 1000)
    ? money(values.reduce((sum, quantity, index) => sum + quantity * products[index].cents, 0)) : '—';
}
function announce(text) { $('feedback').textContent = text; }
function render() {
  const agency = $('agency').value, buyer = $('role').value === 'BUYER';
  const orders = state.orders.filter(order => order.agency === agency);
  $('count').textContent = `${orders.length} ${orders.length === 1 ? 'order' : 'orders'}`;
  $('orders').replaceChildren(...orders.map(order => {
    const row = node('tr');
    row.append(node('td', order.id), node('td', order.description), node('td', money(total(order.items))));
    const status = node('td');
    status.append(node('span', order.status === 'OPEN' ? 'Open' : 'Cancelled', `badge ${order.status === 'OPEN' ? '' : 'cancelled'}`));
    const action = node('td');
    if (order.status === 'OPEN') {
      const button = node('button', 'Cancel');
      button.type = 'button'; button.disabled = !buyer;
      button.setAttribute('aria-label', `Cancel ${order.id}`);
      button.addEventListener('click', () => {
        try { cancelOrder(state, $('agency').value, $('role').value, order.id); render(); announce(`${order.id} cancelled. Open commitments and audit trail updated.`); }
        catch (error) { announce(error.message); }
      });
      action.append(button);
    }
    row.append(status, action); return row;
  }));
  const report = commitments(state, agency);
  $('commitments').replaceChildren(...(report.length ? report.map(item => {
    const row = node('div', undefined, 'commitment');
    row.append(node('span', item.name), node('span', money(item.cents)), node('span', `${item.quantity} ${item.quantity === 1 ? 'unit' : 'units'}`)); return row;
  }) : [node('div', 'No open commitments for this agency.', 'empty')]));
  const events = state.events.filter(event => event.agency === agency);
  $('audit').replaceChildren(...(events.length ? events.map(event => {
    const row = node('div', undefined, 'event');
    const time = node('time', new Date(event.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    time.dateTime = event.time;
    row.append(node('span', `${event.action} ${event.order} · Buyer`), time); return row;
  }) : [node('div', 'Create or cancel an order to see an event.', 'empty')]));
  $('fields').disabled = !buyer; $('viewer').hidden = buyer;
}
$('create-form').addEventListener('submit', event => {
  event.preventDefault();
  try {
    const order = createOrder(state, $('agency').value, $('role').value, $('description').value, quantities());
    render(); announce(`${order.id} created for ${money(total(order.items))}. Audit record added.`);
  } catch (error) { announce(error.message); }
});
for (const id of ['paint', 'paper']) $(id).addEventListener('input', updateTotal);
for (const id of ['agency', 'role']) $(id).addEventListener('change', () => { announce(''); render(); });
$('reset').addEventListener('click', () => {
  state = initialState(); $('agency').value = 'AGENCY-A'; $('role').value = 'BUYER';
  $('create-form').reset(); render(); updateTotal(); announce('Demo reset to the original sample data.');
});
render(); updateTotal();
