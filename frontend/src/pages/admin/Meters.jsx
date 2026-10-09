import CrudPage from '../../components/CrudPage';
import { Badge } from '../../components/ui';
import { dateStr, num } from '../../utils/format';

const columns = [
  { key: 'meterNumber', label: 'Meter ID' }, { key: 'flatNumber', label: 'Flat' },
  { key: 'installationDate', label: 'Installed', render: (r) => dateStr(r.installationDate) },
  { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
  { key: 'initialReading', label: 'Initial (kL)', align: 'right', render: (r) => num(r.initialReading, 3) },
  { key: 'currentReading', label: 'Current (kL)', align: 'right', render: (r) => num(r.currentReading, 3) },
];
const fields = [
  { name: 'meterNumber', label: 'Meter ID / serial', required: true },
  { name: 'householdId', label: 'Household', type: 'select', numeric: true, required: true, optionsFrom: { endpoint: '/households', label: (h) => `${h.flatNumber} — ${h.apartmentName}` } },
  { name: 'installationDate', label: 'Installation date', type: 'date', required: true },
  { name: 'status', label: 'Status', type: 'select', required: true, options: [{ value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' }, { value: 'FAULTY', label: 'Faulty' }] },
  { name: 'initialReading', label: 'Initial reading (kL)', type: 'number', step: '0.001', min: 0, required: true },
];

export default function Meters() {
  return <CrudPage title="Water meters" subtitle="One meter per household" endpoint="/meters" columns={columns} fields={fields}
    defaults={{ status: 'ACTIVE' }} deletable={false} lockOnEdit={['householdId']} />;
}
