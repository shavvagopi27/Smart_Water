import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import useFetch from '../../hooks/useFetch';
import { AlertList, Badge, Card, DataTable, Loading, Notice, PageHeader, StatCard } from '../../components/ui';
import { dateStr, kl, money } from '../../utils/format';

export default function AdminDashboard() {
  const { data, loading, error } = useFetch('/dashboard/admin');
  if (loading) return <Loading />;
  if (error) return <><PageHeader title="Admin dashboard" /><Notice>{error}</Notice></>;
  const chart = data.usageComparison.map((p) => ({ label: p.label, Household: Number(p.household), 'Apartment average': Number(p.apartmentAverage) }));
  const c = data.currentCycle;

  return (
    <>
      <PageHeader title="Admin dashboard" subtitle="Apartment water overview for the current month" />
      <div className="stats">
        <StatCard label="Total households" value={data.totalHouseholds} />
        <StatCard label="Consumption this month" value={kl(data.monthConsumptionKl)} />
        <StatCard label="Water purchased this month" value={kl(data.monthPurchasedKl)} />
        <StatCard label="Total billed (all invoices)" value={money(data.totalBilledAmount)} tone="accent" />
      </div>
      <div className="grid-2">
        <Card title="Usage comparison by flat (this month)">
          <div className="chart"><ResponsiveContainer width="100%" height="100%">
            <BarChart data={chart}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" fontSize={11} /><YAxis fontSize={11} unit=" kL" />
              <Tooltip formatter={(v) => `${v} kL`} /><Legend />
              <Bar dataKey="Household" fill="#0b6fb8" radius={[4, 4, 0, 0]} /><Bar dataKey="Apartment average" fill="#9bd0f5" radius={[4, 4, 0, 0]} /></BarChart>
          </ResponsiveContainer></div>
        </Card>
        <Card title="Current billing cycle" actions={<Link className="btn small" to="/admin/billing-cycles">Manage</Link>}>
          {!c ? <p className="muted">No billing cycle yet.</p> : (
            <div className="kv">
              <div><span>Cycle</span><strong>{c.name}</strong></div>
              <div><span>Period</span><strong>{dateStr(c.startDate)} – {dateStr(c.endDate)}</strong></div>
              <div><span>Status</span><Badge value={c.status} /></div>
              <div><span>Water purchase cost</span><strong>{money(c.totalPurchaseCost)}</strong></div>
              <div><span>Invoices / total</span><strong>{c.invoiceCount} / {money(c.totalInvoiced)}</strong></div>
            </div>)}
        </Card>
      </div>
      <div className="grid-2">
        <Card title="High-consumption households">
          <DataTable rowKey="householdId" rows={data.highConsumptionHouseholds} columns={[
            { key: 'flatNumber', label: 'Flat' }, { key: 'consumption', label: 'This month', align: 'right', render: (r) => kl(r.consumption) }]} />
        </Card>
        <Card title="Potential anomalies" actions={<Link className="btn small" to="/admin/alerts">All alerts</Link>}>
          <AlertList alerts={data.anomalies} showFlat />
        </Card>
      </div>
    </>
  );
}
