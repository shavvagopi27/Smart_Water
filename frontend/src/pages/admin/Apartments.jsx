import CrudPage from '../../components/CrudPage';
import { dateStr } from '../../utils/format';

const columns = [
  { key: 'name', label: 'Name' }, { key: 'address', label: 'Address' }, { key: 'city', label: 'City' },
  { key: 'householdCount', label: 'Households', align: 'right' },
  { key: 'createdAt', label: 'Created', render: (r) => dateStr(r.createdAt) },
];
const fields = [
  { name: 'name', label: 'Apartment name', required: true },
  { name: 'address', label: 'Address', required: true },
  { name: 'city', label: 'City', required: true },
];

export default function Apartments() {
  return <CrudPage title="Apartments" subtitle="Apartment communities managed in the system" endpoint="/apartments" columns={columns} fields={fields} />;
}
