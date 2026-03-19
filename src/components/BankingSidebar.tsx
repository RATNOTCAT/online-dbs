import { NavLink, useLocation } from 'react-router-dom';
import { LayoutDashboard, ArrowLeftRight, History, CreditCard, UserCircle, LogOut, Landmark, Bell, Shield, Target, MessageSquare } from 'lucide-react';
import { useBanking } from '@/contexts/BankingContext';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/payments', label: 'Payments', icon: ArrowLeftRight },
  { to: '/transactions', label: 'Transactions', icon: History },
  { to: '/credit-card', label: 'Credit Card', icon: CreditCard },
  { to: '/notifications', label: 'Notifications', icon: Bell },
  { to: '/assistant', label: 'Assistant', icon: MessageSquare },
  { to: '/goals', label: 'Savings Goals', icon: Target },
  { to: '/profile', label: 'Profile', icon: UserCircle },
];

const BankingSidebar = () => {
  const { logout, user } = useBanking();
  const location = useLocation();

  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-sidebar border-r border-sidebar-border flex flex-col z-50">
      <div className="p-6 border-b border-sidebar-border">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
            <Landmark className="w-5 h-5 text-primary" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-foreground tracking-tight">vibebank</h1>
            <p className="text-xs text-muted-foreground">Secure Banking</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {[...navItems, ...(user?.role === 'admin' ? [{ to: '/admin', label: 'Admin', icon: Shield }] : [])].map(({ to, label, icon: Icon }) => {
          const isActive = location.pathname === to;
          return (
            <NavLink
              key={to}
              to={to}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ${
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-sidebar-foreground hover:bg-sidebar-hover hover:text-foreground'
              }`}
            >
              <Icon className="w-5 h-5" />
              {label}
            </NavLink>
          );
        })}
      </nav>

      <div className="p-4 border-t border-sidebar-border">
        <div className="flex items-center gap-3 px-4 py-2 mb-3">
          <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-xs font-bold text-primary">
            {user?.name?.charAt(0) || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-foreground truncate">{user?.name}</p>
            <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium text-destructive hover:bg-destructive/10 w-full transition-colors"
        >
          <LogOut className="w-5 h-5" />
          Sign Out
        </button>
      </div>
    </aside>
  );
};

export default BankingSidebar;
