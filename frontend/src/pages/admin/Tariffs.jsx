import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../services/api';
import { Badge, Card, DataTable, Loading, Notice, PageHeader } from '../../components/ui';
import { dateStr, todayISO } from '../../utils/format';

const defaultTiers = [
  { fromKl: 0, toKl: 10, ratePerKl: 20 },
  { fromKl: 10, toKl: 20, ratePerKl: 30 },
  { fromKl: 20, toKl: '', ratePerKl: 40 },
];

export default function Tariffs() {
  const [plans, setPlans] = useState([]);
  const [apartments, setApartments] = useState([]);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const load = useCallback(async () => {
    try {
      const [p, a] = await Promise.all([api.get('/tariffs'), api.get('/apartments')]);
      setPlans(p.data); setApartments(a.data);
    } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);

  const openNew = () => setForm({ apartmentId: apartments[0]?.id || '', name: '', effectiveFrom: todayISO(), active: true, tiers: defaultTiers.map((t) => ({ ...t })) });
  const openEdit = (p) => setForm({ id: p.id, apartmentId: p.apartmentId, name: p.name, effectiveFrom: p.effectiveFrom, active: p.active, tiers: p.tiers.map((t) => ({ fromKl: t.fromKl, toKl: t.toKl ?? '', ratePerKl: t.ratePerKl })) });
  const setTier = (i, k, v) => setForm({ ...form, tiers: form.tiers.map((t, idx) => (idx === i ? { ...t, [k]: v } : t)) });

  const save = async (e) => {
    e.preventDefault();
    setError(''); setInfo('');
    const payload = {
      apartmentId: Number(form.apartmentId), name: form.name, effectiveFrom: form.effectiveFrom, active: form.active,
      tiers: form.tiers.map((t) => ({ fromKl: Number(t.fromKl), toKl: t.toKl === '' || t.toKl === null ? null : Number(t.toKl), ratePerKl: Number(t.ratePerKl) })),
    };
    try {
      if (form.id) await api.put(`/tariffs/${form.id}`, payload); else await api.post('/tariffs', payload);
      setForm(null); setInfo('Tariff saved.'); load();
    } catch (err) { setError(errorMessage(err)); }
  };

  const remove = async (p) => {
    if (!window.confirm(`Delete tariff "${p.name}"?`)) return;
    try { await api.delete(`/tariffs/${p.id}`); load(); } catch (err) { setError(errorMessage(err)); }
  };

  const columns = [
    { key: 'name', label: 'Plan' },
    { key: 'effectiveFrom', label: 'Effective from', render: (r) => dateStr(r.effectiveFrom) },
    { key: 'active', label: 'Status', render: (r) => <Badge value={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
    { key: 'tiers', label: 'Tiers (kL → ₹/kL)', render: (r) => r.tiers.map((t) => `${t.fromKl}–${t.toKl ?? '∞'}: ₹${t.ratePerKl}`).join('  |  ') },
    { key: 'a', label: '', align: 'right', render: (r) => (
      <div className="row-actions"><button className="btn small" onClick={() => openEdit(r)}>Edit</button><button className="btn small danger" onClick={() => remove(r)}>Delete</button></div>) },
  ];

  return (
    <>
      <PageHeader title="Tariff management" subtitle="Tiers are read from the database by the billing engine. Only one plan per apartment is active."
        actions={<button className="btn primary" onClick={openNew}>+ New tariff</button>} />
      <Notice>{error}</Notice>
      <Notice type="success">{info}</Notice>
      {form && (
        <Card title={form.id ? 'Edit tariff' : 'New tariff'}>
          <form onSubmit={save}>
            <div className="form-grid">
              <label><span>Apartment *</span>
                <select required disabled={Boolean(form.id)} value={form.apartmentId} onChange={(e) => setForm({ ...form, apartmentId: e.target.value })}>
                  {apartments.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                </select></label>
              <label><span>Plan name *</span><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
              <label><span>Effective from *</span><input type="date" required value={form.effectiveFrom} onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} /></label>
              <label className="check"><span>Active</span><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /></label>
            </div>
            <h4>Tiers</h4>
            <p className="muted">Tiers must start at 0, be contiguous (each “from” equals the previous “to”) and the last tier must be open-ended (leave “to” empty).</p>
            {form.tiers.map((t, i) => (
              <div className="tier-row" key={i}>
                <span>Tier {i + 1}</span>
                <label><span>From kL</span><input type="number" step="0.001" min="0" required value={t.fromKl} onChange={(e) => setTier(i, 'fromKl', e.target.value)} /></label>
                <label><span>To kL</span><input type="number" step="0.001" min="0" placeholder="∞" value={t.toKl} onChange={(e) => setTier(i, 'toKl', e.target.value)} /></label>
                <label><span>Rate ₹/kL</span><input type="number" step="0.01" min="0" required value={t.ratePerKl} onChange={(e) => setTier(i, 'ratePerKl', e.target.value)} /></label>
                <button type="button" className="btn small danger" disabled={form.tiers.length < 2} onClick={() => setForm({ ...form, tiers: form.tiers.filter((_, idx) => idx !== i) })}>Remove</button>
              </div>
            ))}
            <div className="form-actions">
              <button type="button" className="btn" onClick={() => setForm({ ...form, tiers: [...form.tiers, { fromKl: '', toKl: '', ratePerKl: '' }] })}>+ Add tier</button>
              <button className="btn primary">Save tariff</button>
              <button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button>
            </div>
          </form>
        </Card>
      )}
      <Card>{loading ? <Loading /> : <DataTable columns={columns} rows={plans} empty="No tariff configured yet — billing needs an active tariff." />}</Card>
    </>
  );
}
