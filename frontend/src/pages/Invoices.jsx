import { useState } from 'react';
import api, { downloadInvoicePdf, errorMessage } from '../services/api';
import useFetch from '../hooks/useFetch';
import { useAuth } from '../hooks/useAuth';
import { Badge, Card, DataTable, Loading, Notice, PageHeader } from '../components/ui';
import { dateStr, kl, money } from '../utils/format';

/** Shared by residents (/invoices) and admins (/admin/invoices). */
export default function Invoices() {
  const { isAdmin } = useAuth();
  const { data, loading, error, reload } = useFetch('/invoices');
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const [open, setOpen] = useState(null);

  const act = async (fn, okMsg) => {
    setErr(''); setMsg('');
    try { await fn(); setMsg(okMsg); reload(); } catch (e) { setErr(errorMessage(e)); }
  };

  const columns = [
    { key: 'invoiceNumber', label: 'Invoice' },
    ...(isAdmin ? [{ key: 'flatNumber', label: 'Flat' }, { key: 'residentName', label: 'Resident' }] : []),
    { key: 'cycleName', label: 'Billing cycle' },
    { key: 'period', label: 'Period', render: (r) => `${dateStr(r.periodStart)} – ${dateStr(r.periodEnd)}` },
    { key: 'consumption', label: 'Consumption', align: 'right', render: (r) => kl(r.consumption) },
    { key: 'totalAmount', label: 'Total', align: 'right', render: (r) => money(r.totalAmount) },
    { key: 'paymentStatus', label: 'Status', render: (r) => <Badge value={r.paymentStatus} /> },
    {
      key: 'actions', label: '', align: 'right',
      render: (r) => (
        <div className="row-actions">
          <button className="btn small" onClick={() => setOpen(open === r.id ? null : r.id)}>{open === r.id ? 'Hide' : 'Details'}</button>
          <button className="btn small" onClick={() => act(() => downloadInvoicePdf(r.id, r.invoiceNumber), 'PDF downloaded.')}>PDF</button>
          {isAdmin && r.paymentStatus !== 'PAID' && <button className="btn small" onClick={() => act(() => api.put(`/invoices/${r.id}/paid`), 'Marked as paid.')}>Mark paid</button>}
          {isAdmin && <button className="btn small" onClick={() => act(() => api.post(`/invoices/${r.id}/email`), 'E-mail queued (logged only if mail is disabled).')}>E-mail</button>}
        </div>
      ),
    },
  ];
  const detail = data?.find((i) => i.id === open);

  return (
    <>
      <PageHeader title="Invoices" subtitle={isAdmin ? 'All household invoices' : 'Your invoice history'} />
      <Notice>{error || err}</Notice>
      <Notice type="success">{msg}</Notice>
      <Card>{loading ? <Loading /> : <DataTable columns={columns} rows={data} empty="No invoices yet." />}</Card>
      {detail && (
        <Card title={`Itemised: ${detail.invoiceNumber}`}>
          <DataTable rows={detail.items.map((i, k) => ({ ...i, id: k }))} columns={[
            { key: 'description', label: 'Description' },
            { key: 'quantity', label: 'Qty', align: 'right', render: (r) => r.quantity ?? '' },
            { key: 'rate', label: 'Rate', align: 'right', render: (r) => r.rate ?? '' },
            { key: 'amount', label: 'Amount', align: 'right', render: (r) => money(r.amount) },
          ]} />
          <p className="muted">Shared cost basis: <Badge value={detail.sharedCostBasis} /> · Previous reading {detail.previousReading} → current {detail.currentReading}</p>
        </Card>
      )}
    </>
  );
}
