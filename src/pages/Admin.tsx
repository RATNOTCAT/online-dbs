import { useEffect, useState } from 'react';
import { Shield, Users, Wallet, ArrowLeftRight, Lock } from 'lucide-react';
import { motion } from 'framer-motion';
import { BarChart, Bar, CartesianGrid, PieChart, Pie, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminAPI } from '@/services/api';
import { useBanking } from '@/contexts/BankingContext';

const formatCurrency = (n: number) => 'Rs ' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });

const AdminPage = () => {
  const { user } = useBanking();
  const [summary, setSummary] = useState<any | null>(null);
  const [forbidden, setForbidden] = useState(false);
  const [loading, setLoading] = useState(false);
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  useEffect(() => {
    const loadSummary = async () => {
      if (user?.role !== 'admin') {
        setForbidden(false);
        setSummary(null);
        return;
      }

      setLoading(true);
      try {
        const res = await adminAPI.getSummary();
        setSummary(res.data.summary || null);
        setForbidden(false);
      } catch (error: any) {
        if (error?.response?.status === 403) {
          setForbidden(true);
        } else {
          setForbidden(false);
        }
      } finally {
        setLoading(false);
      }
    };
    loadSummary();
  }, [user?.role]);

  const reloadSummary = async () => {
    setLoading(true);
    try {
      const res = await adminAPI.getSummary();
      setSummary(res.data.summary || null);
      setForbidden(false);
    } catch (error: any) {
      if (error?.response?.status === 403) {
        setForbidden(true);
      } else {
        setForbidden(false);
      }
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const stats = [
    { label: 'Users', value: summary?.total_users ?? 0, icon: Users },
    { label: 'Accounts', value: summary?.total_accounts ?? 0, icon: Wallet },
    { label: 'Transactions', value: summary?.total_transactions ?? 0, icon: ArrowLeftRight },
    { label: 'Locked Users', value: summary?.locked_users ?? 0, icon: Lock },
  ];
  const methodBreakdown = summary?.transaction_method_breakdown || [];
  const monthlyVolume = summary?.monthly_transaction_volume || [];
  const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6'];

  if (user?.role !== 'admin' || forbidden) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="banking-card">
          <h1 className="text-2xl font-bold text-foreground">Admin Access Required</h1>
          <p className="text-muted-foreground mt-3">
            This dashboard is now restricted to admin accounts only. The configured admin account contact bank administration.
          </p>
          <div className="mt-4 text-sm text-muted-foreground space-y-1">
            <p>Signed in as: {user?.email || 'Unknown user'}</p>
            <p>Current role: {user?.role || 'unknown'}</p>
          </div>
          {user?.role === 'admin' && (
            <button
              type="button"
              onClick={() => reloadSummary().catch(() => undefined)}
              className="mt-4 banking-button-primary"
            >
              Retry Admin Check
            </button>
          )}
        </div>
      </div>
    );
  }

  if (loading && !summary) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="banking-card">
          <h1 className="text-2xl font-bold text-foreground">Loading Admin Dashboard</h1>
          <p className="text-muted-foreground mt-3">
            Verifying admin access and loading platform reports.
          </p>
        </div>
      </div>
    );
  }

  const unlockUser = async (userId: string) => {
    await adminAPI.unlockUser(userId);
    setActionMessage('User unlocked successfully');
    await reloadSummary();
  };

  const sendBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    await adminAPI.sendBroadcast({ title: broadcastTitle, message: broadcastMessage });
    setBroadcastTitle('');
    setBroadcastMessage('');
    setActionMessage('Broadcast sent to all users');
    await reloadSummary();
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Admin Dashboard</h1>
        <p className="text-muted-foreground mt-1">Monitor users, security events, savings activity, and platform reports.</p>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon }) => (
          <div key={label} className="banking-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">{label}</p>
                <p className="text-3xl font-bold text-foreground mt-2">{value}</p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                <Icon className="w-5 h-5 text-primary" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {actionMessage && (
        <div className="banking-card text-sm text-success">
          {actionMessage}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <div className="banking-card">
          <p className="text-sm text-muted-foreground">Notifications</p>
          <p className="text-2xl font-bold text-foreground mt-2">{summary?.total_notifications ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-2">Unread: {summary?.unread_notifications ?? 0}</p>
        </div>
        <div className="banking-card">
          <p className="text-sm text-muted-foreground">Goals</p>
          <p className="text-2xl font-bold text-foreground mt-2">{summary?.total_goals ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-2">Completed: {summary?.completed_goals ?? 0}</p>
        </div>
        <div className="banking-card">
          <p className="text-sm text-muted-foreground">Goal Savings</p>
          <p className="text-2xl font-bold text-foreground mt-2">{formatCurrency(summary?.goal_saved_total ?? 0)}</p>
          <p className="text-xs text-muted-foreground mt-2">Target: {formatCurrency(summary?.goal_target_total ?? 0)}</p>
        </div>
        <div className="banking-card">
          <p className="text-sm text-muted-foreground">Beneficiaries</p>
          <p className="text-2xl font-bold text-foreground mt-2">{summary?.total_beneficiaries ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-2">Saved transfer shortcuts</p>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <section className="banking-card">
          <h2 className="text-lg font-semibold text-foreground mb-4">Broadcast Notification</h2>
          <form onSubmit={sendBroadcast} className="space-y-4">
            <input
              value={broadcastTitle}
              onChange={(e) => setBroadcastTitle(e.target.value)}
              placeholder="Broadcast title"
              className="w-full rounded-lg border border-border bg-secondary px-4 py-3 text-sm text-foreground"
              required
            />
            <textarea
              value={broadcastMessage}
              onChange={(e) => setBroadcastMessage(e.target.value)}
              placeholder="Send an update to every user"
              className="w-full min-h-28 rounded-lg border border-border bg-secondary px-4 py-3 text-sm text-foreground"
              required
            />
            <button type="submit" className="banking-button-primary">Send Broadcast</button>
          </form>
        </section>

        <section className="banking-card">
          <div className="flex items-center gap-3 mb-4">
            <ArrowLeftRight className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-semibold text-foreground">Transaction Volume by Month</h2>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthlyVolume}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                <XAxis dataKey="month" stroke="#94a3b8" />
                <YAxis stroke="#94a3b8" />
                <Tooltip />
                <Bar dataKey="amount" fill="#3b82f6" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="banking-card">
          <div className="flex items-center gap-3 mb-4">
            <Shield className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-semibold text-foreground">Transfer Method Mix</h2>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={methodBreakdown} dataKey="amount" nameKey="method" innerRadius={60} outerRadius={100} paddingAngle={3}>
                  {methodBreakdown.map((_: any, index: number) => (
                    <Cell key={`cell-${index}`} fill={chartColors[index % chartColors.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-2 gap-2 mt-4">
            {methodBreakdown.map((item: any, index: number) => (
              <div key={item.method} className="flex items-center gap-2 text-xs text-muted-foreground">
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors[index % chartColors.length] }} />
                <span>{item.method}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <section className="banking-card">
          <div className="flex items-center gap-3 mb-4">
            <Shield className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-semibold text-foreground">Recent Audit Logs</h2>
          </div>
          <div className="space-y-3">
            {(summary?.recent_audits || []).length === 0 && <p className="text-sm text-muted-foreground">No audit logs yet.</p>}
            {(summary?.recent_audits || []).map((item: any) => (
              <div key={item.id} className="rounded-xl border border-border p-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-foreground">{item.action}</p>
                  <span className={`text-xs px-2 py-1 rounded-full ${item.status === 'success' ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'}`}>
                    {item.status}
                  </span>
                </div>
                <p className="text-sm text-muted-foreground mt-1">{item.details || 'No details'}</p>
                <p className="text-xs text-muted-foreground mt-2">{item.createdAt || item.created_at}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="banking-card">
          <h2 className="text-lg font-semibold text-foreground mb-4">Users</h2>
          <div className="space-y-3">
            {(summary?.users || []).length === 0 && <p className="text-sm text-muted-foreground">No users found.</p>}
            {(summary?.users || []).map((user: any) => (
              <div key={user.id} className="rounded-xl border border-border p-3">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{user.name}</p>
                    <p className="text-xs text-muted-foreground">@{user.username} • {user.email}</p>
                    <p className="text-xs text-muted-foreground mt-1">Role: {user.role}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">Failed logins: {user.failed_login_attempts}</p>
                    <p className="text-xs text-muted-foreground">Last login: {user.last_login_at || 'Never'}</p>
                    <button
                      type="button"
                      onClick={() => unlockUser(user.id)}
                      className="mt-2 text-xs text-primary hover:underline"
                    >
                      Unlock user
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="banking-card">
        <h2 className="text-lg font-semibold text-foreground mb-4">Recent Notifications</h2>
        <div className="space-y-3">
          {(summary?.recent_notifications || []).length === 0 && <p className="text-sm text-muted-foreground">No notifications yet.</p>}
          {(summary?.recent_notifications || []).map((item: any) => (
            <div key={item.id} className="rounded-xl border border-border p-3 flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-foreground">{item.title}</p>
                <p className="text-xs text-muted-foreground mt-1">{item.type}</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">{item.created_at}</p>
                <p className={`text-xs mt-1 ${item.is_read ? 'text-muted-foreground' : 'text-primary'}`}>{item.is_read ? 'Read' : 'Unread'}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default AdminPage;
