import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../services/api';

/** GET helper: returns { data, loading, error, reload }. Pass url=null to skip. */
export default function useFetch(url, params) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(Boolean(url));
  const [error, setError] = useState('');
  const key = JSON.stringify(params || {});

  const load = useCallback(async () => {
    if (!url) return;
    setLoading(true);
    setError('');
    try {
      const res = await api.get(url, { params: JSON.parse(key) });
      setData(res.data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [url, key]);

  useEffect(() => { load(); }, [load]);
  return { data, loading, error, reload: load, setData };
}
