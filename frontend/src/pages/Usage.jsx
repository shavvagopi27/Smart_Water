import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import useFetch from '../hooks/useFetch';
import { useAuth } from '../hooks/useAuth';
import { Card, DataTable, Loading, Notice, PageHeader } from '../components/ui';
import { dateStr, daysAgoISO, num, shortDate, todayISO } from '../utils/format';

export default function Usage() {
  const { user } = useAuth();
  const [from, setFrom] = useState(daysAgoISO(29));
  const [to, setTo] = useState(todayISO());
  const daily = useFetch('/water-usage/daily', { from, to });
  const monthly = useFetch('/water-usage/monthly');
  const history = useFetch(user.householdId ? `/water-usage/${user.householdId}` : null, { from, to });

  const dailyData = (daily.data || []).map((p) => ({ label: shortDate(p.date), consumption: Number(p.consumption) }));
  const monthlyData = (monthly.data || []).map((p) => ({ label: p.month, consumption: Number(p.consumption) }));

  return (
    <>
      <PageHeader title="My Water Usage" subtitle="Daily and monthly consumption from your meter readings"
        actions={
          <div className="filters">
            <label><span>From</span><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
            <label><span>To</span><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
          </div>} />
      <Notice>{daily.error || monthly.error}</Notice>
      <div className="grid-2">
        <Card title="Daily consumption">
          {daily.loading ? <Loading /> : (
            <div className="chart"><ResponsiveContainer width="100%" height="100%">
              <LineChart data={dailyData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} unit=" kL" />
                <Tooltip formatter={(v) => `${v} kL`} /><Line type="monotone" dataKey="consumption" stroke="#0b6fb8" strokeWidth={2} dot={false} /></LineChart>
            </ResponsiveContainer></div>)}
        </Card>
        <Card title="Monthly consumption (last 12 months)">
          {monthly.loading ? <Loading /> : (
            <div className="chart"><ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthlyData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} unit=" kL" />
                <Tooltip formatter={(v) => `${v} kL`} /><Bar dataKey="consumption" fill="#0b6fb8" radius={[4, 4, 0, 0]} /></BarChart>
            </ResponsiveContainer></div>)}
        </Card>
      </div>
      <Card title="Historical consumption">
        <div className="chart"><ResponsiveContainer width="100%" height="100%">
          <LineChart data={monthlyData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} unit=" kL" />
            <Tooltip formatter={(v) => `${v} kL`} /><Line type="monotone" dataKey="consumption" stroke="#12a37f" strokeWidth={2} /></LineChart>
        </ResponsiveContainer></div>
      </Card>
      <Card title="Meter readings">
        <DataTable rows={[...(history.data || [])].reverse()} columns={[
          { key: 'readingDate', label: 'Date', render: (r) => dateStr(r.readingDate) },
          { key: 'meterNumber', label: 'Meter' },
          { key: 'meterReading', label: 'Reading (kL)', align: 'right', render: (r) => num(r.meterReading, 3) },
          { key: 'consumption', label: 'Consumption (kL)', align: 'right', render: (r) => num(r.consumption, 3) },
        ]} />
      </Card>
    </>
  );
}
