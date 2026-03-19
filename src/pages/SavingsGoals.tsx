import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Target, PiggyBank, CheckCircle2, Wallet } from 'lucide-react';
import { accountAPI, accountSelection, goalAPI } from '@/services/api';

const formatCurrency = (n: number) => 'Rs ' + n.toLocaleString('en-IN', { minimumFractionDigits: 2 });

const SavingsGoalsPage = () => {
  const [goals, setGoals] = useState<any[]>([]);
  const [summary, setSummary] = useState<any>(null);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<string>('');
  const [createForm, setCreateForm] = useState({
    title: '',
    description: '',
    category: 'Emergency',
    target_amount: '',
    target_date: '',
  });
  const [contribution, setContribution] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const loadGoals = async () => {
    const res = await goalAPI.getGoals();
    setGoals(res.data.goals || []);
    setSummary(res.data.summary || null);
  };

  useEffect(() => {
    const load = async () => {
      const accountsRes = await accountAPI.getAccounts();
      const availableAccounts = accountsRes.data?.accounts || [];
      setAccounts(availableAccounts);
      const defaultAccount = accountSelection.get() || availableAccounts[0]?.account_number || '';
      if (defaultAccount) {
        accountSelection.set(defaultAccount);
        setSelectedAccount(defaultAccount);
      }
      await loadGoals();
    };
    load();
  }, []);

  const stats = useMemo(() => [
    { label: 'Goals', value: summary?.total_goals ?? 0, icon: Target },
    { label: 'Completed', value: summary?.completed_goals ?? 0, icon: CheckCircle2 },
    { label: 'Saved', value: formatCurrency(summary?.saved_amount ?? 0), icon: PiggyBank },
    { label: 'Target', value: formatCurrency(summary?.target_amount ?? 0), icon: Wallet },
  ], [summary]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await goalAPI.createGoal({
        ...createForm,
        target_amount: Number(createForm.target_amount),
      });
      setCreateForm({ title: '', description: '', category: 'Emergency', target_amount: '', target_date: '' });
      await loadGoals();
    } finally {
      setSubmitting(false);
    }
  };

  const handleContribute = async (goalId: string) => {
    const amount = Number(contribution[goalId]);
    if (!amount) {
      return;
    }
    setSubmitting(true);
    try {
      await goalAPI.contributeToGoal(goalId, {
        amount,
        account_number: selectedAccount || undefined,
      });
      setContribution((prev) => ({ ...prev, [goalId]: '' }));
      await loadGoals();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Savings Goals</h1>
        <p className="text-muted-foreground mt-1">Create goal-based targets and move funds from your selected account into them.</p>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        {stats.map(({ label, value, icon: Icon }) => (
          <div key={label} className="banking-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">{label}</p>
                <p className="text-2xl font-bold text-foreground mt-2">{value}</p>
              </div>
              <div className="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center">
                <Icon className="w-5 h-5 text-primary" />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[360px,1fr] gap-6">
        <section className="banking-card">
          <h2 className="text-lg font-semibold text-foreground mb-4">Create Goal</h2>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="text-sm text-muted-foreground">Title</label>
              <input
                className="mt-2 w-full bg-secondary border border-border rounded-lg px-3 py-2 text-foreground"
                value={createForm.title}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, title: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Category</label>
              <select
                className="mt-2 w-full bg-secondary border border-border rounded-lg px-3 py-2 text-foreground"
                value={createForm.category}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, category: e.target.value }))}
              >
                <option>Emergency</option>
                <option>Travel</option>
                <option>Education</option>
                <option>Vehicle</option>
                <option>Gadgets</option>
                <option>General</option>
              </select>
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Target Amount</label>
              <input
                type="number"
                min="1"
                step="0.01"
                className="mt-2 w-full bg-secondary border border-border rounded-lg px-3 py-2 text-foreground"
                value={createForm.target_amount}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, target_amount: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Target Date</label>
              <input
                type="date"
                className="mt-2 w-full bg-secondary border border-border rounded-lg px-3 py-2 text-foreground"
                value={createForm.target_date}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, target_date: e.target.value }))}
              />
            </div>
            <div>
              <label className="text-sm text-muted-foreground">Description</label>
              <textarea
                className="mt-2 w-full bg-secondary border border-border rounded-lg px-3 py-2 text-foreground min-h-24"
                value={createForm.description}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, description: e.target.value }))}
              />
            </div>
            <button type="submit" disabled={submitting} className="w-full banking-button-primary disabled:opacity-60">
              {submitting ? 'Saving...' : 'Create Goal'}
            </button>
          </form>
        </section>

        <section className="space-y-4">
          <div className="banking-card flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-foreground">Goal Tracker</h2>
              <p className="text-sm text-muted-foreground mt-1">Contributions are deducted from the selected account and logged in transactions.</p>
            </div>
            <select
              className="bg-secondary border border-border rounded-lg px-3 py-2 text-sm text-foreground"
              value={selectedAccount}
              onChange={(e) => {
                accountSelection.set(e.target.value);
                setSelectedAccount(e.target.value);
              }}
            >
              {accounts.map((account) => (
                <option key={account.id} value={account.account_number}>
                  {account.account_type} - {account.account_number}
                </option>
              ))}
            </select>
          </div>

          {goals.length === 0 && (
            <div className="banking-card text-sm text-muted-foreground">No goals yet. Create your first goal to start tracking savings progress.</div>
          )}

          {goals.map((goal) => (
            <div key={goal.id} className="banking-card">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-lg font-semibold text-foreground">{goal.title}</h3>
                    <span className={`text-xs px-2 py-1 rounded-full ${goal.status === 'completed' ? 'bg-success/10 text-success' : 'bg-primary/10 text-primary'}`}>
                      {goal.status}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{goal.description || 'No description added'}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">Category</p>
                  <p className="text-sm text-foreground">{goal.category}</p>
                </div>
              </div>

              <div className="mt-4">
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-muted-foreground">{formatCurrency(goal.saved_amount)} saved</span>
                  <span className="text-muted-foreground">{formatCurrency(goal.target_amount)} target</span>
                </div>
                <div className="h-3 bg-secondary rounded-full overflow-hidden">
                  <div className="h-full bg-primary rounded-full transition-all duration-300" style={{ width: `${Math.min(goal.progress_percent, 100)}%` }} />
                </div>
                <div className="flex justify-between text-xs text-muted-foreground mt-2">
                  <span>{goal.progress_percent.toFixed(1)}% complete</span>
                  <span>{goal.target_date ? `Target by ${goal.target_date}` : 'No target date'}</span>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-3">
                <input
                  type="number"
                  min="1"
                  step="0.01"
                  placeholder="Contribution amount"
                  className="bg-secondary border border-border rounded-lg px-3 py-2 text-foreground flex-1 min-w-44"
                  value={contribution[goal.id] || ''}
                  onChange={(e) => setContribution((prev) => ({ ...prev, [goal.id]: e.target.value }))}
                  disabled={goal.status === 'completed'}
                />
                <button
                  type="button"
                  onClick={() => handleContribute(goal.id)}
                  disabled={submitting || goal.status === 'completed'}
                  className="banking-button-primary disabled:opacity-60"
                >
                  {goal.status === 'completed' ? 'Completed' : 'Contribute'}
                </button>
              </div>
            </div>
          ))}
        </section>
      </div>
    </div>
  );
};

export default SavingsGoalsPage;
