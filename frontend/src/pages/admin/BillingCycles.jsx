import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../services/api';
import { Badge, Card, DataTable, Loading, Notice, PageHeader } from '../../components/ui';
import { dateStr, money } from '../../utils/format';

const monthDefaults = () => {
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const iso = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  return { name: `Water bill ${iso(first).slice(0, 7)}`, startDate: iso(first), endDate: iso(last) };
};

export default function BillingCycles() {
  const [cycles, setCycles] = useState([]);
  const [apartments, setApartments] = useState([]);
  const [households, setHouseholds] = useState([]);
  const [form, setForm] = useState(null);
  const [adj, setAdj] = useState({}); // { [cycleId]: [{householdId, amount, reason}] }
  const [adjForm, setAdjForm] = useState({ householdId: '', amount: '', reason: '' });
  const [adjOpen, setAdjOpen] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const load = useCallback(async () => {
    try {
      const [c, a, h] = await Promise.all([api.get('/billing-cycles'), api.get('/apartments'), api.get('/households')]);
      setCycles(c.data); setApartments(a.data); setHouseholds(h.data);
    } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const run = async (fn, okMsg) => {
    setError(''); setInfo('');
    try { await fn(); setInfo(okMsg); load(); } catch (e) { setError(errorMessage(e)); }
  };

  const create = (e) => {
    e.preventDefault();
    run(async () => { await api.post('/billing-cycles', { ...form, apartmentId: Number(form.apartmentId) }); setForm(null); }, 'Billing cycle created (OPEN).');
  };

  const addAdjustment = (cycleId) => {
    if (!adjForm.householdId || adjForm.amount === '' || !adjForm.reason) return;
    setAdj({ ...adj, [cycleId]: [...(adj[cycleId] || []), { householdId: Number(adjForm.householdId), amount: Number(adjForm.amount), reason: adjForm.reason }] });
    setAdjForm({ householdId: '', amount: '', reason: '' });
  };

  const columns = [
    { key: 'name', label: 'Cycle' },
    { key: 'period', label: 'Period', render: (r) => `${dateStr(r.startDate)} – ${dateStr(r.endDate)}` },
    { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
    { key: 'totalPurchaseCost', label: 'Water purchased (₹)', align: 'right', render: (r) => money(r.totalPurchaseCost) },
    { key: 'invoiceCount', label: 'Invoices', align: 'right' },
    { key: 'totalInvoiced', label: 'Invoiced total', align: 'right', render: (r) => money(r.totalInvoiced) },
    {
      key: 'actions', label: '', align: 'right', render: (r) => (
        <div className="row-actions">
          {r.status === 'OPEN' && <>
            <button className="btn small" onClick={() => setAdjOpen(adjOpen === r.id ? null : r.id)}>Adjustments</button>
            <button className="btn small" onClick={() => run(() => api.post(`/billing-cycles/${r.id}/generate`, { adjustments: adj[r.id] || [] }), 'Invoices generated.')}>Generate invoices</button>
            <button className="btn small primary" onClick={() => window.confirm('Finalize this cycle? It will be locked and bills e-mailed.') && run(() => api.put(`/billing-cycles/${r.id}/finalize`), 'Cycle finalized and bills sent.')}>Finalize</button>
          </>}
          {r.status === 'FINALIZED' && <button className="btn small" onClick={() => run(() => api.put(`/billing-cycles/${r.id}/archive`), 'Cycle archived.')}>Archive</button>}
          <Link className="btn small" to="/admin/invoices">Invoices</Link>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Billing cycles" subtitle="OPEN → generate invoices → FINALIZE (locks data and e-mails bills) → ARCHIVE"
        actions={<button className="btn primary" onClick={() => setForm({ apartmentId: apartments[0]?.id || '', ...monthDefaults() })}>+ New cycle</button>} />
      <Notice>{error}</Notice>
      <Notice type="success">{info}</Notice>
      {form && (
        <Card title="New billing cycle">
          <form className="form-grid" onSubmit={create}>
            <label><span>Apartment *</span><select required value={form.apartmentId} onChange={(e) => setForm({ ...form, apartmentId: e.target.value })}>
              {apartments.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select></label>
            <label><span>Name *</span><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
            <label><span>Start date *</span><input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
            <label><span>End date *</span><input type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label>
            <div className="form-actions"><button className="btn primary">Create</button><button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button></div>
          </form>
        </Card>
      )}
      {adjOpen && (
        <Card title="Invoice adjustments for this generation (credits are negative)">
          <div className="form-grid">
            <label><span>Household</span><select value={adjForm.householdId} onChange={(e) => setAdjForm({ ...adjForm, householdId: e.target.value })}>
              <option value="">Select…</option>{households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber}</option>)}</select></label>
            <label><span>Amount (₹)</span><input type="number" step="0.01" value={adjForm.amount} onChange={(e) => setAdjForm({ ...adjForm, amount: e.target.value })} /></label>
            <label><span>Reason</span><input value={adjForm.reason} onChange={(e) => setAdjForm({ ...adjForm, reason: e.target.value })} /></label>
            <div className="form-actions"><button className="btn" onClick={() => addAdjustment(adjOpen)}>Add adjustment</button></div>
          </div>
          <DataTable rowKey="reason" rows={adj[adjOpen] || []} empty="No adjustments queued. They are applied when you press “Generate invoices”."
            columns={[{ key: 'householdId', label: 'Flat', render: (r) => households.find((h) => h.id === r.householdId)?.flatNumber }, { key: 'amount', label: 'Amount', render: (r) => money(r.amount) }, { key: 'reason', label: 'Reason' }]} />
        </Card>
      )}
      <Card>{loading ? <Loading /> : <DataTable columns={columns} rows={cycles} empty="No billing cycles yet." />}</Card>
    </>
  );
}
