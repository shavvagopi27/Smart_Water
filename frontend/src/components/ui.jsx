import { dateStr } from '../utils/format';

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      <div className="actions">{actions}</div>
    </div>
  );
}

export function Card({ title, children, actions, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-head">
          <h3>{title}</h3>
          <div className="actions">{actions}</div>
        </div>
      )}
      {children}
    </section>
  );
}

export function StatCard({ label, value, hint, tone }) {
  return (
    <div className={`stat ${tone || ''}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

export function Notice({ type = 'error', children }) {
  if (!children) return null;
  return <div className={`notice ${type}`}>{children}</div>;
}

export function Loading({ text = 'Loading…' }) {
  return <div className="loading">{text}</div>;
}

export function Empty({ children = 'Nothing to show yet.' }) {
  return <div className="empty">{children}</div>;
}

export function Badge({ value }) {
  const tone = {
    OPEN: 'blue', FINALIZED: 'green', ARCHIVED: 'grey', PENDING: 'amber', PAID: 'green',
    ACTIVE: 'green', INACTIVE: 'grey', FAULTY: 'red', INFO: 'blue', WARNING: 'amber', CRITICAL: 'red',
    ADMIN: 'purple', RESIDENT: 'blue', METERED: 'green', AREA_FALLBACK: 'amber', NONE: 'grey',
  }[value] || 'grey';
  return <span className={`badge ${tone}`}>{String(value).replaceAll('_', ' ')}</span>;
}

/** Simple responsive table. columns: [{ key, label, render?(row), align? }] */
export function DataTable({ columns, rows, empty, rowKey = 'id' }) {
  if (!rows || rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c.key} style={{ textAlign: c.align }}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r[rowKey] ?? i}>
              {columns.map((c) => (
                <td key={c.key} style={{ textAlign: c.align }}>{c.render ? c.render(r) : r[c.key] ?? '-'}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function AlertList({ alerts, onRead, showFlat }) {
  if (!alerts || alerts.length === 0) return <Empty>No alerts. Everything looks normal.</Empty>;
  return (
    <ul className="alert-list">
      {alerts.map((a) => (
        <li key={a.id} className={a.read ? 'read' : ''}>
          <div className="alert-top">
            <Badge value={a.severity} />
            <strong>{a.alertType.replaceAll('_', ' ')}</strong>
            {showFlat && <span className="muted">Flat {a.flatNumber}</span>}
            <span className="muted">{dateStr(a.usageDate)}</span>
          </div>
          <p>{a.message}</p>
          {onRead && !a.read && <button className="btn small" onClick={() => onRead(a)}>Mark as read</button>}
        </li>
      ))}
    </ul>
  );
}
