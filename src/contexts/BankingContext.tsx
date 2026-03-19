import React, { createContext, useContext, useEffect, useState } from 'react';
import { accountAPI, accountSelection, authAPI, userAPI } from '@/services/api';

export interface User {
  id: string;
  username: string;
  name: string;
  email: string;
  role: string;
  phone: string;
  address: string;
  accountNumber: string;
  transactionPin: string;
  joinDate: string;
}

export interface Transaction {
  id: string;
  type: 'transfer' | 'upi' | 'imps' | 'neft' | 'rtgs' | 'credit' | 'debit';
  sender: string;
  receiver: string;
  amount: number;
  date: string;
  description: string;
  status: 'completed' | 'pending' | 'failed';
}

export interface CreditCard {
  number: string;
  holderName: string;
  expiry: string;
  cvv: string;
  limit: number;
  used: number;
}

interface ActionResult {
  success: boolean;
  message: string;
}

interface BankingState {
  isAuthenticated: boolean;
  user: User | null;
  balance: number;
  transactions: Transaction[];
  creditCard: CreditCard | null;
  login: (identifier: string, password: string) => Promise<boolean>;
  register: (username: string, name: string, email: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  setAuthenticatedUser: (userId: string, userName: string, email: string) => Promise<void>;
  makeTransfer: (receiver: string, amount: number, type: Transaction['type'], pin: string, description?: string) => Promise<ActionResult>;
  updateProfile: (updates: Partial<User>) => Promise<ActionResult>;
  changePassword: (oldPassword: string, newPassword: string) => Promise<ActionResult>;
  setTransactionPin: (pin: string, password?: string) => Promise<ActionResult>;
}

const BankingContext = createContext<BankingState | null>(null);

export const useBanking = () => {
  const ctx = useContext(BankingContext);
  if (!ctx) throw new Error('useBanking must be used within BankingProvider');
  return ctx;
};

export const BankingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(Boolean(localStorage.getItem('access_token')));
  const [user, setUser] = useState<User | null>(null);
  const [balance, setBalance] = useState(0);
  const [transactions] = useState<Transaction[]>([]);
  const [creditCard] = useState<CreditCard | null>(null);

  const loadSessionData = async () => {
    const token = localStorage.getItem('access_token');
    if (!token) {
      setIsAuthenticated(false);
      setUser(null);
      setBalance(0);
      return;
    }

    try {
      const [profileRes, accountsRes] = await Promise.all([
        userAPI.getProfile(),
        accountAPI.getAccounts(),
      ]);

      const profile = profileRes.data?.user;
      const accounts = accountsRes.data?.accounts || [];
      const selectedAccount = accountSelection.get() || profile?.account_number || accounts[0]?.account_number || '';

      if (selectedAccount) {
        accountSelection.set(selectedAccount);
      }

      let balanceValue = 0;
      if (selectedAccount) {
        const balanceRes = await accountAPI.getBalance(selectedAccount);
        if (balanceRes.data?.success) {
          balanceValue = balanceRes.data.balance;
        }
      }

      setUser({
        id: profile?.id || localStorage.getItem('user_id') || '',
        username: profile?.username || localStorage.getItem('user_username') || '',
        name: profile?.name || localStorage.getItem('user_name') || '',
        email: profile?.email || '',
        role: profile?.role || localStorage.getItem('user_role') || 'user',
        phone: profile?.phone || '',
        address: profile?.address || '',
        accountNumber: selectedAccount || '',
        transactionPin: '',
        joinDate: profile?.joined_date || '',
      });
      setBalance(balanceValue);
      setIsAuthenticated(true);
    } catch (error) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_id');
      localStorage.removeItem('user_name');
      localStorage.removeItem('user_username');
      localStorage.removeItem('user_role');
      accountSelection.clear();
      setIsAuthenticated(false);
      setUser(null);
      setBalance(0);
    }
  };

  useEffect(() => {
    loadSessionData();
  }, []);

  const login = async (identifier: string, password: string) => {
    try {
      const response = await authAPI.login({ identifier, password });
      if (response.data?.success) {
        localStorage.setItem('access_token', response.data.access_token);
        localStorage.setItem('user_id', response.data.user_id);
        localStorage.setItem('user_name', response.data.name);
        localStorage.setItem('user_username', response.data.username || '');
        localStorage.setItem('user_role', response.data.role || 'user');
        await setAuthenticatedUser(response.data.user_id, response.data.name, response.data.email || identifier);
        return true;
      }
      return false;
    } catch {
      return false;
    }
  };

  const register = async (username: string, name: string, email: string, password: string) => {
    try {
      const response = await authAPI.register({ username, name, email, password });
      if (response.data?.success) {
        localStorage.setItem('access_token', response.data.access_token);
        localStorage.setItem('user_id', response.data.user_id);
        localStorage.setItem('user_name', response.data.name);
        localStorage.setItem('user_username', response.data.username || username);
        localStorage.setItem('user_role', response.data.role || 'user');
        if (response.data.account_number) {
          accountSelection.set(response.data.account_number);
        }
        await setAuthenticatedUser(response.data.user_id, response.data.name, response.data.email || email);
        return true;
      }
      return false;
    } catch {
      return false;
    }
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch {
      // Ignore logout transport errors and clear local session anyway.
    }
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_name');
    localStorage.removeItem('user_username');
    localStorage.removeItem('user_role');
    accountSelection.clear();
    setIsAuthenticated(false);
    setUser(null);
    setBalance(0);
  };

  const setAuthenticatedUser = async (userId: string, userName: string, email: string) => {
    await loadSessionData();
    setUser((prev) => ({
      id: userId,
      username: prev?.username || localStorage.getItem('user_username') || '',
      name: prev?.name || userName,
      email: prev?.email || email,
      role: prev?.role || localStorage.getItem('user_role') || 'user',
      phone: prev?.phone || '',
      address: prev?.address || '',
      accountNumber: prev?.accountNumber || accountSelection.get() || '',
      transactionPin: '',
      joinDate: prev?.joinDate || '',
    }));
    setIsAuthenticated(true);
  };

  const makeTransfer = async () => {
    return { success: false, message: 'Transfers are handled from the Payments page' };
  };

  const updateProfile = async (updates: Partial<User>): Promise<ActionResult> => {
    try {
      const response = await userAPI.updateProfile({
        name: updates.name,
        phone: updates.phone,
        address: updates.address,
      });
      if (response.data?.success) {
        await loadSessionData();
        return { success: true, message: response.data.message || 'Profile updated successfully' };
      }
      return { success: false, message: response.data?.message || 'Profile update failed' };
    } catch (error: any) {
      return { success: false, message: error?.response?.data?.message || 'Profile update failed' };
    }
  };

  const changePassword = async (oldPassword: string, newPassword: string): Promise<ActionResult> => {
    try {
      const response = await userAPI.changePassword({ old_password: oldPassword, new_password: newPassword });
      return {
        success: Boolean(response.data?.success),
        message: response.data?.message || 'Password changed successfully',
      };
    } catch (error: any) {
      return { success: false, message: error?.response?.data?.message || 'Password change failed' };
    }
  };

  const setTransactionPin = async (pin: string, password?: string): Promise<ActionResult> => {
    try {
      const secret = password || prompt('Enter your password to set transaction PIN') || '';
      if (!secret) {
        return { success: false, message: 'Password is required' };
      }
      const response = await userAPI.setTransactionPin({ pin, password: secret });
      return {
        success: Boolean(response.data?.success),
        message: response.data?.message || 'Transaction PIN set successfully',
      };
    } catch (error: any) {
      return { success: false, message: error?.response?.data?.message || 'Transaction PIN update failed' };
    }
  };

  return (
    <BankingContext.Provider value={{
      isAuthenticated,
      user,
      balance,
      transactions,
      creditCard,
      login,
      register,
      logout,
      setAuthenticatedUser,
      makeTransfer,
      updateProfile,
      changePassword,
      setTransactionPin,
    }}
    >
      {children}
    </BankingContext.Provider>
  );
};
