import { Navigate } from 'react-router-dom';
import { useBanking } from '@/contexts/BankingContext';

const Index = () => {
  try {
    const { isAuthenticated } = useBanking();
    console.log('Index: isAuthenticated =', isAuthenticated);
    return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />;
  } catch (error) {
    console.error('Index error:', error);
    return <div style={{padding: '20px', color: '#fff'}}>Error loading Banking Context: {error instanceof Error ? error.message : String(error)}</div>;
  }
};

export default Index;
