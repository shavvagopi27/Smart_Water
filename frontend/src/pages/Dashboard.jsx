import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { downloadInvoicePdf } from '../services/api';
import useFetch from '../hooks/useFetch';
import { AlertList, Badge, Card, Loading, Notice, PageHeader, StatCard } from '../components/ui';
import { dateStr, kl, money, shortDate } from '../utils/format';

export default function Dashboard() {
  const { data, loading, error, reload } = useFetch('/dashboard/resident');
  if (loading) return <Loading />;
  if (error) return <><PageHeader title="Dashboard" /><Notice>{error}</Notice></>;

  const markRead = async (a) => { await api.put(`/alerts/${a.id}/read`); reload(); };
  const daily = data.dailySeries.map((p) => ({ ...p, label: shortDate(p.date), consumption: Number(p.consumption) }));
  const monthly = data.monthlyComparison.map((p) => ({ label: p.label, You: Number(p.household), 'Apartment average': Number(p.apartmentAverage) }));
  const inv = data.latestInvoice;

  return (
    <>
      <PageHeader title={`Welcome, ${data.userName}`} subtitle={`Flat ${data.flatNumber} — your water usage at a glance`} />
      <div className="stats">
        <StatCard label="Latest daily usage" value={kl(data.latestDayConsumption)} hint={data.latestReadingDate ? dateStr(data.latestReadingDate) : 'No readings yet'} />
        <StatCard label="This month so far" value={kl(data.monthToDateConsumption)} hint={`Apartment avg ${kl(data.apartmentAverageMonthToDate)}`} />
        <StatCard label="Estimated bill (water charge)" value={money(data.estimatedCurrentCharge)} hint="Based on tariff tiers" tone="accent" />
        <StatCard label="Unread alerts" value={data.unreadAlerts} tone={data.unreadAlerts > 0 ? 'warn' : ''} />
      </div>

      <div className="grid-2">
        <Card title="Daily consumption (last 30 days)">
          <div className="chart">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={daily}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" fontSize={11} />
                <YAxis fontSize={11} unit=" kL" />
                <Tooltip formatter={(v) => `${v} kL`} />
                <Line type="monotone" dataKey="consumption" stroke="#0b6fb8" strokeWidth={2} dot={false} name="Consumption" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card title="You vs apartment average (monthly)">
          <div className="chart">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthly}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" fontSize={11} />
                <YAxis fontSize={11} unit=" kL" />
                <Tooltip formatter={(v) => `${v} kL`} />
                <Legend />
                <Bar dataKey="You" fill="#0b6fb8" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Apartment average" fill="#9bd0f5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="grid-2">
        <Card title="Latest invoice" actions={<Link className="btn small" to="/invoices">All invoices</Link>}>
          {inv ? (
            <div className="invoice-mini">
              <div><strong>{inv.invoiceNumber}</strong> <Badge value={inv.paymentStatus} /></div>
              <div className="muted">{inv.cycleName} · {dateStr(inv.periodStart)} – {dateStr(inv.periodEnd)}</div>
              <div className="big">{money(inv.totalAmount)}</div>
              <div className="muted">Consumption {kl(inv.consumption)} · Water {money(inv.waterCharge)} · Shared {money(inv.sharedCost)}</div>
              <button className="btn" onClick={() => downloadInvoicePdf(inv.id, inv.invoiceNumber)}>Download PDF</button>
            </div>
          ) : <p className="muted">No invoice has been generated for your flat yet.</p>}
        </Card>
        <Card title="Recent alerts" actions={<Link className="btn small" to="/alerts">View all</Link>}>
          <AlertList alerts={data.recentAlerts} onRead={markRead} />
        </Card>
      </div>

      <Card title="Water-saving tips" actions={<Link className="btn small" to="/tips">More tips</Link>}>
        <div className="tips">
          {data.tips.map((t) => <div key={t.id} className="tip"><strong>{t.title}</strong><p>{t.content}</p></div>)}
        </div>
      </Card>
    </>
  );
}
