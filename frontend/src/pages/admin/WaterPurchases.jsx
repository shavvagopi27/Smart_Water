import CrudPage from '../../components/CrudPage';
import { Badge } from '../../components/ui';
import { dateStr, money, num } from '../../utils/format';

const columns = [
  { key: 'purchaseDate', label: 'Date', render: (r) => dateStr(r.purchaseDate) },
  { key: 'supplier', label: 'Supplier' },
  { key: 'supplyType', label: 'Type', render: (r) => <Badge value={r.supplyType} /> },
  { key: 'volumeKl', label: 'Volume (kL)', align: 'right', render: (r) => num(r.volumeKl, 3) },
  { key: 'unitCost', label: 'Unit cost', align: 'right', render: (r) => money(r.unitCost) },
  { key: 'totalCost', label: 'Total cost', align: 'right', render: (r) => money(r.totalCost) },
  { key: 'billingCycleId', label: 'Cycle', render: (r) => r.billingCycleId || 'by date' },
];
const fields = [
  { name: 'apartmentId', label: 'Apartment', type: 'select', numeric: true, required: true, optionsFrom: { endpoint: '/apartments', label: (a) => a.name } },
  { name: 'supplier', label: 'Supplier', required: true },
  { name: 'supplyType', label: 'Supply type', type: 'select', required: true, options: [{ value: 'TANKER', label: 'Tanker delivery' }, { value: 'MUNICIPAL', label: 'Municipal supply' }] },
  { name: 'purchaseDate', label: 'Purchase date', type: 'date', required: true },
  { name: 'volumeKl', label: 'Volume (kL)', type: 'number', step: '0.001', min: 0, required: true },
  { name: 'unitCost', label: 'Unit cost (₹/kL)', type: 'number', step: '0.01', min: 0, required: true },
  { name: 'totalCost', label: 'Total cost (₹) — blank = volume × unit cost', type: 'number', step: '0.01', min: 0 },
  { name: 'billingCycleId', label: 'Billing cycle (optional)', type: 'select', numeric: true, placeholder: 'Assign by purchase date', optionsFrom: { endpoint: '/billing-cycles', label: (c) => `${c.name} (${c.status})` } },
];

export default function WaterPurchases() {
  return <CrudPage title="Bulk water purchases" subtitle="Tanker and municipal supply — the cost is shared between households at billing time"
    endpoint="/water-purchases" columns={columns} fields={fields} defaults={{ supplyType: 'TANKER' }} lockOnEdit={['apartmentId']} />;
}
