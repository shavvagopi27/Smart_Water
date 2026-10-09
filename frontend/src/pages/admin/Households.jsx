import CrudPage from '../../components/CrudPage';
import { Badge } from '../../components/ui';

const columns = [
  { key: 'flatNumber', label: 'Flat' }, { key: 'apartmentName', label: 'Apartment' },
  { key: 'householdSize', label: 'Size', align: 'right' }, { key: 'areaSqft', label: 'Area (sq ft)', align: 'right' },
  { key: 'meterNumber', label: 'Meter', render: (r) => r.meterNumber || '—' },
  { key: 'residentNames', label: 'Residents', render: (r) => (r.residentNames.length ? r.residentNames.join(', ') : '—') },
  { key: 'active', label: 'Status', render: (r) => <Badge value={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
];
const fields = [
  { name: 'apartmentId', label: 'Apartment', type: 'select', numeric: true, required: true, optionsFrom: { endpoint: '/apartments', label: (a) => a.name } },
  { name: 'flatNumber', label: 'Flat number', required: true },
  { name: 'householdSize', label: 'Household size', type: 'number', min: 1, required: true },
  { name: 'areaSqft', label: 'Flat area (sq ft) — used for fallback cost sharing', type: 'number', step: '0.01', min: 1, required: true },
  { name: 'active', label: 'Active', type: 'checkbox' },
];

export default function Households() {
  return <CrudPage title="Households" subtitle="Flats in the apartment" endpoint="/households" columns={columns} fields={fields}
    defaults={{ active: true }} lockOnEdit={['apartmentId']} />;
}
