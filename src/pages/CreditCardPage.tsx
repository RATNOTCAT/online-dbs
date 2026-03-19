import { useEffect, useState } from 'react';
import { useBanking } from '@/contexts/BankingContext';
import { motion } from 'framer-motion';
import { CreditCard, Eye, EyeOff, Shield, TrendingUp } from 'lucide-react';
import { cardAPI } from '@/services/api';

const formatCurrency = (n: number) => '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2 });

const CreditCardPage = () => {
  const { user } = useBanking();
  const [creditCard, setCreditCard] = useState<any>(null);
  const [showCvv, setShowCvv] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadCard = async () => {
      try {
        setLoading(true);
        const res = await cardAPI.getCreditCard();
        if (res.data.success) {
          setCreditCard(res.data.card);
        }
      } catch (err) {
        console.error('Error loading credit card:', err);
      } finally {
        setLoading(false);
      }
    };

    loadCard();
  }, []);

  if (loading) {
    return <div className="max-w-4xl mx-auto mt-8 text-center text-muted-foreground">Loading credit card...</div>;
  }

  if (!creditCard) {
    return <div className="max-w-4xl mx-auto mt-8 text-center text-muted-foreground">Credit card not found</div>;
  }

  const utilization = (creditCard.used_limit / creditCard.credit_limit) * 100;
  const available = creditCard.available_balance;

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="text-2xl font-bold text-foreground">Credit Card</h1>
        <p className="text-muted-foreground mt-1">Manage your Vibe Bank credit card</p>
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Card Visual */}
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.1 }}>
          <div className="credit-card-visual aspect-[1.6/1] flex flex-col justify-between">
            <div className="flex justify-between items-start relative z-10">
              <div>
                <p className="text-xs text-foreground/60 uppercase tracking-[0.3em] font-semibold">Vibe Bank</p>
                <p className="text-[10px] text-foreground/40 mt-0.5">Platinum</p>
              </div>
              <CreditCard className="w-10 h-10 text-foreground/30" />
            </div>

            <div className="relative z-10 space-y-4">
              <p className="text-xl sm:text-2xl font-mono text-foreground tracking-[0.25em]">{creditCard.card_number}</p>
              <div className="flex justify-between items-end">
                <div>
                  <p className="text-[10px] text-foreground/50 uppercase">Cardholder</p>
                  <p className="text-sm text-foreground font-medium">{creditCard.holder_name}</p>
                </div>
                <div className="text-right">
                  <p className="text-[10px] text-foreground/50 uppercase">Valid Thru</p>
                  <p className="text-sm text-foreground font-medium">{creditCard.expiry}</p>
                </div>
                <div className="text-right">
                  <p className="text-[10px] text-foreground/50 uppercase">CVV</p>
                  <div className="flex items-center gap-2">
                    <p className="text-sm text-foreground font-mono">{showCvv ? creditCard.cvv : '•••'}</p>
                    <button onClick={() => setShowCvv(!showCvv)} className="text-foreground/50 hover:text-foreground">
                      {showCvv ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Card Details */}
        <div className="space-y-4">
          <div className="stat-card">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm text-muted-foreground">Credit Limit</span>
              <TrendingUp className="w-4 h-4 text-primary" />
            </div>
            <p className="text-xl font-bold text-foreground font-mono">{formatCurrency(creditCard.credit_limit)}</p>
          </div>

          <div className="stat-card">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm text-muted-foreground">Amount Used</span>
            </div>
            <p className="text-xl font-bold text-warning font-mono">{formatCurrency(creditCard.used_limit)}</p>
            <div className="mt-3 h-2 bg-secondary rounded-full overflow-hidden">
              <div className="h-full bg-warning rounded-full transition-all" style={{ width: `${utilization}%` }} />
            </div>
            <p className="text-xs text-muted-foreground mt-2">{utilization.toFixed(1)}% utilized</p>
          </div>

          <div className="stat-card">
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm text-muted-foreground">Available Credit</span>
              <Shield className="w-4 h-4 text-success" />
            </div>
            <p className="text-xl font-bold text-success font-mono">{formatCurrency(available)}</p>
          </div>
        </div>
      </div>

      {/* Card Info */}
      <div className="banking-card">
        <h2 className="text-lg font-semibold text-foreground mb-4">Card Information</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[
            ['Card Type', 'Visa Platinum'],
            ['Account Holder', user?.name || ''],
            ['Billing Cycle', '1st - 30th of each month'],
            ['Due Date', '15th of each month'],
            ['Interest Rate', '18.99% APR'],
            ['Reward Points', '2,450 pts'],
          ].map(([label, value]) => (
            <div key={label} className="flex justify-between py-2 border-b border-border last:border-0">
              <span className="text-sm text-muted-foreground">{label}</span>
              <span className="text-sm font-medium text-foreground">{value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default CreditCardPage;
