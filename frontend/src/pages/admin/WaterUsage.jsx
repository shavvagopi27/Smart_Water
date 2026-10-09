import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../services/api';
import { Card, DataTable, Notice, PageHeader } from '../../components/ui';
import { dateStr, daysAgoISO, num, todayISO } from '../../utils/format';

export default function WaterUsage() {
  const [households, setHouseholds] = useState([]);
  const [householdId, setHouseholdId] = useState('');
  const [date, setDate] = useState(todayISO());
  const [reading, setReading] = useState('');
  const [history, setHistory] = useState([]);
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const household = households.find((h) => String(h.id) === String(householdId));

  useEffect(() => { api.get('/households').then((r) => setHouseholds(r.data)).catch((e) => setError(errorMessage(e))); }, []);

  const loadHistory = async (id) => {
    if (!id) { setHistory([]); return; }
    try { setHistory((await api.get(`/water-usage/${id}`, { params: { from: daysAgoISO(90), to: todayISO() } })).data.reverse()); } catch (e) { setError(errorMessage(e)); }
  };
  useEffect(() => { loadHistory(householdId); }, [householdId]);

  const submitReading = async (e) => {
    e.preventDefault();
    setError(''); setInfo('');
    try {
      const { data } = await api.post('/water-usage', { householdId: Number(householdId), meterId: household.meterId, readingDate: date, meterReading: Number(reading) });
      setInfo(`Reading saved. Consumption since previous reading: ${num(data.consumption, 3)} kL`);
      setReading('');
      loadHistory(householdId);
    } catch (err) { setError(errorMessage(err)); }
  };

  const upload = async (e) => {
    e.preventDefault();
    setError(''); setInfo(''); setResult(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const { data } = await api.post('/water-usage/upload', fd);
      setResult(data);
      loadHistory(householdId);
    } catch (err) { setError(errorMessage(err)); }
  };

  return (
    <>
      <PageHeader title="Water usage" subtitle="Enter manual daily readings or upload a CSV of meter readings" />
      <Notice>{error}</Notice>
      <Notice type="success">{info}</Notice>
      <div className="grid-2">
        <Card title="Manual daily reading">
          <form className="form-grid" onSubmit={submitReading}>
            <label><span>Household *</span>
              <select required value={householdId} onChange={(e) => setHouseholdId(e.target.value)}>
                <option value="">Select…</option>
                {households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber} {h.meterNumber ? `(${h.meterNumber})` : '(no meter)'}</option>)}
              </select></label>
            <label><span>Reading date *</span><input type="date" required max={todayISO()} value={date} onChange={(e) => setDate(e.target.value)} /></label>
            <label><span>Meter reading (kL) *</span><input type="number" step="0.001" min="0" required value={reading} onChange={(e) => setReading(e.target.value)} /></label>
            <div className="form-actions"><button className="btn primary" disabled={!household?.meterId}>Save reading</button></div>
            {household && !household.meterId && <Notice>This household has no meter. Add one under Meters first.</Notice>}
          </form>
        </Card>
        <Card title="Bulk CSV upload">
          <form className="form-grid" onSubmit={upload}>
            <p className="muted wide">Columns: <code>meterId,householdId,readingDate,meterReading</code>. <code>meterId</code> may be the meter ID (e.g. WM-1001) or its numeric database id. Dates use <code>yyyy-MM-dd</code>.</p>
            <label className="wide"><span>CSV file *</span><input type="file" accept=".csv,text/csv" required onChange={(e) => setFile(e.target.files[0])} /></label>
            <div className="form-actions"><button className="btn primary">Upload</button></div>
          </form>
          {result && (
            <div className="upload-result">
              <div className="stats small">
                <div className="stat"><span className="stat-label">Total</span><strong className="stat-value">{result.totalRecords}</strong></div>
                <div className="stat"><span className="stat-label">Saved</span><strong className="stat-value">{result.successfulRecords}</strong></div>
                <div className="stat"><span className="stat-label">Failed</span><strong className="stat-value">{result.failedRecords}</strong></div>
                <div className="stat"><span className="stat-label">Duplicates</span><strong className="stat-value">{result.duplicateRecords}</strong></div>
              </div>
              {result.errors.length > 0 && (
                <DataTable rowKey="row" rows={result.errors} columns={[{ key: 'row', label: 'CSV row' }, { key: 'message', label: 'Problem' }]} />
              )}
            </div>
          )}
        </Card>
      </div>
      <Card title={household ? `Recent readings — flat ${household.flatNumber}` : 'Recent readings (select a household)'}>
        <DataTable rows={history} columns={[
          { key: 'readingDate', label: 'Date', render: (r) => dateStr(r.readingDate) },
          { key: 'meterNumber', label: 'Meter' },
          { key: 'meterReading', label: 'Reading (kL)', align: 'right', render: (r) => num(r.meterReading, 3) },
          { key: 'consumption', label: 'Consumption (kL)', align: 'right', render: (r) => num(r.consumption, 3) },
          { key: 'source', label: 'Source' },
        ]} />
      </Card>
    </>
  );
}
