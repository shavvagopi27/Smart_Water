import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { TOKEN_KEY, USER_KEY } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
  });
  const [ready, setReady] = useState(false);

  const store = (data) => {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  };

  const refresh = useCallback(async () => {
    const { data } = await api.get('/auth/me');
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUser(data);
    return data;
  }, []);

  // validate the stored token once on start-up
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) { setReady(true); return; }
    refresh().catch(() => {}).finally(() => setReady(true));
  }, [refresh]);

  const value = useMemo(() => ({
    user,
    ready,
    isAdmin: user?.role === 'ADMIN',
    login: async (email, password) => store((await api.post('/auth/login', { email, password })).data),
    register: async (payload) => store((await api.post('/auth/register', payload)).data),
    logout: () => {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      setUser(null);
    },
    refresh,
  }), [user, ready, refresh]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
