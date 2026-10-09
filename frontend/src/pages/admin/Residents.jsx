import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../services/api';
import { Badge, Card, DataTable, Loading, Notice, PageHeader } from '../../components/ui';

const blank = { householdId: '', name: '', email: '', password: '', phone: '' };

export default function Residents() {
  const [residents, setResidents] = useState([]);
  const [households, setHouseholds] = useState([]);
  const [form, setForm] = useState(blank);
  const [assign, setAssign] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const load = useCallback(async () => {
    try {
      const [r, h] = await Promise.all([api.get('/users', { params: { role: 'RESIDENT' } }), api.get('/households')]);
      setResidents(r.data);
      setHouseholds(h.data);
    } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const run = async (fn, okMsg) => {
    setError(''); setInfo('');
    try { await fn(); setInfo(okMsg); load(); } catch (e) { setError(errorMessage(e)); }
  };

  const addResident = (e) => {
    e.preventDefault();
    run(async () => {
      const { householdId, ...body } = form;
      await api.post(`/households/${householdId}/residents`, { ...body, password: body.password || null, phone: body.phone || null });
      setForm(blank);
    }, 'Resident added to household.');
  };

  const columns = [
    { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'phone', label: 'Phone', render: (r) => r.phone || '—' },
    { key: 'flatNumber', label: 'Flat', render: (r) => r.flatNumber || <span className="muted">unassigned</span> },
    { key: 'enabled', label: 'Account', render: (r) => <Badge value={r.enabled ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions', label: 'Actions', render: (r) => (
        <div className="row-actions">
          <select value={assign[r.id] || ''} onChange={(e) => setAssign({ ...assign, [r.id]: e.target.value })}>
            <option value="">Assign to flat…</option>
            {households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber}</option>)}
          </select>
          <button className="btn small" disabled={!assign[r.id]}
            onClick={() => run(() => api.put(`/users/${r.id}`, { name: r.name, phone: r.phone, householdId: Number(assign[r.id]) }), 'Resident assigned.')}>Assign</button>
          {r.householdId && <button className="btn small" onClick={() => run(() => api.delete(`/households/${r.householdId}/residents/${r.id}`), 'Resident unlinked from household.')}>Unlink</button>}
          <button className="btn small" onClick={() => run(() => api.put(`/users/${r.id}`, { name: r.name, phone: r.phone, enabled: !r.enabled }), 'Account updated.')}>{r.enabled ? 'Disable' : 'Enable'}</button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Residents" subtitle="Create resident accounts for a flat or link self-registered users to their flat" />
      <Notice>{error}</Notice>
      <Notice type="success">{info}</Notice>
      <Card title="Add resident to a household">
        <form className="form-grid" onSubmit={addResident}>
          <label><span>Household *</span>
            <select required value={form.householdId} onChange={(e) => setForm({ ...form, householdId: e.target.value })}>
              <option value="">Select…</option>
              {households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber} — {h.apartmentName}</option>)}
            </select></label>
          <label><span>Full name *</span><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label><span>Email *</span><input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          <label><span>Password (new accounts, min 8)</span><input type="password" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
          <label><span>Phone</span><input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
          <div className="form-actions"><button className="btn primary">Add resident</button></div>
        </form>
      </Card>
      <Card title="All residents">{loading ? <Loading /> : <DataTable columns={columns} rows={residents} />}</Card>
    </>
  );
}
