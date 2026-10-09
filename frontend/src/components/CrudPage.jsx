import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../services/api';
import { Card, DataTable, Loading, Notice, PageHeader } from './ui';

/**
 * Generic admin CRUD screen driven by configuration.
 *  columns: [{ key, label, render? }]
 *  fields:  [{ name, label, type: text|number|date|select|checkbox, required, step, options | optionsFrom }]
 *  optionsFrom: { endpoint, value = 'id', label: (item) => string }
 */
export default function CrudPage({
  title, subtitle, endpoint, columns, fields, defaults = {}, toPayload,
  creatable = true, editable = true, deletable = true, extraActions, lockOnEdit = [],
}) {
  const [rows, setRows] = useState([]);
  const [lookups, setLookups] = useState({});
  const [form, setForm] = useState(null); // { id?, values }
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(endpoint);
      setRows(data);
      setError('');
    } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, [endpoint]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    fields.filter((f) => f.optionsFrom).forEach(async (f) => {
      try {
        const { data } = await api.get(f.optionsFrom.endpoint);
        setLookups((l) => ({ ...l, [f.name]: data.map((d) => ({ value: d[f.optionsFrom.value || 'id'], label: f.optionsFrom.label(d) })) }));
      } catch { /* lookup failures are shown when saving */ }
    });
  }, [fields]);

  const optionsOf = (f) => f.options || lookups[f.name] || [];
  const openCreate = () => { setInfo(''); setForm({ values: { ...defaults } }); };
  const openEdit = (row) => {
    setInfo('');
    const values = {};
    fields.forEach((f) => { values[f.name] = row[f.name] ?? ''; });
    setForm({ id: row.id, values });
  };
  const set = (name, value) => setForm((f) => ({ ...f, values: { ...f.values, [name]: value } }));

  const convert = (values) => {
    const out = {};
    fields.forEach((f) => {
      let v = values[f.name];
      if (f.type === 'number' || f.numeric) v = v === '' || v === undefined || v === null ? null : Number(v);
      else if (f.type === 'checkbox') v = Boolean(v);
      else if (v === '') v = null;
      out[f.name] = v;
    });
    return toPayload ? toPayload(out, form?.id) : out;
  };

  const save = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const payload = convert(form.values);
      if (form.id) await api.put(`${endpoint}/${form.id}`, payload);
      else await api.post(endpoint, payload);
      setForm(null);
      setInfo(form.id ? 'Saved changes.' : 'Created successfully.');
      load();
    } catch (err) { setError(errorMessage(err)); }
  };

  const remove = async (row) => {
    if (!window.confirm('Delete this record? This cannot be undone.')) return;
    setError('');
    try {
      await api.delete(`${endpoint}/${row.id}`);
      setInfo('Deleted.');
      load();
    } catch (err) { setError(errorMessage(err)); }
  };

  const cols = [...columns, {
    key: '_actions', label: '', align: 'right',
    render: (r) => (
      <div className="row-actions">
        {extraActions && extraActions(r, load)}
        {editable && <button className="btn small" onClick={() => openEdit(r)}>Edit</button>}
        {deletable && <button className="btn small danger" onClick={() => remove(r)}>Delete</button>}
      </div>
    ),
  }];

  return (
    <>
      <PageHeader title={title} subtitle={subtitle}
        actions={creatable && <button className="btn primary" onClick={openCreate}>+ Add new</button>} />
      <Notice>{error}</Notice>
      <Notice type="success">{info}</Notice>

      {form && (
        <Card title={form.id ? 'Edit record' : 'Add new record'}>
          <form className="form-grid" onSubmit={save}>
            {fields.map((f) => (
              <label key={f.name} className={f.type === 'checkbox' ? 'check' : ''}>
                <span>{f.label}{f.required && ' *'}</span>
                {f.type === 'select' ? (
                  <select value={form.values[f.name] ?? ''} required={f.required}
                    disabled={Boolean(form.id) && lockOnEdit.includes(f.name)}
                    onChange={(e) => set(f.name, e.target.value)}>
                    <option value="">{f.placeholder || 'Select…'}</option>
                    {optionsOf(f).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                ) : f.type === 'checkbox' ? (
                  <input type="checkbox" checked={Boolean(form.values[f.name])} onChange={(e) => set(f.name, e.target.checked)} />
                ) : (
                  <input type={f.type || 'text'} step={f.step} min={f.min} value={form.values[f.name] ?? ''}
                    required={f.required} disabled={Boolean(form.id) && lockOnEdit.includes(f.name)}
                    onChange={(e) => set(f.name, e.target.value)} />
                )}
              </label>
            ))}
            <div className="form-actions">
              <button type="submit" className="btn primary">Save</button>
              <button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button>
            </div>
          </form>
        </Card>
      )}

      <Card>{loading ? <Loading /> : <DataTable columns={cols} rows={rows} />}</Card>
    </>
  );
}
