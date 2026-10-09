import { Link } from 'react-router-dom';
import { downloadInvoicePdf } from '../services/api';
import useFetch from '../hooks/useFetch';
import { Badge, Card, DataTable, Loading, Notice, PageHeader, StatCard } from '../components/ui';
import { dateStr, kl, money, num } from '../utils/format';

export default function Billing() {
  const { data, loading, error } = useFetch('/dashboard/resident');
  if (loading) return <Loading />;
  if (error) return <><PageHeader title="Current Bill" /><Notice>{error}</Notice></>;
  const inv = data.latestInvoice;

  return (
    <>
      <PageHeader title="Current Bill" subtitle="Your latest invoice and the running estimate for this month" />
      <div className="stats">
        <StatCard label="Month-to-date usage" value={kl(data.monthToDateConsumption)} />
        <StatCard label="Estimated water charge (so far)" value={money(data.estimatedCurrentCharge)} hint="Tariff tiers only; shared cost is added on the invoice" tone="accent" />
      </div>
      <Card title={inv ? `Invoice ${inv.invoiceNumber}` : 'Latest invoice'}
        actions={inv && <button className="btn" onClick={() => downloadInvoicePdf(inv.id, inv.invoiceNumber)}>Download PDF</button>}>
        {!inv ? <p className="muted">No invoice has been generated yet. <Link to="/dashboard">Back to dashboard</Link></p> : (
          <>
            <div className="kv">
              <div><span>Billing period</span><strong>{dateStr(inv.periodStart)} – {dateStr(inv.periodEnd)}</strong></div>
              <div><span>Previous reading</span><strong>{num(inv.previousReading, 3)} kL</strong></div>
              <div><span>Current reading</span><strong>{num(inv.currentReading, 3)} kL</strong></div>
              <div><span>Consumption</span><strong>{kl(inv.consumption, 3)}</strong></div>
              <div><span>Status</span><Badge value={inv.paymentStatus} /></div>
            </div>
            <DataTable rows={inv.items.map((i, k) => ({ ...i, id: k }))} columns={[
              { key: 'description', label: 'Description' },
              { key: 'quantity', label: 'Qty (kL)', align: 'right', render: (r) => (r.quantity == null ? '' : num(r.quantity, 3)) },
              { key: 'rate', label: 'Rate (₹/kL)', align: 'right', render: (r) => (r.rate == null ? '' : num(r.rate)) },
              { key: 'amount', label: 'Amount', align: 'right', render: (r) => money(r.amount) },
            ]} />
            <div className="totals">
              <div><span>Water charge</span><strong>{money(inv.waterCharge)}</strong></div>
              <div><span>Shared water cost</span><strong>{money(inv.sharedCost)}</strong></div>
              <div><span>Adjustments</span><strong>{money(inv.adjustment)}</strong></div>
              <div className="grand"><span>Total due</span><strong>{money(inv.totalAmount)}</strong></div>
            </div>
            <p className="muted"><strong>Payment instructions:</strong> {inv.paymentInstructions}</p>
          </>
        )}
      </Card>
    </>
  );
}
