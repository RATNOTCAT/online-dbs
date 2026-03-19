import { useState, useMemo, useEffect } from 'react';
import { motion } from 'framer-motion';
import { ArrowUpRight, ArrowDownLeft, Search, Filter } from 'lucide-react';
import { accountAPI, accountSelection, transactionAPI } from '@/services/api';

const formatCurrency = (n: number) => 'Rs ' + n.toLocaleString('en-IN', { minimumFractionDigits: 2 });

const typeLabels: Record<string, string> = {
  transfer: 'Transfer', upi: 'UPI', imps: 'IMPS', neft: 'NEFT', rtgs: 'RTGS', credit: 'Credit', debit: 'Debit',
};

const Transactions = () => {
  const [accounts, setAccounts] = useState<any[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<string>('');
  const [transactions, setTransactions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [sortBy, setSortBy] = useState<'date' | 'amount'>('date');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  const loadTransactions = async (accountNumber?: string) => {
    try {
      setLoading(true);
      const res = await transactionAPI.getTransactions(accountNumber || undefined);
      if (res.data.success) {
        setTransactions(res.data.transactions);
      }
    } catch (err) {
      console.error('Error loading transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const loadAccounts = async () => {
      const res = await accountAPI.getAccounts();
      const availableAccounts = res.data?.accounts || [];
      setAccounts(availableAccounts);
      const storedAccount = accountSelection.get() || availableAccounts[0]?.account_number || '';
      setSelectedAccount(storedAccount);
      if (storedAccount) {
        accountSelection.set(storedAccount);
      }
      await loadTransactions(storedAccount || undefined);
    };

    loadAccounts();
  }, []);

  const filtered = useMemo(() => {
    let list = [...transactions];
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(t =>
        (t.description || '').toLowerCase().includes(q) ||
        (t.receiver_name || '').toLowerCase().includes(q) ||
        (t.sender_name || '').toLowerCase().includes(q) ||
        (t.receiver_account_no || '').toLowerCase().includes(q) ||
        (t.sender_account_no || '').toLowerCase().includes(q)
      );
    }
    if (typeFilter !== 'all') list = list.filter(t => t.type === typeFilter);
    if (fromDate) list = list.filter(t => new Date(t.created_at) >= new Date(fromDate));
    if (toDate) {
      const end = new Date(toDate);
      end.setHours(23, 59, 59, 999);
      list = list.filter(t => new Date(t.created_at) <= end);
    }
    list.sort((a, b) => {
      const mul = sortDir === 'desc' ? -1 : 1;
      if (sortBy === 'date') return mul * (new Date(a.created_at).getTime() - new Date(b.created_at).getTime());
      return mul * (a.amount - b.amount);
    });
    return list;
  }, [transactions, search, typeFilter, fromDate, toDate, sortBy, sortDir]);

  const exportCsv = () => {
    const header = ['Date', 'Type', 'Entry', 'Description', 'Counterparty', 'Amount', 'Reference'];
    const rows = filtered.map((tx) => {
      const isOutgoing = tx.entry_type !== 'credit';
      const counterparty = isOutgoing
        ? tx.receiver_name || tx.receiver_account_no || 'External account'
        : tx.sender_name || tx.sender_account_no || 'External source';
      return [
        new Date(tx.created_at).toLocaleDateString(),
        tx.type,
        tx.entry_type,
        tx.description,
        counterparty,
        tx.amount,
        tx.reference_number || '',
      ];
    });
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `vibebank-statement-${selectedAccount || 'all'}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const exportPdf = () => {
    const accountLabel = accounts.find((account) => account.account_number === selectedAccount);
    const rows = filtered.map((tx) => {
      const isOutgoing = tx.entry_type !== 'credit';
      const counterparty = isOutgoing
        ? tx.receiver_name || tx.receiver_account_no || 'External account'
        : tx.sender_name || tx.sender_account_no || 'External source';
      return `
        <tr>
          <td>${new Date(tx.created_at).toLocaleDateString()}</td>
          <td>${tx.type.toUpperCase()} / ${tx.entry_type.toUpperCase()}</td>
          <td>${tx.description || ''}</td>
          <td>${counterparty}</td>
          <td style="text-align:right;">${isOutgoing ? '-' : '+'}${formatCurrency(tx.amount)}</td>
        </tr>
      `;
    }).join('');

    const summaryCredits = filtered.filter((tx) => tx.entry_type === 'credit').reduce((sum, tx) => sum + tx.amount, 0);
    const summaryDebits = filtered.filter((tx) => tx.entry_type !== 'credit').reduce((sum, tx) => sum + tx.amount, 0);

    const printable = window.open('', '_blank', 'width=900,height=700');
    if (!printable) return;

    printable.document.write(`
      <html>
        <head>
          <title>Vibe Bank Statement</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 32px; color: #111827; }
            h1 { margin: 0 0 8px; }
            p { margin: 4px 0; color: #4b5563; }
            .summary { display: flex; gap: 24px; margin: 24px 0; }
            .card { border: 1px solid #d1d5db; border-radius: 12px; padding: 16px; min-width: 180px; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { border-bottom: 1px solid #e5e7eb; padding: 10px 8px; font-size: 12px; text-align: left; }
            th { text-transform: uppercase; color: #6b7280; font-size: 11px; }
          </style>
        </head>
        <body>
          <h1>Vibe Bank Statement</h1>
          <p>Generated on ${new Date().toLocaleString()}</p>
          <p>Account: ${accountLabel ? `${accountLabel.account_type} - ${accountLabel.account_number}` : selectedAccount}</p>
          <p>Entries: ${filtered.length}</p>
          <div class="summary">
            <div class="card"><strong>Total Credits</strong><p>${formatCurrency(summaryCredits)}</p></div>
            <div class="card"><strong>Total Debits</strong><p>${formatCurrency(summaryDebits)}</p></div>
          </div>
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Description</th>
                <th>Counterparty</th>
                <th style="text-align:right;">Amount</th>
              </tr>
            </thead>
            <tbody>${rows || '<tr><td colspan="5">No transactions found</td></tr>'}</tbody>
          </table>
        </body>
      </html>
    `);
    printable.document.close();
    printable.focus();
    printable.print();
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Transaction History</h1>
        <p className="text-muted-foreground mt-1">{transactions.length} total ledger entries</p>
      </motion.div>

      <div className="banking-card flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search transactions..."
              className="w-full bg-secondary border border-border rounded-lg pl-10 pr-4 py-2.5 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
            />
          </div>
          <div className="flex gap-3 items-center">
            <Filter className="w-4 h-4 text-muted-foreground" />
            <select
              value={typeFilter}
              onChange={e => setTypeFilter(e.target.value)}
              className="bg-secondary border border-border rounded-lg px-3 py-2.5 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option value="all">All Types</option>
              {Object.entries(typeLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <select
              value={`${sortBy}-${sortDir}`}
              onChange={e => {
                const [s, d] = e.target.value.split('-');
                setSortBy(s as 'date' | 'amount');
                setSortDir(d as 'asc' | 'desc');
              }}
              className="bg-secondary border border-border rounded-lg px-3 py-2.5 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option value="date-desc">Newest First</option>
              <option value="date-asc">Oldest First</option>
              <option value="amount-desc">Highest Amount</option>
              <option value="amount-asc">Lowest Amount</option>
            </select>
          </div>
          <div className="flex gap-3 items-center">
            <input
              type="date"
              value={fromDate}
              onChange={e => setFromDate(e.target.value)}
              className="bg-secondary border border-border rounded-lg px-3 py-2.5 text-foreground text-sm"
            />
            <input
              type="date"
              value={toDate}
              onChange={e => setToDate(e.target.value)}
              className="bg-secondary border border-border rounded-lg px-3 py-2.5 text-foreground text-sm"
            />
            <button
              onClick={exportCsv}
              type="button"
              className="px-4 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-medium"
            >
              Export CSV
            </button>
            <button
              onClick={exportPdf}
              type="button"
              className="px-4 py-2.5 rounded-lg bg-secondary text-foreground text-sm font-medium border border-border"
            >
              Export PDF
            </button>
          </div>
        </div>

        {accounts.length > 0 && (
          <div className="flex items-center gap-3">
            <span className="text-sm text-muted-foreground">Account</span>
            <select
              value={selectedAccount}
              onChange={async (e) => {
                const next = e.target.value;
                setSelectedAccount(next);
                accountSelection.set(next);
                await loadTransactions(next);
              }}
              className="bg-secondary border border-border rounded-lg px-3 py-2.5 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              {accounts.map((account) => (
                <option key={account.id} value={account.account_number}>
                  {account.account_type} - {account.account_number}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="banking-card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-6 py-4">Description</th>
                <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-6 py-4">Counterparty</th>
                <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-6 py-4">Type</th>
                <th className="text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider px-6 py-4">Date</th>
                <th className="text-right text-xs font-semibold text-muted-foreground uppercase tracking-wider px-6 py-4">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center text-muted-foreground">Loading transactions...</td>
                </tr>
              ) : filtered.length > 0 ? (
                filtered.map((tx, i) => {
                  const isOutgoing = tx.entry_type !== 'credit';
                  const counterparty = isOutgoing
                    ? tx.receiver_name || tx.receiver_account_no || 'External account'
                    : tx.sender_name || tx.sender_account_no || 'External source';

                  return (
                    <motion.tr key={tx.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.03 }} className="hover:bg-secondary/50 transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${isOutgoing ? 'bg-destructive/10' : 'bg-success/10'}`}>
                            {isOutgoing ? <ArrowUpRight className="w-4 h-4 text-destructive" /> : <ArrowDownLeft className="w-4 h-4 text-success" />}
                          </div>
                          <span className="text-sm font-medium text-foreground">{tx.description}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-sm text-muted-foreground">
                        {isOutgoing ? `To: ${counterparty}` : `From: ${counterparty}`}
                      </td>
                      <td className="px-6 py-4">
                        <span className="text-xs font-medium bg-secondary px-2.5 py-1 rounded-md text-foreground">
                          {tx.type.toUpperCase()} / {tx.entry_type.toUpperCase()}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-muted-foreground">{new Date(tx.created_at).toLocaleDateString()}</td>
                      <td className="px-6 py-4 text-right">
                        <span className={`text-sm font-semibold font-mono ${isOutgoing ? 'text-destructive' : 'text-success'}`}>
                          {isOutgoing ? '-' : '+'}{formatCurrency(tx.amount)}
                        </span>
                      </td>
                    </motion.tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center text-muted-foreground">No transactions found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Transactions;
