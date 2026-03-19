import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, ArrowUpRight, ArrowDownLeft, Wallet, CreditCard, Send, History } from 'lucide-react';
import { Link } from 'react-router-dom';
import { accountAPI, transactionAPI, cardAPI, accountSelection } from '@/services/api';
import { BarChart, Bar, CartesianGrid, PieChart, Pie, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

const formatCurrency = (n: number) => 'Rs ' + n.toLocaleString('en-IN', { minimumFractionDigits: 2 });

const Dashboard = () => {
  const [balance, setBalance] = useState(0);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [accountNumber, setAccountNumber] = useState<string | null>(null);
  const [transactions, setTransactions] = useState<any[]>([]);
  const [creditCard, setCreditCard] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const loadAccountData = async (selectedAccount?: string | null) => {
    const [balanceRes, txRes] = await Promise.all([
      accountAPI.getBalance(selectedAccount || undefined),
      transactionAPI.getTransactions(selectedAccount || undefined),
    ]);

    if (balanceRes.data.success) {
      setBalance(balanceRes.data.balance);
      setAccountNumber(balanceRes.data.account_number || null);
    }
    if (txRes.data.success) {
      setTransactions(txRes.data.transactions);
    }
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        const [accountsRes, cardRes] = await Promise.all([
          accountAPI.getAccounts(),
          cardAPI.getCreditCard(),
        ]);

        const availableAccounts = accountsRes.data?.accounts || [];
        setAccounts(availableAccounts);

        const selectedAccount = accountSelection.get() || availableAccounts[0]?.account_number || null;
        if (selectedAccount) {
          accountSelection.set(selectedAccount);
        }

        await loadAccountData(selectedAccount);

        if (cardRes.data.success) {
          setCreditCard(cardRes.data.card);
        }
      } catch (err) {
        console.error('Error loading dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const totalIncome = transactions.filter((t) => t.entry_type === 'credit').reduce((s, t) => s + t.amount, 0);
  const totalExpense = transactions.filter((t) => t.entry_type !== 'credit').reduce((s, t) => s + t.amount, 0);
  const recentTx = transactions.slice(0, 5);
  const flowByMethod = Object.values(
    transactions.reduce((acc: Record<string, { method: string; amount: number }>, tx) => {
      const key = tx.method || tx.type || 'other';
      acc[key] = acc[key] || { method: key.toUpperCase(), amount: 0 };
      acc[key].amount += tx.amount;
      return acc;
    }, {})
  );
  const monthlyFlow = Object.values(
    transactions.reduce((acc: Record<string, { month: string; credit: number; debit: number }>, tx) => {
      const month = new Date(tx.created_at).toLocaleString('en-IN', { month: 'short' });
      acc[month] = acc[month] || { month, credit: 0, debit: 0 };
      if (tx.entry_type === 'credit') acc[month].credit += tx.amount;
      else acc[month].debit += tx.amount;
      return acc;
    }, {})
  );
  const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6'];

  const quickActions = [
    { label: 'Send Money', icon: Send, to: '/payments', color: 'text-primary' },
    { label: 'Transactions', icon: History, to: '/transactions', color: 'text-success' },
    { label: 'Credit Card', icon: CreditCard, to: '/credit-card', color: 'text-warning' },
  ];

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Welcome back</h1>
        <p className="text-muted-foreground mt-1">Here is your financial overview</p>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="stat-card">
          <div className="flex items-center justify-between mb-3">
            <span className="text-muted-foreground text-sm">Selected Account Balance</span>
            <Wallet className="w-5 h-5 text-primary" />
          </div>
          <p className="text-2xl font-bold text-foreground font-mono">{formatCurrency(balance)}</p>
          <p className="text-xs text-success mt-2 flex items-center gap-1"><TrendingUp className="w-3 h-3" /> Active account ledger</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }} className="stat-card">
          <div className="flex items-center justify-between mb-3">
            <span className="text-muted-foreground text-sm">Credits</span>
            <ArrowDownLeft className="w-5 h-5 text-success" />
          </div>
          <p className="text-2xl font-bold text-success font-mono">{formatCurrency(totalIncome)}</p>
          <p className="text-xs text-muted-foreground mt-2">{transactions.filter((t) => t.entry_type === 'credit').length} credit entries</p>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="stat-card">
          <div className="flex items-center justify-between mb-3">
            <span className="text-muted-foreground text-sm">Debits</span>
            <ArrowUpRight className="w-5 h-5 text-destructive" />
          </div>
          <p className="text-2xl font-bold text-destructive font-mono">{formatCurrency(totalExpense)}</p>
          <p className="text-xs text-muted-foreground mt-2">{transactions.filter((t) => t.entry_type !== 'credit').length} debit entries</p>
        </motion.div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-1 space-y-3">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Quick Actions</h2>
          {quickActions.map(({ label, icon: Icon, to, color }) => (
            <Link key={to} to={to} className="stat-card flex items-center gap-4 cursor-pointer">
              <div className="w-10 h-10 rounded-lg bg-secondary flex items-center justify-center">
                <Icon className={`w-5 h-5 ${color}`} />
              </div>
              <span className="text-sm font-medium text-foreground">{label}</span>
            </Link>
          ))}
        </div>

        <div className="lg:col-span-2">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Credit Card</h2>
            <div className="flex items-center gap-3">
              {accounts.length > 0 && (
                <select
                  value={accountNumber || ''}
                  onChange={async (e) => {
                    const nextAccount = e.target.value;
                    accountSelection.set(nextAccount);
                    await loadAccountData(nextAccount);
                  }}
                  className="bg-secondary border border-border rounded-lg px-3 py-2 text-xs text-foreground"
                >
                  {accounts.map((account) => (
                    <option key={account.id} value={account.account_number}>
                      {account.account_type} - {account.account_number}
                    </option>
                  ))}
                </select>
              )}
              {accountNumber && (
                <div className="text-xs text-muted-foreground">
                  Account: <span className="font-mono text-foreground">{accountNumber}</span>
                </div>
              )}
            </div>
          </div>
          {creditCard && (
            <div className="credit-card-visual h-48 flex flex-col justify-between">
              <div className="flex justify-between items-start relative z-10">
                <p className="text-xs text-foreground/60 uppercase tracking-widest">Vibe Bank</p>
                <CreditCard className="w-8 h-8 text-foreground/40" />
              </div>
              <div className="relative z-10">
                <p className="text-lg font-mono text-foreground tracking-[0.2em] mb-3">{creditCard.card_number}</p>
                <div className="flex justify-between">
                  <div>
                    <p className="text-[10px] text-foreground/50 uppercase">Cardholder</p>
                    <p className="text-sm text-foreground font-medium">{creditCard.holder_name}</p>
                  </div>
                  <div>
                    <p className="text-[10px] text-foreground/50 uppercase">Expires</p>
                    <p className="text-sm text-foreground font-medium">{creditCard.expiry}</p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Recent Transactions</h2>
          <Link to="/transactions" className="text-xs text-primary hover:underline">View All</Link>
        </div>
        <div className="banking-card p-0 divide-y divide-border">
          {loading ? (
            <div className="px-6 py-4 text-center text-muted-foreground">Loading transactions...</div>
          ) : recentTx.length > 0 ? (
            recentTx.map((tx) => {
              const isOutgoing = tx.entry_type !== 'credit';
              const counterparty = isOutgoing
                ? tx.receiver_name || tx.receiver_account_no || 'External account'
                : tx.sender_name || tx.sender_account_no || 'External source';
              return (
                <div key={tx.id} className="flex items-center justify-between px-6 py-4">
                  <div className="flex items-center gap-4">
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${isOutgoing ? 'bg-destructive/10' : 'bg-success/10'}`}>
                      {isOutgoing ? <ArrowUpRight className="w-4 h-4 text-destructive" /> : <ArrowDownLeft className="w-4 h-4 text-success" />}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">{tx.description}</p>
                      <p className="text-xs text-muted-foreground">{isOutgoing ? `To: ${counterparty}` : `From: ${counterparty}`}</p>
                      <p className="text-xs text-muted-foreground">{new Date(tx.created_at).toLocaleDateString()} · {tx.type.toUpperCase()}</p>
                    </div>
                  </div>
                  <span className={`text-sm font-semibold font-mono ${isOutgoing ? 'text-destructive' : 'text-success'}`}>
                    {isOutgoing ? '-' : '+'}{formatCurrency(tx.amount)}
                  </span>
                </div>
              );
            })
          ) : (
            <div className="px-6 py-4 text-center text-muted-foreground">No transactions yet</div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <div className="banking-card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Monthly Flow</h2>
              <p className="text-xs text-muted-foreground mt-1">Credits vs debits by month</p>
            </div>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthlyFlow}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                <XAxis dataKey="month" stroke="#94a3b8" />
                <YAxis stroke="#94a3b8" />
                <Tooltip />
                <Bar dataKey="credit" fill="#10b981" radius={[6, 6, 0, 0]} />
                <Bar dataKey="debit" fill="#ef4444" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="banking-card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Method Usage</h2>
              <p className="text-xs text-muted-foreground mt-1">Where your transfers are going</p>
            </div>
          </div>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={flowByMethod} dataKey="amount" nameKey="method" innerRadius={60} outerRadius={100} paddingAngle={3}>
                  {flowByMethod.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={chartColors[index % chartColors.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-2 gap-2 mt-4">
            {flowByMethod.map((item, index) => (
              <div key={item.method} className="flex items-center gap-2 text-xs text-muted-foreground">
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: chartColors[index % chartColors.length] }} />
                <span>{item.method}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
