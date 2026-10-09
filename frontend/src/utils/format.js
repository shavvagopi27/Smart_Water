export const money = (v) => (v === null || v === undefined ? '-' : `₹${Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`);
export const kl = (v, digits = 2) => (v === null || v === undefined ? '-' : `${Number(v).toLocaleString('en-IN', { maximumFractionDigits: digits })} kL`);
export const num = (v, digits = 2) => (v === null || v === undefined ? '-' : Number(v).toLocaleString('en-IN', { maximumFractionDigits: digits }));
export const dateStr = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-');
export const shortDate = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }) : '');
export const todayISO = () => new Date().toISOString().slice(0, 10);
export const daysAgoISO = (n) => new Date(Date.now() - n * 86400000).toISOString().slice(0, 10);
