import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeftRight, Smartphone, Zap, Building2, Landmark, Check, AlertCircle, Trash2 } from 'lucide-react';
import axios from 'axios';
import { accountAPI, accountSelection, beneficiaryAPI, transactionAPI } from '@/services/api';

const methods = [
  { id: 'simple_transfer' as const, label: 'Simple Transfer', icon: ArrowLeftRight, desc: 'Direct transfer' },
  { id: 'transfer' as const, label: 'Account Transfer', icon: Building2, desc: 'With account and IFSC' },
  { id: 'upi' as const, label: 'UPI', icon: Smartphone, desc: 'Instant UPI payment' },
  { id: 'imps' as const, label: 'IMPS', icon: Zap, desc: '24/7 transfer' },
  { id: 'neft' as const, label: 'NEFT', icon: Building2, desc: 'Standard transfer' },
  { id: 'rtgs' as const, label: 'RTGS', icon: Landmark, desc: 'High value transfer' },
];

type PaymentMethod = 'simple_transfer' | 'transfer' | 'upi' | 'imps' | 'neft' | 'rtgs';
type ReceiverValidation =
  | { status: 'idle' }
  | { status: 'valid'; owner?: string }
  | { status: 'not-found' }
  | { status: 'invalid' };

type Beneficiary = {
  id: string;
  type: 'account' | 'upi';
  name: string;
  nickname?: string;
  account_number?: string;
  ifsc_code?: string;
  upi_id?: string;
};

const ACCOUNT_METHODS: PaymentMethod[] = ['simple_transfer', 'transfer', 'imps', 'neft', 'rtgs'];

const Payments = () => {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('simple_transfer');
  const [balance, setBalance] = useState(0);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [loading, setLoading] = useState(false);

  const [receiver, setReceiver] = useState('');
  const [receiverName, setReceiverName] = useState('');
  const [receiverIfsc, setReceiverIfsc] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [saveBeneficiary, setSaveBeneficiary] = useState(false);
  const [beneficiaryNickname, setBeneficiaryNickname] = useState('');
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null);
  const [receiverValidation, setReceiverValidation] = useState<ReceiverValidation>({ status: 'idle' });

  useEffect(() => {
    const loadPaymentsData = async () => {
      try {
        const [accountsRes, beneficiaryRes] = await Promise.all([
          accountAPI.getAccounts(),
          beneficiaryAPI.getBeneficiaries(),
        ]);

        const availableAccounts = accountsRes.data?.accounts || [];
        const savedBeneficiaries = beneficiaryRes.data?.beneficiaries || [];
        const currentAccount = accountSelection.get() || availableAccounts[0]?.account_number || '';

        setAccounts(availableAccounts);
        setBeneficiaries(savedBeneficiaries);
        setSelectedAccount(currentAccount);

        if (currentAccount) {
          accountSelection.set(currentAccount);
          const balanceRes = await accountAPI.getBalance(currentAccount);
          if (balanceRes.data.success) {
            setBalance(balanceRes.data.balance);
          }
        }
      } catch (err) {
        console.error('Error loading payments data:', err);
      }
    };

    loadPaymentsData();
  }, []);

  useEffect(() => {
    let mounted = true;
    const shouldValidate = ACCOUNT_METHODS.includes(selectedMethod);

    if (!shouldValidate) {
      setReceiverValidation({ status: 'idle' });
      return;
    }

    const accountNumber = receiver.trim();
    if (!accountNumber) {
      setReceiverValidation({ status: 'idle' });
      return;
    }

    if (!/^\d{8,16}$/.test(accountNumber)) {
      setReceiverValidation({ status: 'invalid' });
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await accountAPI.lookup(accountNumber);
        if (!mounted) {
          return;
        }
        if (res.data.success) {
          setReceiverValidation({ status: 'valid', owner: res.data.owner_name });
          if (!receiverName.trim()) {
            setReceiverName(res.data.owner_name || '');
          }
        } else {
          setReceiverValidation({ status: 'not-found' });
        }
      } catch (err) {
        if (mounted) {
          setReceiverValidation({ status: 'not-found' });
        }
      }
    }, 500);

    return () => {
      mounted = false;
      clearTimeout(timer);
    };
  }, [receiver, receiverName, selectedMethod]);

  const formatCurrency = (value: number) => `Rs. ${value.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

  const resetTransferForm = () => {
    setReceiver('');
    setReceiverName('');
    setReceiverIfsc('');
    setAmount('');
    setDescription('');
    setSaveBeneficiary(false);
    setBeneficiaryNickname('');
    setReceiverValidation({ status: 'idle' });
  };

  const applyBeneficiary = (beneficiary: Beneficiary) => {
    if (beneficiary.type === 'upi') {
      setSelectedMethod('upi');
      setReceiver(beneficiary.upi_id || '');
      setReceiverName(beneficiary.name || '');
      setReceiverIfsc('');
      setReceiverValidation({ status: 'idle' });
      return;
    }

    setSelectedMethod(beneficiary.ifsc_code ? 'transfer' : 'simple_transfer');
    setReceiver(beneficiary.account_number || '');
    setReceiverName(beneficiary.name || '');
    setReceiverIfsc(beneficiary.ifsc_code || '');
  };

  const persistBeneficiary = async () => {
    const type = selectedMethod === 'upi' ? 'upi' : 'account';
    const payload = type === 'upi'
      ? {
          type,
          name: receiverName.trim(),
          nickname: beneficiaryNickname.trim() || undefined,
          upi_id: receiver.trim(),
        }
      : {
          type,
          name: receiverName.trim(),
          nickname: beneficiaryNickname.trim() || undefined,
          account_number: receiver.trim(),
          ifsc_code: receiverIfsc.trim() || undefined,
        };

    const res = await beneficiaryAPI.createBeneficiary(payload);
    if (res.data?.beneficiary) {
      setBeneficiaries((current) => [res.data.beneficiary, ...current]);
    }
  };

  const deleteBeneficiary = async (beneficiaryId: string) => {
    try {
      await beneficiaryAPI.deleteBeneficiary(beneficiaryId);
      setBeneficiaries((current) => current.filter((beneficiary) => beneficiary.id !== beneficiaryId));
    } catch (err) {
      console.error('Error deleting beneficiary:', err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setResult(null);
    setLoading(true);

    const parsedAmount = parseFloat(amount);
    if (Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setResult({ success: false, message: 'Enter a valid amount' });
      setLoading(false);
      return;
    }

    if (!receiver.trim()) {
      setResult({ success: false, message: 'Enter receiver details' });
      setLoading(false);
      return;
    }

    try {
      let response;

      switch (selectedMethod) {
        case 'simple_transfer':
          response = await transactionAPI.simpleTransfer({
            receiver_account: receiver,
            amount: parsedAmount,
            description: description || 'Simple Transfer',
          });
          break;
        case 'transfer':
          if (!receiverName.trim() || !receiverIfsc.trim()) {
            setResult({ success: false, message: 'Please enter receiver name and IFSC code' });
            setLoading(false);
            return;
          }
          response = await transactionAPI.accountTransfer({
            receiver_account: receiver,
            receiver_ifsc: receiverIfsc,
            receiver_name: receiverName,
            amount: parsedAmount,
            description: description || 'Account Transfer',
          });
          break;
        case 'upi':
          if (!receiverName.trim()) {
            setResult({ success: false, message: 'Please enter receiver name' });
            setLoading(false);
            return;
          }
          response = await transactionAPI.upiTransfer({
            receiver_upi: receiver,
            receiver_name: receiverName,
            amount: parsedAmount,
            description: description || 'UPI Transfer',
          });
          break;
        case 'imps':
          if (!receiverName.trim() || !receiverIfsc.trim()) {
            setResult({ success: false, message: 'Please enter receiver name and IFSC code' });
            setLoading(false);
            return;
          }
          response = await transactionAPI.impsTransfer({
            receiver_account: receiver,
            receiver_ifsc: receiverIfsc,
            receiver_name: receiverName,
            amount: parsedAmount,
            description: description || 'IMPS Transfer',
          });
          break;
        case 'neft':
          if (!receiverName.trim() || !receiverIfsc.trim()) {
            setResult({ success: false, message: 'Please enter receiver name and IFSC code' });
            setLoading(false);
            return;
          }
          response = await transactionAPI.neftTransfer({
            receiver_account: receiver,
            receiver_ifsc: receiverIfsc,
            receiver_name: receiverName,
            amount: parsedAmount,
            description: description || 'NEFT Transfer',
          });
          break;
        case 'rtgs':
          if (parsedAmount < 100000) {
            setResult({ success: false, message: 'RTGS minimum amount is Rs. 1,00,000' });
            setLoading(false);
            return;
          }
          if (!receiverName.trim() || !receiverIfsc.trim()) {
            setResult({ success: false, message: 'Please enter receiver name and IFSC code' });
            setLoading(false);
            return;
          }
          response = await transactionAPI.rtgsTransfer({
            receiver_account: receiver,
            receiver_ifsc: receiverIfsc,
            receiver_name: receiverName,
            amount: parsedAmount,
            description: description || 'RTGS Transfer',
          });
          break;
        default:
          setResult({ success: false, message: 'Invalid payment method' });
          setLoading(false);
          return;
      }

      if (response.data.success) {
        if (saveBeneficiary && receiverName.trim()) {
          try {
            await persistBeneficiary();
          } catch (beneficiaryError: any) {
            const message = axios.isAxiosError(beneficiaryError)
              ? beneficiaryError.response?.data?.message || 'Transfer worked, but the beneficiary could not be saved'
              : 'Transfer worked, but the beneficiary could not be saved';
            setResult({ success: true, message: `${response.data.message}. ${message}` });
            setBalance(response.data.new_balance);
            resetTransferForm();
            setLoading(false);
            return;
          }
        }

        setResult({ success: true, message: response.data.message });
        setBalance(response.data.new_balance);
        resetTransferForm();
      } else {
        setResult({ success: false, message: response.data.message || 'Transaction failed' });
      }
    } catch (err: any) {
      if (axios.isAxiosError(err)) {
        setResult({ success: false, message: err.response?.data?.message || 'Transaction failed' });
      } else {
        setResult({ success: false, message: 'An error occurred. Please try again.' });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Send Money</h1>
        <p className="text-muted-foreground mt-1">Transfer funds using your preferred method</p>
      </motion.div>

      <section className="banking-card">
        <div className="flex items-center justify-between gap-4 mb-4">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Saved Beneficiaries</h2>
            <p className="text-sm text-muted-foreground">Store repeat recipients for faster transfers.</p>
          </div>
          <span className="text-xs text-muted-foreground">{beneficiaries.length} saved</span>
        </div>

        {beneficiaries.length === 0 ? (
          <p className="text-sm text-muted-foreground">No beneficiaries yet. After a successful transfer, save the receiver here for quick reuse.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {beneficiaries.map((beneficiary) => (
              <div key={beneficiary.id} className="rounded-xl border border-border bg-secondary/40 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{beneficiary.nickname || beneficiary.name}</p>
                    <p className="text-xs text-muted-foreground mt-1">{beneficiary.type === 'upi' ? 'UPI beneficiary' : 'Bank beneficiary'}</p>
                    <p className="text-xs text-muted-foreground mt-2 font-mono">
                      {beneficiary.type === 'upi' ? beneficiary.upi_id : beneficiary.account_number}
                    </p>
                    {beneficiary.ifsc_code && (
                      <p className="text-xs text-muted-foreground font-mono">{beneficiary.ifsc_code}</p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteBeneficiary(beneficiary.id)}
                    className="text-muted-foreground hover:text-destructive transition"
                    aria-label="Delete beneficiary"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
                <button
                  type="button"
                  onClick={() => applyBeneficiary(beneficiary)}
                  className="mt-4 text-sm font-medium text-primary hover:opacity-80 transition"
                >
                  Use beneficiary
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {methods.map(({ id, label, icon: Icon, desc }) => (
          <button
            key={id}
            onClick={() => {
              setSelectedMethod(id);
              setResult(null);
            }}
            className={`stat-card text-left transition-all ${selectedMethod === id ? 'border-primary/40 ring-1 ring-primary/20 bg-primary/5' : ''}`}
          >
            <Icon className={`w-5 h-5 mb-2 ${selectedMethod === id ? 'text-primary' : 'text-muted-foreground'}`} />
            <p className="text-xs font-semibold text-foreground">{label}</p>
            <p className="text-[10px] text-muted-foreground mt-0.5 hidden sm:block">{desc}</p>
          </button>
        ))}
      </div>

      <motion.div layout className="banking-card">
        <div className="flex items-center justify-between mb-6 gap-4 flex-wrap">
          <h2 className="text-lg font-semibold text-foreground">{methods.find((method) => method.id === selectedMethod)?.label}</h2>
          <div className="flex items-center gap-3 flex-wrap">
            {accounts.length > 0 && (
              <select
                value={selectedAccount}
                onChange={async (e) => {
                  const nextAccount = e.target.value;
                  setSelectedAccount(nextAccount);
                  accountSelection.set(nextAccount);
                  const res = await accountAPI.getBalance(nextAccount);
                  if (res.data.success) {
                    setBalance(res.data.balance);
                  }
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
            <span className="text-sm text-muted-foreground">
              Balance: <span className="text-foreground font-mono">{formatCurrency(balance)}</span>
            </span>
          </div>
        </div>

        <AnimatePresence mode="wait">
          {result && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg mb-5 ${result.success ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'}`}
            >
              {result.success ? <Check className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
              <span className="text-sm">{result.message}</span>
            </motion.div>
          )}
        </AnimatePresence>

        <form onSubmit={handleSubmit} className="space-y-5">
          {selectedMethod === 'simple_transfer' && (
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Receiver Account Number</label>
              <input
                type="text"
                value={receiver}
                onChange={(e) => setReceiver(e.target.value)}
                placeholder="Enter account number (8-16 digits)"
                className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition font-mono"
                required
              />
              {receiverValidation.status !== 'idle' && (
                <p className={`text-xs mt-2 ${receiverValidation.status === 'valid' ? 'text-success' : 'text-destructive'}`}>
                  {receiverValidation.status === 'valid'
                    ? `Found: ${receiverValidation.owner}`
                    : receiverValidation.status === 'invalid'
                      ? 'Enter 8-16 digit account number'
                      : 'Account not found'}
                </p>
              )}
            </div>
          )}

          {selectedMethod === 'upi' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="block text-sm font-medium text-muted-foreground mb-2">UPI ID</label>
                <input
                  type="text"
                  value={receiver}
                  onChange={(e) => setReceiver(e.target.value)}
                  placeholder="name@upi"
                  className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-muted-foreground mb-2">Receiver Name</label>
                <input
                  type="text"
                  value={receiverName}
                  onChange={(e) => setReceiverName(e.target.value)}
                  placeholder="Full name"
                  className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
                  required
                />
              </div>
            </div>
          )}

          {(selectedMethod === 'transfer' || selectedMethod === 'imps' || selectedMethod === 'neft' || selectedMethod === 'rtgs') && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-medium text-muted-foreground mb-2">Receiver Account Number</label>
                  <input
                    type="text"
                    value={receiver}
                    onChange={(e) => setReceiver(e.target.value)}
                    placeholder="Account number"
                    className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition font-mono"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-muted-foreground mb-2">IFSC Code</label>
                  <input
                    type="text"
                    value={receiverIfsc}
                    onChange={(e) => setReceiverIfsc(e.target.value.toUpperCase())}
                    placeholder="VIBE0001234"
                    className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition font-mono uppercase"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-muted-foreground mb-2">Receiver Name</label>
                <input
                  type="text"
                  value={receiverName}
                  onChange={(e) => setReceiverName(e.target.value)}
                  placeholder="Full name of receiver"
                  className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
                  required
                />
              </div>
            </>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-medium text-muted-foreground mb-2">Amount (Rs.)</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                min="1"
                step="0.01"
                className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition font-mono"
                required
              />
            </div>
            {selectedMethod === 'rtgs' && (
              <div className="pt-7 text-xs text-muted-foreground italic">Minimum amount: Rs. 1,00,000</div>
            )}
          </div>

          {(selectedMethod === 'upi' || ACCOUNT_METHODS.includes(selectedMethod)) && receiver.trim() && receiverName.trim() && (
            <div className="rounded-xl border border-border bg-secondary/30 p-4 space-y-3">
              <label className="flex items-center gap-3 text-sm text-foreground">
                <input
                  type="checkbox"
                  checked={saveBeneficiary}
                  onChange={(e) => setSaveBeneficiary(e.target.checked)}
                  className="rounded border-border"
                />
                Save this recipient as a beneficiary
              </label>
              {saveBeneficiary && (
                <input
                  type="text"
                  value={beneficiaryNickname}
                  onChange={(e) => setBeneficiaryNickname(e.target.value)}
                  placeholder="Nickname (optional), for example Hostel Rent"
                  className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
                />
              )}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-muted-foreground mb-2">Description (optional)</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What is this transfer for?"
              className="w-full bg-secondary border border-border rounded-lg px-4 py-3 text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full md:w-auto px-8 py-3 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:opacity-90 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Processing...' : 'Send Money'}
          </button>
        </form>
      </motion.div>
    </div>
  );
};

export default Payments;
