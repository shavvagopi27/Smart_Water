import { useState } from 'react';
import api, { errorMessage } from '../services/api';
import useFetch from '../hooks/useFetch';
import { useAuth } from '../hooks/useAuth';
import { AlertList, Card, Loading, Notice, PageHeader } from '../components/ui';

/** Shared by residents and admins. Admins can also trigger detection manually. */
export default function Alerts() {
  const { isAdmin } = useAuth();
  const { data, loading, error, reload } = useFetch('/alerts');
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const markRead = async (a) => { try { await api.put(`/alerts/${a.id}/read`); reload(); } catch (e) { setErr(errorMessage(e)); } };
  const run = async () => {
    setErr(''); setMsg('');
    try { const { data: r } = await api.post('/alerts/run-detection'); setMsg(`Detection finished: ${r.newAlerts} new alert(s).`); reload(); } catch (e) { setErr(errorMessage(e)); }
  };

  return (
    <>
      <PageHeader title="Alerts" subtitle="Unusual usage is flagged as potential only — please verify before concluding there is a leak."
        actions={isAdmin && <button className="btn primary" onClick={run}>Run detection now</button>} />
      <Notice>{error || err}</Notice>
      <Notice type="success">{msg}</Notice>
      <Card>{loading ? <Loading /> : <AlertList alerts={data} onRead={markRead} showFlat={isAdmin} />}</Card>
    </>
  );
}
