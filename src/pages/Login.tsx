import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Landmark, Eye, EyeOff } from 'lucide-react';
import { motion } from 'framer-motion';
import axios from 'axios';
import { useBanking } from '@/contexts/BankingContext';
import { authAPI, accountSelection } from '@/services/api';

const Login = () => {
  const navigate = useNavigate();
  const { setAuthenticatedUser } = useBanking();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    const normalizedIdentifier = identifier.includes('@') ? identifier.toLowerCase() : identifier;

    try {
      const response = await authAPI.login({ identifier: normalizedIdentifier, password });

      if (response.data.success) {
        localStorage.setItem('user_id', response.data.user_id);
        localStorage.setItem('user_name', response.data.name);
        localStorage.setItem('user_username', response.data.username || '');
        localStorage.setItem('access_token', response.data.access_token);
        accountSelection.clear();

        await setAuthenticatedUser(response.data.user_id, response.data.name, response.data.email || normalizedIdentifier);
        navigate('/dashboard');
      } else {
        setError('Invalid credentials');
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || 'Invalid credentials');
      } else {
        setError('Invalid credentials');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-4">
            <Landmark className="w-7 h-7 text-primary" />
          </div>
          <h1 className="text-2xl font-bold text-foreground">Welcome to Vibe Bank</h1>
          <p className="text-muted-foreground mt-1">Sign in with your username or email</p>
        </div>

        <div className="banking-card">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="bg-red-500/10 text-red-500 text-sm px-4 py-3 rounded-lg">
                {error}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium mb-2">Username or Email</label>
              <input
                type="text"
                value={identifier}
                onChange={(e) => {
                  const value = e.target.value;
                  setIdentifier(value.includes('@') ? value.toLowerCase() : value);
                }}
                className="w-full bg-secondary border rounded-lg px-4 py-3 text-sm"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Password</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-secondary border rounded-lg px-4 py-3 pr-12 text-sm"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-3 top-1/2 -translate-y-1/2"
                >
                  {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 rounded-lg bg-primary text-white font-semibold text-sm hover:opacity-90 transition"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-sm mt-5">
            Don't have an account?{' '}
            <Link to="/register" className="text-primary font-medium">
              Create one
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
};

export default Login;
