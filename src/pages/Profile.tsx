import { useEffect, useState } from 'react';
import { useBanking } from '@/contexts/BankingContext';
import { motion } from 'framer-motion';
import { User, Lock, KeyRound, Check, AlertCircle, Wallet } from 'lucide-react';
import { accountAPI, accountSelection } from '@/services/api';

const Profile = () => {
  const { user, updateProfile, changePassword, setTransactionPin } = useBanking();
  const [tab, setTab] = useState<'info' | 'password' | 'pin' | 'accounts'>('info');

  const [name, setName] = useState(user?.name || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [address, setAddress] = useState(user?.address || '');

  const [oldPwd, setOldPwd] = useState('');
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');

  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [pinPassword, setPinPassword] = useState('');

  const [accounts, setAccounts] = useState<any[]>([]);
  const [newAccountType, setNewAccountType] = useState('Savings');

  const [message, setMessage] = useState<{ success: boolean; text: string } | null>(null);

  const loadAccounts = async () => {
    try {
      const res = await accountAPI.getAccounts();
      if (res.data.success) {
        setAccounts(res.data.accounts);
        if (!accountSelection.get() && res.data.accounts[0]?.account_number) {
          accountSelection.set(res.data.accounts[0].account_number);
        }
      }
    } catch (err) {
      console.error('Failed to load accounts', err);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  useEffect(() => {
    setName(user?.name || '');
    setPhone(user?.phone || '');
    setAddress(user?.address || '');
  }, [user]);

  const handleProfileSave = async (e: React.FormEvent) => {
    e.preventDefault();
    const res = await updateProfile({ name, phone, address });
    setMessage({ success: res.success, text: res.message });
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPwd !== confirmPwd) { setMessage({ success: false, text: 'Passwords do not match' }); return; }
    if (!/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{6,}$/.test(newPwd)) { setMessage({ success: false, text: 'Password must be at least 6 characters and include 1 uppercase letter, 1 number, and 1 special character' }); return; }
    const res = await changePassword(oldPwd, newPwd);
    setMessage({ success: res.success, text: res.message });
    if (res.success) { setOldPwd(''); setNewPwd(''); setConfirmPwd(''); }
  };

  const handlePinChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPin.length !== 4) { setMessage({ success: false, text: 'PIN must be 4 digits' }); return; }
    if (newPin !== confirmPin) { setMessage({ success: false, text: 'PINs do not match' }); return; }
    if (!pinPassword) { setMessage({ success: false, text: 'Password is required to update PIN' }); return; }
    const res = await setTransactionPin(newPin, pinPassword);
    setMessage({ success: res.success, text: res.message });
    if (res.success) { setNewPin(''); setConfirmPin(''); setPinPassword(''); }
  };

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await accountAPI.createAccount({ account_type: newAccountType });
      if (res.data.success) {
        await loadAccounts();
        accountSelection.set(res.data.account.account_number);
        setMessage({ success: true, text: 'New account created successfully' });
      } else {
        setMessage({ success: false, text: res.data.message || 'Could not create account' });
      }
    } catch (err) {
      setMessage({ success: false, text: 'Could not create account' });
    }
  };

  const tabs = [
    { id: 'info' as const, label: 'Personal Info', icon: User },
    { id: 'password' as const, label: 'Password', icon: Lock },
    { id: 'pin' as const, label: 'Transaction PIN', icon: KeyRound },
    { id: 'accounts' as const, label: 'Accounts', icon: Wallet },
  ];

  const inputCls = 'w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition';

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Profile Settings</h1>
        <p className="text-muted-foreground mt-1">Manage your Vibe Bank account details</p>
      </motion.div>

      <div className="banking-card flex items-center gap-5">
        <div className="w-16 h-16 rounded-full bg-primary/20 flex items-center justify-center text-2xl font-bold text-primary">
          {user?.name?.charAt(0)}
        </div>
        <div>
          <p className="text-lg font-semibold text-foreground">{user?.name}</p>
          <p className="text-sm text-muted-foreground">{user?.email}</p>
          <div className="flex items-center gap-2 mt-2">
            <span className="text-xs text-muted-foreground">Role:</span>
            <span className={`text-xs px-2 py-1 rounded-full ${user?.role === 'admin' ? 'bg-primary/10 text-primary' : 'bg-secondary text-muted-foreground'}`}>
              {user?.role || 'user'}
            </span>
          </div>
          <p className="text-xs text-muted-foreground mt-1">Accounts: <span className="font-mono text-foreground">{accounts.length}</span></p>
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => { setTab(id); setMessage(null); }}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium transition-all ${
              tab === id ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'
            }`}
          >
            <Icon className="w-4 h-4" /> {label}
          </button>
        ))}
      </div>

      {message && (
        <div className={`flex items-center gap-3 px-4 py-3 rounded-lg ${message.success ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'}`}>
          {message.success ? <Check className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
          <span className="text-sm">{message.text}</span>
        </div>
      )}

      <div className="banking-card">
        {tab === 'info' && (
          <form onSubmit={handleProfileSave} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Username</label>
              <input type="text" value={user?.username || ''} className={`${inputCls} opacity-60 cursor-not-allowed`} disabled />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Account Holder Name</label>
              <input type="text" value={name} onChange={e => setName(e.target.value)} className={inputCls} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Email</label>
              <input type="email" value={user?.email || ''} className={`${inputCls} opacity-60 cursor-not-allowed`} disabled />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Phone</label>
              <input type="tel" value={phone} onChange={e => setPhone(e.target.value)} className={inputCls} />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Address</label>
              <textarea value={address} onChange={e => setAddress(e.target.value)} className={`${inputCls} resize-none`} rows={3} />
            </div>
            <button type="submit" className="px-6 py-3 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:opacity-90 transition">
              Save Changes
            </button>
          </form>
        )}

        {tab === 'password' && (
          <form onSubmit={handlePasswordChange} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Current Password</label>
              <input type="password" value={oldPwd} onChange={e => setOldPwd(e.target.value)} className={inputCls} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">New Password</label>
              <input type="password" value={newPwd} onChange={e => setNewPwd(e.target.value)} className={inputCls} pattern="^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{6,}$" title="Password must be at least 6 characters and include 1 uppercase letter, 1 number, and 1 special character" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Confirm New Password</label>
              <input type="password" value={confirmPwd} onChange={e => setConfirmPwd(e.target.value)} className={inputCls} required />
            </div>
            <button type="submit" className="px-6 py-3 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:opacity-90 transition">
              Change Password
            </button>
          </form>
        )}

        {tab === 'pin' && (
          <form onSubmit={handlePinChange} className="space-y-5 max-w-xs">
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">New 4-Digit PIN</label>
              <input type="password" value={newPin} onChange={e => setNewPin(e.target.value.replace(/\D/g, '').slice(0, 4))} maxLength={4} className={`${inputCls} font-mono tracking-[0.3em] text-center`} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Confirm PIN</label>
              <input type="password" value={confirmPin} onChange={e => setConfirmPin(e.target.value.replace(/\D/g, '').slice(0, 4))} maxLength={4} className={`${inputCls} font-mono tracking-[0.3em] text-center`} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Account Password</label>
              <input type="password" value={pinPassword} onChange={e => setPinPassword(e.target.value)} className={inputCls} required />
            </div>
            <button type="submit" className="px-6 py-3 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:opacity-90 transition">
              Update PIN
            </button>
          </form>
        )}

        {tab === 'accounts' && (
          <div className="space-y-6">
            <form onSubmit={handleCreateAccount} className="flex flex-col md:flex-row gap-4 items-start md:items-end">
              <div className="flex-1">
                <label className="block text-sm font-medium text-muted-foreground mb-2">New Account Type</label>
                <select value={newAccountType} onChange={e => setNewAccountType(e.target.value)} className={inputCls}>
                  <option value="Savings">Savings</option>
                  <option value="Current">Current</option>
                  <option value="Student">Student</option>
                </select>
              </div>
              <button type="submit" className="px-6 py-3 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:opacity-90 transition">
                Create Account
              </button>
            </form>

            <div className="space-y-3">
              {accounts.map((account) => {
                const isSelected = accountSelection.get() === account.account_number;
                return (
                  <div key={account.id} className="border border-border rounded-xl p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-foreground">{account.account_type} Account</p>
                      <p className="text-xs text-muted-foreground font-mono">{account.account_number}</p>
                      <p className="text-xs text-muted-foreground">Balance: Rs {Number(account.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
                    </div>
                    <button
                      onClick={() => {
                        accountSelection.set(account.account_number);
                        setMessage({ success: true, text: `Selected ${account.account_number} as active account` });
                      }}
                      className={`px-4 py-2 rounded-lg text-sm font-medium ${isSelected ? 'bg-success/10 text-success' : 'bg-secondary text-foreground'}`}
                    >
                      {isSelected ? 'Active Account' : 'Use This Account'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
