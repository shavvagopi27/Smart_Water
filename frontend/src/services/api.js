import axios from 'axios';

export const TOKEN_KEY = 'sw_token';
export const USER_KEY = 'sw_user';

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLogin = err.config?.url?.includes('/auth/login');
    if (err.response?.status === 401 && !isLogin) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

/** Turns an Axios error into a readable message (uses the backend ApiError body when present). */
export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.errors) return Object.entries(data.errors).map(([k, v]) => `${k}: ${v}`).join('; ');
  if (data?.message) return data.message;
  if (err?.code === 'ERR_NETWORK') return 'Cannot reach the server. Is the backend running?';
  return err?.message || 'Something went wrong';
}

/** Downloads the PDF of an invoice (needs the Authorization header, so a plain link would not work). */
export async function downloadInvoicePdf(id, invoiceNumber) {
  const res = await api.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = `${invoiceNumber || 'invoice-' + id}.pdf`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export default api;
