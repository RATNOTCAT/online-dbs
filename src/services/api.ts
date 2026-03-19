import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';
const getSelectedAccountNumber = () => localStorage.getItem('selected_account_number');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API calls
export const authAPI = {
  register: (data: { username: string; name: string; email: string; password: string; phone?: string }) =>
    api.post('/register', data),
  login: (data: { identifier: string; password: string }) =>
    api.post('/login', data),
  logout: () =>
    api.post('/logout'),
};

// User API calls
export const userAPI = {
  getProfile: () =>
    api.get('/user/profile'),
  updateProfile: (data: any) =>
    api.put('/user/profile', data),
  changePassword: (data: { old_password: string; new_password: string }) =>
    api.post('/user/change-password', data),
  setTransactionPin: (data: { pin: string; password: string }) =>
    api.post('/user/set-transaction-pin', data),
};

// Account API calls
export const accountAPI = {
  getAccount: (accountNumber?: string) =>
    api.get('/account', { params: accountNumber ? { account_number: accountNumber } : undefined }),
  getBalance: (accountNumber?: string) =>
    api.get('/account/balance', { params: accountNumber ? { account_number: accountNumber } : undefined }),
  getAccounts: () =>
    api.get('/accounts'),
  createAccount: (data: { account_type: string }) =>
    api.post('/accounts', data),
  lookup: (account_number: string) =>
    api.get(`/account/lookup?account_number=${encodeURIComponent(account_number)}`),
};

// Transaction API calls
export const transactionAPI = {
  getTransactions: (accountNumber?: string) =>
    api.get('/transactions', { params: accountNumber ? { account_number: accountNumber } : undefined }),
  simpleTransfer: (data: { receiver_account: string; amount: number; description: string }) =>
    api.post('/transactions/simple-transfer', { ...data, source_account: getSelectedAccountNumber() || undefined }),
  accountTransfer: (data: {
    receiver_account: string;
    receiver_ifsc: string;
    receiver_name: string;
    amount: number;
    description: string;
  }) =>
    api.post('/transactions/account-transfer', { ...data, source_account: getSelectedAccountNumber() || undefined }),
  upiTransfer: (data: {
    receiver_upi: string;
    receiver_name: string;
    amount: number;
    description: string;
  }) =>
    api.post('/transactions/upi-transfer', { ...data, source_account: getSelectedAccountNumber() || undefined }),
  impsTransfer: (data: {
    receiver_account: string;
    receiver_ifsc: string;
    receiver_name: string;
    amount: number;
    description: string;
  }) =>
    api.post('/transactions/imps', { ...data, source_account: getSelectedAccountNumber() || undefined }),
  neftTransfer: (data: {
    receiver_account: string;
    receiver_ifsc: string;
    receiver_name: string;
    amount: number;
    description: string;
  }) =>
    api.post('/transactions/neft', { ...data, source_account: getSelectedAccountNumber() || undefined }),
  rtgsTransfer: (data: {
    receiver_account: string;
    receiver_ifsc: string;
    receiver_name: string;
    amount: number;
    description: string;
  }) =>
    api.post('/transactions/rtgs', { ...data, source_account: getSelectedAccountNumber() || undefined }),
};

export const beneficiaryAPI = {
  getBeneficiaries: () =>
    api.get('/beneficiaries'),
  createBeneficiary: (data: {
    type: 'account' | 'upi';
    name: string;
    nickname?: string;
    account_number?: string;
    ifsc_code?: string;
    upi_id?: string;
  }) =>
    api.post('/beneficiaries', data),
  deleteBeneficiary: (beneficiaryId: string) =>
    api.delete(`/beneficiaries/${beneficiaryId}`),
};

export const notificationAPI = {
  getNotifications: () =>
    api.get('/notifications'),
  markRead: (notificationId: string) =>
    api.post(`/notifications/${notificationId}/read`),
  markAllRead: () =>
    api.post('/notifications/read-all'),
};

export const adminAPI = {
  getSummary: () =>
    api.get('/user/admin-summary'),
  unlockUser: (userId: string) =>
    api.post(`/user/admin/users/${userId}/unlock`),
  sendBroadcast: (data: { title: string; message: string }) =>
    api.post('/user/admin/broadcast', data),
};

export const goalAPI = {
  getGoals: () =>
    api.get('/goals'),
  createGoal: (data: { title: string; description?: string; category?: string; target_amount: number; target_date?: string }) =>
    api.post('/goals', data),
  contributeToGoal: (goalId: string, data: { amount: number; account_number?: string }) =>
    api.post(`/goals/${goalId}/contribute`, data),
};

export const chatAPI = {
  getStatus: () =>
    api.get('/chat/status'),
  getHistory: () =>
    api.get('/chat/history'),
  getInsights: () =>
    api.get('/chat/insights'),
  sendMessage: (data: { message: string; history?: Array<{ role: string; content: string }> }) =>
    api.post('/chat', data),
  clearHistory: () =>
    api.delete('/chat/history'),
};

// Credit Card API calls
export const cardAPI = {
  getCreditCard: () =>
    api.get('/credit-card'),
};

export const accountSelection = {
  get: () => getSelectedAccountNumber(),
  set: (accountNumber: string) => localStorage.setItem('selected_account_number', accountNumber),
  clear: () => localStorage.removeItem('selected_account_number'),
};

export default api;
