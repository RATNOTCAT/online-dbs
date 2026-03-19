import { Outlet, Navigate } from 'react-router-dom';
import BankingSidebar from './BankingSidebar';
import { useBanking } from '@/contexts/BankingContext';

const BankingLayout = () => {
  const { isAuthenticated } = useBanking();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  return (
    <div className="min-h-screen bg-background">
      <BankingSidebar />
      <main className="ml-64 p-8 min-h-screen">
        <Outlet />
      </main>
    </div>
  );
};

export default BankingLayout;
