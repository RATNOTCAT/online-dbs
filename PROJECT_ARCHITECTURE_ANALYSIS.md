# Sterling Bank Portal - Complete Architecture Analysis

## Table of Contents
1. [Overview](#overview)
2. [Technologies Used](#technologies-used)
3. [Project Structure](#project-structure)
4. [How Frontend Communicates with Backend](#how-frontend-communicates-with-backend)
5. [How Backend Communicates with Database](#how-backend-communicates-with-database)
6. [Login Authentication Flow](#login-authentication-flow)
7. [Transfer Money Feature Flow](#transfer-money-feature-flow)
8. [Routing Architecture](#routing-architecture)
9. [Role of Axios](#role-of-axios)
10. [Role of CORS](#role-of-cors)
11. [Complete Data Flow](#complete-data-flow)
12. [Architecture Pattern](#architecture-pattern)
13. [Production Improvements](#production-improvements)

---

## Overview

**Sterling Bank Portal** is a full-stack online banking application that allows users to:
- Register and log in securely
- View account balance and details
- Transfer money via multiple methods (Simple Transfer, Account Transfer, UPI, IMPS, NEFT, RTGS)
- View transaction history
- Manage credit cards
- Edit profile information

**Stack Summary:**
- **Frontend**: React + TypeScript + Vite + Tailwind CSS
- **Backend**: Flask (Python) + SQLAlchemy ORM
- **Database**: SQLite (development) / MySQL (production-ready)
- **Authentication**: JWT (JSON Web Tokens)
- **Communication**: REST API + Axios HTTP client

---

## Technologies Used

### Frontend Stack

| Technology | Purpose | Version |
|-----------|---------|---------|
| **React** | UI library for building interactive user interfaces | 18.3.1 |
| **TypeScript** | Adds static typing to JavaScript for safer code | 5.8.3 |
| **Vite** | Lightning-fast build tool and dev server | 5.4.19 |
| **Tailwind CSS** | Utility-first CSS framework for styling | 3.4.17 |
| **Framer Motion** | Animation library for smooth UI transitions | 12.34.1 |
| **React Router** | Client-side routing between pages | 6.30.1 |
| **Axios** | HTTP client for API requests | 1.13.5 |
| **shadcn/ui** | Pre-built accessible UI components (Radix UI based) | Latest |
| **Lucide React** | Beautiful SVG icon library | 0.462.0 |
| **React Hook Form** | Lightweight form state management | 7.61.1 |
| **React Query** | Data fetching and caching library | 5.83.0 |
| **Zod** | TypeScript-first schema validation | 3.25.76 |

### Backend Stack

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Flask** | Python web framework for building REST API | 3.1.0 |
| **SQLAlchemy** | ORM for database operations | 2.0.46 |
| **Flask-SQLAlchemy** | SQLAlchemy integration with Flask | 3.1.1 |
| **Flask-CORS** | Cross-Origin Resource Sharing support | 4.0.0 |
| **Flask-JWT-Extended** | JWT authentication implementation | 4.6.0 |
| **Werkzeug** | WSGI utilities for secure password hashing | 3.1.0 |
| **Python-dotenv** | Environment variable management | 1.0.1 |

---

## Project Structure

```
sterling-bank-portal/
│
├── frontend/                          # React application (port 8083)
│   ├── src/
│   │   ├── components/
│   │   │   ├── BankingLayout.tsx     # Main layout wrapper
│   │   │   ├── BankingSidebar.tsx    # Navigation sidebar
│   │   │   ├── NavLink.tsx           # Navigation link component
│   │   │   └── ui/                   # shadcn/ui components (accordion, buttons, etc.)
│   │   │
│   │   ├── pages/                     # Page components
│   │   │   ├── Index.tsx             # Landing/home page
│   │   │   ├── Login.tsx             # Login page
│   │   │   ├── Register.tsx          # Registration page
│   │   │   ├── Dashboard.tsx         # Main dashboard (balance, account info)
│   │   │   ├── Payments.tsx          # Transfer money page
│   │   │   ├── Transactions.tsx      # Transaction history
│   │   │   ├── CreditCardPage.tsx    # Credit card details
│   │   │   ├── Profile.tsx           # User profile editing
│   │   │   └── NotFound.tsx          # 404 page
│   │   │
│   │   ├── contexts/
│   │   │   └── BankingContext.tsx    # Global state management (React Context API)
│   │   │
│   │   ├── services/
│   │   │   └── api.ts               # Axios API client with all endpoints
│   │   │
│   │   ├── hooks/
│   │   │   ├── use-mobile.tsx        # Mobile device detection hook
│   │   │   └── use-toast.ts          # Toast notification hook
│   │   │
│   │   ├── lib/
│   │   │   └── utils.ts             # Utility functions
│   │   │
│   │   ├── App.tsx                   # App component with routes
│   │   ├── main.tsx                  # Entry point
│   │   └── index.css                 # Global styles
│   │
│   ├── package.json                  # npm dependencies
│   ├── vite.config.ts               # Vite configuration
│   ├── tsconfig.json                # TypeScript configuration
│   └── tailwind.config.ts           # Tailwind CSS configuration
│
├── backend/                           # Flask application (port 5000)
│   ├── app/
│   │   ├── models/                   # Database models
│   │   │   ├── user.py              # User model (authentication, profile)
│   │   │   ├── account.py           # Account model (balance, account number)
│   │   │   ├── transaction.py       # Transaction model (all transfers)
│   │   │   ├── credit_card.py       # Credit card model
│   │   │   └── payment_method.py    # Saved payment methods
│   │   │
│   │   ├── routes/                   # API endpoints
│   │   │   ├── auth.py              # POST /register, POST /login, POST /logout
│   │   │   ├── users.py             # GET/PUT /user/profile, POST /user/change-password
│   │   │   ├── accounts.py          # GET /account, GET /account/balance, GET /account/lookup
│   │   │   ├── transactions.py      # All transaction endpoints (6 payment methods)
│   │   │   └── cards.py             # GET /credit-card
│   │   │
│   │   ├── utils/
│   │   │   └── validators.py        # Email, password, UPI, account number validation
│   │   │
│   │   └── __init__.py              # Flask app factory
│   │
│   ├── config.py                     # Configuration (DB, CORS, JWT)
│   ├── run.py                        # Entry point: python run.py
│   ├── requirements.txt              # Python dependencies
│   ├── banking.db                    # SQLite database (auto-created)
│   └── README.md                     # Backend documentation
│
└── README.md                          # Project README
```

### What Each Folder Does:

**Frontend (`src/`)**
- **components/**: Reusable UI elements (buttons, forms, sidebars)
- **pages/**: Full-page components displayed via routing
- **contexts/**: Global state (who is logged in, balance, transactions)
- **services/**: API client that talks to backend
- **hooks/**: Custom React hooks for shared logic
- **lib/**: Utility functions (formatters, helpers)

**Backend (`app/`)**
- **models/**: Database table definitions (what data is stored)
- **routes/**: API endpoints (what the backend can do)
- **utils/**: Validation logic (check email format, password strength, etc.)

---

## How Frontend Communicates with Backend

### The Bridge: Axios HTTP Client

**File**: `src/services/api.ts`

```typescript
// Creates an HTTP client that points to backend
const api = axios.create({
  baseURL: 'http://localhost:5000/api',  // Backend server address
  headers: {
    'Content-Type': 'application/json',
  },
});

// Authentication: Add JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Communication Flow (Step-by-Step):

1. **User clicks a button** (e.g., "Transfer Money")
2. **Frontend calls an API method** (e.g., `transactionAPI.simpleTransfer()`)
3. **Axios prepares the HTTP request**:
   - Adds backend URL (`http://localhost:5000/api`)
   - Adds JWT token from localStorage in Authorization header
   - Converts data to JSON
4. **Backend receives the request** and processes it
5. **Backend sends JSON response** with success/error
6. **Frontend receives response** and updates UI (show result, update balance, etc.)

### Example: Transfer Money Request

```
FRONTEND                          AXIOS                           BACKEND
┌─────────────┐               ┌──────────────┐              ┌─────────────┐
│ User clicks │               │   HTTP POST  │              │   Flask API │
│  "Transfer" │──────────────>│  with JWT    │─────────────>│   Endpoint  │
│             │               │  token       │              │             │
│ Gets form   │               │  (Axios)     │              │ Validates   │
│ data        │               │              │              │ Processes   │
│             │<─────────────│ JSON Response│<─────────────│ Returns     │
│ Updates UI  │               │  (success)   │              │ result      │
│ Shows msg   │               │              │              │             │
└─────────────┘               └──────────────┘              └─────────────┘
```

### All API Endpoints (Frontend → Backend):

**Authentication**
```
POST /api/register          → Create account
POST /api/login             → Get JWT token
POST /api/logout            → Session cleanup
```

**Account**
```
GET /api/account            → Get account details
GET /api/account/balance    → Get current balance
GET /api/account/lookup     → Find account owner by account number
```

**Transactions**
```
POST /api/transactions/simple-transfer      → Transfer by account number only
POST /api/transactions/account-transfer     → Transfer by account + IFSC
POST /api/transactions/upi-transfer         → UPI transfer
POST /api/transactions/imps                 → IMPS transfer
POST /api/transactions/neft                 → NEFT transfer
POST /api/transactions/rtgs                 → RTGS transfer
GET  /api/transactions                      → Get transaction history
```

**User Profile**
```
GET /api/user/profile           → Get user data
PUT /api/user/profile           → Update user data
POST /api/user/change-password  → Change password
POST /api/user/set-transaction-pin → Set transaction PIN
```

**Credit Card**
```
GET /api/credit-card            → Get card details
```

---

## How Backend Communicates with Database

### The Bridge: SQLAlchemy ORM

**What is an ORM?**
ORM = Object Relational Mapping. Instead of writing SQL queries, you write Python code that auto-converts to SQL.

### Database Schema (6 Tables):

#### 1. users (Authentication & Profile)
```
id              UUID primary key
name            User's full name
email           Unique email (login credential)
password_hash   Encrypted password (never stored plain-text)
phone           Phone number
address         Mailing address
date_of_birth   Birth date
aadhar_number   Indian ID (unique)
pan_number      Tax ID (unique)
transaction_pin 4-digit PIN for transfers
is_active       Account active/suspended
created_at      Account creation timestamp
updated_at      Last modified timestamp
```

#### 2. accounts (Bank Accounts)
```
id              UUID primary key
user_id         Foreign key → users.id (who owns this account)
account_number  Generated 16-digit account number (unique)
ifsc_code       Bank code (STRB0001234)
account_type    "Savings" or "Current"
balance         Available money in account
overdraft_limit Maximum borrowing allowed
is_active       Account active/frozen
created_at      Account creation timestamp
```

#### 3. transactions (Transfer History)
```
id                  UUID primary key
account_id          Foreign key → accounts.id
sender_id           Foreign key → users.id (who sent money)
receiver_id         Foreign key → users.id (who received money)
type                "transfer", "upi", "imps", "neft", "rtgs"
method              Payment method
amount              How much money
description         What was it for
receiver_name       Recipient's name
receiver_account_no Recipient's account
receiver_ifsc       Recipient's bank code
receiver_upi        Recipient's UPI ID
status              "completed", "pending", "failed"
reference_number    Transaction ID
pin_verified        Was PIN entered correctly
created_at          When transaction was requested
completed_at        When transaction was completed
```

#### 4. credit_cards (Credit Cards)
```
id                  UUID primary key
user_id             Foreign key → users.id
card_number         Generated Visa number (unique)
holder_name         Name on card
expiry_month        Month (1-12)
expiry_year         Year (2028, etc.)
cvv                 3-digit security code
credit_limit        Max you can spend (₹50,000)
used_limit          Amount currently used
available_balance   credit_limit - used_limit
is_active           Card active/blocked
created_at          Card creation date
```

#### 5. payment_methods (Saved Methods)
```
id                  UUID primary key
user_id             Foreign key → users.id
type                "upi", "account", "card", etc.
upi_id              Saved UPI ID
account_number      Saved account number
ifsc_code           Saved bank code
account_holder_name Saved name
bank_name           Saved bank
is_primary          Default payment method
is_verified         Has this been verified
created_at          When saved
```

### How SQLAlchemy Works (Example):

**Instead of SQL:**
```sql
-- Raw SQL (Backend doesn't do this)
SELECT * FROM accounts WHERE user_id = '123' AND is_active = true;
```

**Python with SQLAlchemy:**
```python
# app/models/account.py - Define the table structure
class Account(db.Model):
    __tablename__ = 'accounts'
    id = db.Column(db.String(36), primary_key=True)
    user_id = db.Column(db.String(36), db.ForeignKey('users.id'))
    account_number = db.Column(db.String(16), unique=True)
    balance = db.Column(db.Float, default=0.0)

# app/routes/accounts.py - Query the database
@accounts_bp.route('/account', methods=['GET'])
def get_account():
    account = Account.query.filter_by(user_id=user_id).first()
    return jsonify(account.to_dict())
```

### ORM Benefits:
✅ No SQL injection attacks (automatic escaping)
✅ Works with SQLite in dev, MySQL/PostgreSQL in production (just change config)
✅ Type-safe relationships (user.accounts, account.transactions)
✅ Automatic table creation: `db.create_all()`

---

## Login Authentication Flow

### Step-by-Step (Visual):

```
STEP 1: User enters email & password
┌──────────────────────────────────────┐
│ Login Page (Frontend)                │
│ Email: alex@example.com              │
│ Password: ••••••••                   │
│ [Sign In Button]                     │
└──────────────────────────────────────┘
            │
            │ POST /api/login
            │ { email, password }
            ▼
┌──────────────────────────────────────┐
│ Backend: auth.py /login              │
│ 1. Find user by email in database    │
│    SELECT * FROM users WHERE email=?│
│ 2. Compare passwords:                │
│    check_password_hash(stored_hash,  │
│                        entered_pwd)  │
│ 3. If passwords match:               │
│    - Create JWT token (signed)       │
│    - Return token + user data        │
└──────────────────────────────────────┘
            │
            │ Return JSON:
            │ {
            │   success: true,
            │   user_id: "abc123",
            │   name: "Alex Morgan",
            │   access_token: "eyJhbG..."
            │ }
            ▼
┌──────────────────────────────────────┐
│ Frontend: Login.tsx                  │
│ 1. Save token in localStorage:       │
│    localStorage.setItem(              │
│      'access_token',                 │
│      'eyJhbG...'                     │
│    )                                 │
│ 2. Save user info in Context:        │
│    setAuthenticatedUser(id, name)    │
│ 3. Redirect to Dashboard             │
│    navigate('/dashboard')            │
└──────────────────────────────────────┘
```

### What is JWT (JSON Web Token)?

A JWT is a **signed token** that proves "this person is logged in".

**Structure**: `header.payload.signature`

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.  (header: algorithm)
eyJzdWIiOiAiYWJjMTIzIiwgImV4cCI6IDE2MDB}  (payload: user_id, expiry)
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  (signature: secret-signed)
```

**Where the token goes**: Every subsequent request adds it in the Authorization header:
```
POST /api/transactions/simple-transfer
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Backend verification**:
```python
@jwt_required()  # This decorator verifies the token
def simple_transfer():
    user_id = get_jwt_identity()  # Extract user_id from token
    # Now we know who is making this request
```

### Why This is Secure:

1. **Password never sent again** (only once at login)
2. **Token has expiry date** (expires in 24 hours)
3. **Token is signed** (can't be forged without backend's secret key)
4. **Frontend can't modify it** (signature would be invalid)

---

## Transfer Money Feature Flow

### Scenario: Alex sends ₹5,000 to Jane

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND: Payments.tsx                                      │
│ User selects:                                               │
│ - Method: "Simple Transfer"                                 │
│ - Receiver: "4082123456789999" (Jane's account number)     │
│ - Amount: 5000                                              │
│ - Description: "Lunch money"                                │
│ Clicks: [Send Money]                                        │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Axios POST request
                         │ URL: http://localhost:5000/api/transactions/simple-transfer
                         │ Headers: Authorization: Bearer <JWT>
                         │ Body: {
                         │   receiver_account: "408212345678999",
                         │   amount: 5000,
                         │   description: "Lunch money"
                         │ }
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ BACKEND: transactions.py /simple-transfer                   │
│                                                             │
│ 1. Verify JWT token                                         │
│    user_id = get_jwt_identity()  # "alex123"              │
│                                                             │
│ 2. Validate inputs                                          │
│    - Is ₹5,000 a positive number? ✓                        │
│    - Is account number 8-16 digits? ✓                      │
│                                                             │
│ 3. Get Alex's account from database                         │
│    account = Account.query                                 │
│      .filter_by(user_id="alex123").first()                │
│    → Balance: ₹124,750                                      │
│                                                             │
│ 4. Check balance                                            │
│    if 124,750 >= 5,000? ✓ YES                             │
│                                                             │
│ 5. Find Jane's account                                      │
│    jane_account = Account.query                            │
│      .filter_by(account_number="408212345678999")          │
│      .first()                                              │
│    → Found! Jane's account ID: "jane456"                   │
│    → Jane's current balance: ₹45,000                       │
│                                                             │
│ 6. Database transaction (all-or-nothing):                   │
│    START TRANSACTION                                        │
│                                                             │
│    a) Deduct from Alex's account                            │
│       account.balance = 124,750 - 5,000 = 119,750         │
│                                                             │
│    b) Add to Jane's account                                 │
│       jane_account.balance = 45,000 + 5,000 = 50,000      │
│                                                             │
│    c) Create transaction record                             │
│       INSERT INTO transactions {                            │
│         account_id: "alex_acct_id",                        │
│         sender_id: "alex123",                              │
│         receiver_id: "jane456",                            │
│         type: "transfer",                                  │
│         method: "account",                                 │
│         amount: 5000,                                      │
│         description: "Lunch money",                        │
│         receiver_name: "Jane Smith",                       │
│         receiver_account_no: "408212345678999",           │
│         status: "completed",                               │
│         completed_at: 2026-02-18 15:30:45                │
│       }                                                    │
│                                                             │
│    COMMIT TRANSACTION                                       │
│    → If any step fails, ROLLBACK (undo all)               │
│                                                             │
│ 7. Return success response                                  │
│    {                                                        │
│      "success": true,                                       │
│      "message": "Transfer completed successfully",         │
│      "new_balance": 119750,                                │
│      "transaction": { ...transaction details... }          │
│    }                                                        │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Axios receives response
                         │ Response body is JSON
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND: Payments.tsx (handleSubmit)                       │
│                                                             │
│ if (response.data.success) {                                │
│   1. Show success message to user                           │
│      "Transfer completed successfully"                      │
│                                                             │
│   2. Update balance on screen                               │
│      setBalance(119750)  // from response.new_balance      │
│                                                             │
│   3. Clear form fields                                      │
│      setReceiver('')                                        │
│      setAmount('')                                          │
│                                                             │
│   4. Update transaction history                             │
│      (next time user views Transactions page,              │
│       GET /api/transactions will show this transfer)       │
│ }                                                            │
│                                                             │
│ // User sees in UI:                                        │
│ ✓ "Transfer completed successfully"                        │
│ Balance: ₹119,750 (updated from ₹124,750)                 │
└─────────────────────────────────────────────────────────────┘
```

### What Could Go Wrong (Error Handling):

```python
# Validation errors
if not validate_account_number(receiver_account):
    return {"success": False, "message": "Invalid account number"}, 400

if account.balance < amount:
    return {"success": False, "message": "Insufficient balance"}, 400

if not receiver_account_obj:
    return {"success": False, "message": "Receiver account not found"}, 404

# Database errors (caught in try-except)
except Exception as e:
    db.session.rollback()  # UNDO all changes
    return {"success": False, "message": str(e)}, 500
```

---

## Routing Architecture

### Frontend Routing (React Router)

**File**: `App.tsx`
```tsx
<Routes>
  <Route path="/" element={<Index />} />
  <Route path="/login" element={<Login />} />
  <Route path="/register" element={<Register />} />
  
  <Route element={<BankingLayout />}>
    <Route path="/dashboard" element={<Dashboard />} />
    <Route path="/payments" element={<Payments />} />
    <Route path="/transactions" element={<Transactions />} />
    <Route path="/credit-card" element={<CreditCardPage />} />
    <Route path="/profile" element={<Profile />} />
  </Route>
  
  <Route path="*" element={<NotFound />} />
</Routes>
```

**How it works:**
```
URL in browser          → Which component to render
/                      → Index (landing page)
/login                 → Login form
/register              → Registration form
/dashboard             → Main dashboard (inside BankingLayout)
/payments              → Transfer money form (inside BankingLayout)
/transactions          → Transaction history (inside BankingLayout)
/credit-card           → Credit card details (inside BankingLayout)
/profile               → Profile editor (inside BankingLayout)
/any-other-path        → NotFound (404 page)
```

**BankingLayout** adds sidebar and header to protected pages:
```
┌──────────────────────────────────────────┐
│  Header: Bank Logo + User Name           │
├─────────┬────────────────────────────────┤
│ Sidebar │ Page Content                   │
│ Menu    │ (Dashboard/Payments/etc)       │
│         │                                │
│ - Home  │                                │
│ - Pay   │                                │
│ - Trans │                                │
│ - Card  │                                │
│ - Prof  │                                │
└─────────┴────────────────────────────────┘
```

### Backend Routing (Flask Blueprints)

**File**: `app/__init__.py`
```python
# Register all blueprints (route groups)
app.register_blueprint(auth_bp, url_prefix='/api')      # /api/register, /api/login
app.register_blueprint(users_bp, url_prefix='/api')     # /api/user/profile
app.register_blueprint(transactions_bp, url_prefix='/api') # /api/transactions/*
app.register_blueprint(accounts_bp, url_prefix='/api')  # /api/account
app.register_blueprint(cards_bp, url_prefix='/api')     # /api/credit-card
```

### Routing Summary:

**Client-side (Frontend)**: React Router manages what's displayed on the user's screen
**Server-side (Backend)**: Flask routes determine which code runs to handle a request

---

## Role of Axios

### What is Axios?

Axios is a **JavaScript HTTP client** that makes requests from the browser to the backend.

### Why not just use `fetch()`?

`fetch()` is built-in, but Axios is simpler:

```javascript
// Axios (simpler)
transactionAPI.simpleTransfer({ receiver_account, amount, description })

// vs fetch (more verbose)
fetch('http://localhost:5000/api/transactions/simple-transfer', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ receiver_account, amount, description })
})
.then(res => res.json())
.then(data => console.log(data))
```

### Axios Features Used:

**1. Base URL Configuration**
```typescript
const api = axios.create({
  baseURL: 'http://localhost:5000/api',  // Don't repeat this URL
});
```

**2. Auto JWT Injection** (Interceptors)
```typescript
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;  // Add to every request
  }
  return config;
});
```

**3. Method Shortcuts**
```typescript
api.get(url)                    // GET request
api.post(url, data)             // POST with JSON body
api.put(url, data)              // PUT (update)
api.post(url, data)             // DELETE
```

**4. Error Handling**
```typescript
try {
  const response = await authAPI.login({ email, password });
  if (response.data.success) {
    // Success
  }
} catch (error) {
  if (axios.isAxiosError(error)) {
    console.log(error.response?.data?.message);
  }
}
```

### All Axios Calls in This Project:

```typescript
// In src/services/api.ts
export const authAPI = {
  register: (data) => api.post('/register', data),
  login: (data) => api.post('/login', data),
  logout: () => api.post('/logout'),
};

export const userAPI = {
  getProfile: () => api.get('/user/profile'),
  updateProfile: (data) => api.put('/user/profile', data),
  changePassword: (data) => api.post('/user/change-password', data),
  setTransactionPin: (data) => api.post('/user/set-transaction-pin', data),
};

export const accountAPI = {
  getAccount: () => api.get('/account'),
  getBalance: () => api.get('/account/balance'),
  lookup: (account_number) => api.get(`/account/lookup?account_number=${account_number}`),
};

export const transactionAPI = {
  getTransactions: () => api.get('/transactions'),
  simpleTransfer: (data) => api.post('/transactions/simple-transfer', data),
  accountTransfer: (data) => api.post('/transactions/account-transfer', data),
  upiTransfer: (data) => api.post('/transactions/upi-transfer', data),
  impsTransfer: (data) => api.post('/transactions/imps', data),
  neftTransfer: (data) => api.post('/transactions/neft', data),
  rtgsTransfer: (data) => api.post('/transactions/rtgs', data),
};

export const cardAPI = {
  getCreditCard: () => api.get('/credit-card'),
};
```

---

## Role of CORS

### What is CORS?

CORS = **Cross-Origin Resource Sharing**

**The Problem**: Modern browsers block requests between different origins for security.

```
Frontend: http://localhost:8083        ← Different port (different origin)
Backend:  http://localhost:5000        ↓
          Browser: "Are you allowed to talk to backend?"
```

**Without CORS**: ❌ Blocked
```
XMLHttpRequest from origin 'http://localhost:8083' 
has been blocked by CORS policy
```

**With CORS**: ✓ Allowed

### How CORS is Enabled in This Project:

**Backend** (`config.py`):
```python
CORS_ORIGINS = [
    "http://localhost:5173",    # Vite default port
    "http://localhost:3000",    # Another common port
    "http://localhost:8081",    # Vite dev port
    "http://localhost:8082",    # Alternative dev port
    "http://localhost:8083",    # Current dev port
]

# In __init__.py
CORS(app, origins=app.config['CORS_ORIGINS'])
```

**What CORS Does**:

1. **Frontend makes request** to `http://localhost:5000/api/login`
2. **Browser sends OPTIONS preflight request** (automatic):
   ```
   OPTIONS /api/login HTTP/1.1
   Origin: http://localhost:8083
   ```
3. **Backend responds with CORS headers**:
   ```
   Access-Control-Allow-Origin: http://localhost:8083
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE
   Access-Control-Allow-Headers: Content-Type, Authorization
   ```
4. **Browser sees approval** → Allows the actual request ✓

### Production CORS:

```python
# For production (change these)
CORS_ORIGINS = [
    "https://yourbank.com",
    "https://www.yourbank.com",
]
```

---

## Complete Data Flow

### End-to-End Flow: User Registration → Transfer → View History

```
┌────────────────────────────────────────────────────────────────┐
│ STEP 1: USER REGISTRATION                                      │
└────────────────────────────────────────────────────────────────┘

User fills form:
  Name: Alex Morgan
  Email: alex@example.com
  Password: SecurePass123

Frontend (Register.tsx):
  ↓ Calls authAPI.register(...)
  
Axios sends POST to:
  URL: http://localhost:5000/api/register
  Body: {
    "name": "Alex Morgan",
    "email": "alex@example.com",
    "password": "SecurePass123"
  }

Backend (app/routes/auth.py):
  ↓ Receives JSON
  ↓ Validates email format
  ↓ Validates password strength
  ↓ Checks if email already exists
  
  ↓ Creates database records:
    1. INSERT INTO users:
       id: "uuid-123",
       name: "Alex Morgan",
       email: "alex@example.com",
       password_hash: "$2b$12$..." (hashed, not plain-text)
       
    2. INSERT INTO accounts:
       id: "acct-uuid",
       user_id: "uuid-123",
       account_number: "4082" + random 12 digits = "408212345678999"
       balance: 100000.0 (initial balance)
       
    3. INSERT INTO credit_cards:
       id: "card-uuid",
       user_id: "uuid-123",
       card_number: "4532123456789012" (auto-generated)
       holder_name: "ALEX MORGAN"
       credit_limit: 50000.0

Backend generates JWT token (signed with secret key):
  Payload: user_id: "uuid-123"
  Expires: 24 hours from now

Backend returns JSON response:
  {
    "success": true,
    "user_id": "uuid-123",
    "name": "Alex Morgan",
    "email": "alex@example.com",
    "account_number": "408212345678999",
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }

Frontend receives and stores:
  localStorage.setItem('user_id', 'uuid-123')
  localStorage.setItem('user_name', 'Alex Morgan')
  localStorage.setItem('access_token', 'eyJhbGci...')
  
Frontend redirects to Dashboard


┌────────────────────────────────────────────────────────────────┐
│ STEP 2: USER VIEWS DASHBOARD                                   │
└────────────────────────────────────────────────────────────────┘

Frontend (Dashboard.tsx):
  ↓ useEffect runs on page load
  ↓ Calls accountAPI.getBalance()
  
Axios sends GET with JWT:
  URL: http://localhost:5000/api/account/balance
  Headers: Authorization: Bearer eyJhbGci...

Backend (app/routes/accounts.py):
  ↓ Receives request
  ↓ Verifies JWT token is valid and not expired
  ↓ Extracts user_id from token: "uuid-123"
  
  ↓ Queries database:
    SELECT * FROM accounts 
    WHERE user_id = "uuid-123"
    
  ↓ Returns:
    {
      "success": true,
      "balance": 100000.0,
      "account_number": "408212345678999",
      "account_type": "Savings"
    }

Frontend receives and displays:
  ┌──────────────────────────┐
  │  Account Balance         │
  │  ₹100,000.00            │
  │  Account: 408212345678999│
  │  Type: Savings           │
  └──────────────────────────┘


┌────────────────────────────────────────────────────────────────┐
│ STEP 3: USER MAKES TRANSFER                                    │
└────────────────────────────────────────────────────────────────┘

User fills transfer form:
  Receiver Account: "408298765432111"
  Amount: 5000
  Description: "Groceries"

Frontend (Payments.tsx):
  ↓ User clicks "Send Money"
  ↓ Validates form (amount > 0, etc.)
  ↓ Calls transactionAPI.simpleTransfer(...)

Axios sends POST with JWT:
  URL: http://localhost:5000/api/transactions/simple-transfer
  Headers: Authorization: Bearer eyJhbGci...
  Body: {
    "receiver_account": "408298765432111",
    "amount": 5000,
    "description": "Groceries"
  }

Backend (app/routes/transactions.py):
  ↓ Verifies JWT: user_id = "uuid-123"
  
  ↓ Validates inputs:
    - Is 5000 a positive number? ✓
    - Is account number 8-16 digits? ✓
  
  ↓ Query Alex's account:
    SELECT balance FROM accounts 
    WHERE user_id = "uuid-123"
    Result: 100000.0
  
  ↓ Check balance: 100000 >= 5000? ✓
  
  ↓ Find receiver:
    SELECT * FROM accounts 
    WHERE account_number = "408298765432111"
    Result: Found! Let's call them receiver_id = "uuid-456"
  
  ↓ Start database transaction (all-or-nothing):
    UPDATE accounts SET balance = 95000 
    WHERE user_id = "uuid-123"
    
    UPDATE accounts SET balance = (old_balance + 5000)
    WHERE account_number = "408298765432111"
    
    INSERT INTO transactions:
      account_id: "acct-uuid"
      sender_id: "uuid-123"
      receiver_id: "uuid-456"
      type: "transfer"
      amount: 5000
      status: "completed"
      completed_at: 2026-02-18 15:30:45
  
  ↓ COMMIT (if no errors)

Backend returns JSON:
  {
    "success": true,
    "message": "Transfer completed successfully",
    "new_balance": 95000,
    "transaction": {
      "id": "txn-uuid",
      "amount": 5000,
      "receiver_name": "Jane Smith",
      "completed_at": "2026-02-18T15:30:45"
    }
  }

Frontend receives and updates UI:
  ✓ Shows: "Transfer completed successfully"
  Dashboard updates: Balance now shows ₹95,000
  Clear form fields for next transfer


┌────────────────────────────────────────────────────────────────┐
│ STEP 4: USER VIEWS TRANSACTION HISTORY                         │
└────────────────────────────────────────────────────────────────┘

Front end (Transactions.tsx):
  ↓ User navigates to "Transactions" page
  ↓ useEffect calls transactionAPI.getTransactions()

Axios sends GET with JWT:
  URL: http://localhost:5000/api/transactions
  Headers: Authorization: Bearer eyJhbGci...

Backend (app/routes/transactions.py):
  ↓ Verifies JWT: user_id = "uuid-123"
  
  ↓ Get user's account:
    SELECT id FROM accounts WHERE user_id = "uuid-123"
    Result: account_id = "acct-uuid"
  
  ↓ Get all transactions for this account:
    SELECT * FROM transactions 
    WHERE account_id = "acct-uuid"
    ORDER BY created_at DESC
    
    Returns:
    [
      {
        id: "txn-uuid",
        type: "transfer",
        amount: 5000,
        receiver_name: "Jane Smith",
        status: "completed",
        completed_at: "2026-02-18T15:30:45"
      },
      ... (other transactions) ...
    ]

Backend returns JSON:
  {
    "success": true,
    "transactions": [... (array of transactions) ...]
  }

Frontend displays in table:
  ┌──────────────────────────────────────────────────┐
  │ TRANSACTION HISTORY                              │
  ├──────────┬───────────┬────────┬──────────────────┤
  │ Date     │ Type      │ Amount │ Receiver         │
  ├──────────┼───────────┼────────┼──────────────────┤
  │ Feb 18   │ Transfer  │ ₹5000  │ Jane Smith       │
  │ Feb 17   │ UPI       │ ₹2000  │ Amazon Pay       │
  │ Feb 16   │ NEFT      │ ₹1200  │ Electric Co.     │
  └──────────┴───────────┴────────┴──────────────────┘
```

---

## Architecture Pattern

### What Pattern is This?

This project uses a **REST API + MVVM** architecture:

#### **REST API** (Backend)
- **Stateless**: No session stored on server
- **HTTP verbs**: GET (read), POST (create), PUT (update), DELETE (remove)
- **JSON communication**: All responses are JSON

```
GET  /api/account          → Retrieve account
POST /api/transactions/*   → Create transaction
PUT  /api/user/profile     → Update profile
```

#### **MVVM** (Frontend)
- **Model**: Data (user, balance, transactions)
- **View**: UI (React components - Login, Dashboard, etc.)
- **ViewModel**: Context API + Hooks (state management)

### Architecture Diagram:

```
┌─────────────────────────────────────────────────────────────┐
│                     FRONTEND (React)                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ VIEW LAYER                                             │ │
│  │ ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │ │
│  │ │ Login.tsx    │  │ Dashboard    │  │ Payments.tsx │  │ │
│  │ └──────────────┘  └──────────────┘  └──────────────┘  │ │
│  │        ↓                 ↓                  ↓           │ │
│  │ ┌────────────────────────────────────────────────────┐ │ │
│  │ │ VIEWMODEL LAYER                                    │ │ │
│  │ │ BankingContext.tsx (State Management)              │ │ │
│  │ │ - isAuthenticated, user, balance, transactions     │ │ │
│  │ │ - useAuth, useBalance, useTransactions hooks       │ │ │
│  │ └────────────────────────────────────────────────────┘ │ │
│  │        ↓                                                 │ │
│  │ ┌────────────────────────────────────────────────────┐ │ │
│  │ │ SERVICE LAYER                                      │ │ │
│  │ │ api.ts (Axios HTTP Client)                         │ │ │
│  │ │ - authAPI, userAPI, transactionAPI, etc.           │ │ │
│  │ └────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                    HTTP + JWT Token
            (Axios Interceptor adds token)
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    BACKEND (Flask)                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ROUTE LAYER (Endpoints)                                │ │
│  │ /api/register, /api/login, /api/transactions/*, etc.   │ │
│  │ @jwt_required() decorator verifies token               │ │
│  └────────────────────────────────────────────────────────┘ │
│                    ↓                                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ BUSINESS LOGIC LAYER                                   │ │
│  │ - Validate inputs (email, password, amounts)           │ │
│  │ - Check balance, verify passwords                      │ │
│  │ - Process transfers (deduct/add balances)              │ │
│  │ - Generate JWT tokens                                  │ │
│  └────────────────────────────────────────────────────────┘ │
│                    ↓                                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ MODEL LAYER (SQLAlchemy ORM)                           │ │
│  │ User, Account, Transaction, CreditCard models          │ │
│  │ Models convert Python → SQL automatically              │ │
│  └────────────────────────────────────────────────────────┘ │
│                    ↓                                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ DATABASE LAYER                                         │ │
│  │ SQLite (development) / MySQL (production)              │ │
│  │ 6 tables: users, accounts, transactions, etc.          │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Why This Architecture is Good:

✅ **Separation of Concerns**: Frontend and backend are independent
✅ **Scalability**: Can replace SQLite with MySQL without changing code
✅ **Testability**: Can test backend API without frontend
✅ **Reusability**: API can serve mobile app, desktop app, etc.
✅ **Security**: Passwords never cross network (JWT tokens instead)

---

## Production Improvements

### Current State (Development)
```
✓ Works locally
✗ Hard-coded URLs
✗ SQLite (not suitable for multiple users)
✗ No HTTPS
✗ No input validation on some fields
✗ No rate limiting
✗ No audit logging
```

### 1. Database Improvements

**Current**: SQLite (file-based, single-user)
```python
SQLALCHEMY_DATABASE_URI = 'sqlite:///banking.db'
```

**Production**: MySQL or PostgreSQL
```python
SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://user:pass@db.example.com:3306/sterling_bank'

# OR PostgreSQL
SQLALCHEMY_DATABASE_URI = 'postgresql://user:pass@db.example.com:5432/sterling_bank'
```

**Benefits**: Multi-user, backups, scaling, transactions, automatic failover

**Additional DB improvements**:
```python
# Add database migrations (Alembic) for schema changes
# Add read replicas for reports (separate from writes)
# Add database-level encryption
# Add automated backups and point-in-time recovery
# Add connection pooling (QueuePool in SQLAlchemy)
```

### 2. Security Improvements

**SSL/TLS (HTTPS)**
```python
# Production config
if app.env == 'production':
    # Enforce HTTPS
    app.config['PREFERRD_URL_SCHEME'] = 'https'
    
    # Only secure cookies
    app.config['SESSION_COOKIE_SECURE'] = True
    app.config['SESSION_COOKIE_HTTPONLY'] = True
    app.config['SESSION_COOKIE_SAMESITE'] = 'Strict'
```

**Password Hashing** (already done, but verify)
```python
# Good: Uses Werkzeug's generate_password_hash
user.set_password(password)  # Automatically hashed

# Bad: Would be storing plain-text
password_hash = password  # NEVER DO THIS
```

**Rate Limiting** (prevent brute force)
```python
from flask_limiter import Limiter

limiter = Limiter(app, key_func=lambda: request.remote_addr)

@auth_bp.route('/login', methods=['POST'])
@limiter.limit("5 per minute")  # Max 5 login attempts/minute
def login():
    ...
```

**Input Validation** (already done, but add more)
```python
from marshmallow import Schema, fields, validate

class TransferSchema(Schema):
    receiver_account = fields.Str(
        required=True,
        validate=validate.Length(min=8, max=16)
    )
    amount = fields.Float(
        required=True,
        validate=validate.Range(min=1, max=1000000)
    )
```

**SQL Injection Protection** (already protected by ORM)
```python
# Safe (SQLAlchemy auto-escapes):
Account.query.filter_by(account_number=account_number).first()

# Dangerous (raw SQL - never do this):
db.session.execute(f"SELECT * FROM accounts WHERE account_number = '{account_number}'")
```

### 3. Environment Configuration

**Current** (hard-coded):
```python
CORS_ORIGINS = ["http://localhost:8083"]
API_BASE_URL = "http://localhost:5000/api"
```

**Production** (use environment variables):
```python
# backend/.env
FLASK_ENV=production
FLASK_APP=run.py
DATABASE_URL=mysql+pymysql://user:pass@db.example.com/sterling_bank
SECRET_KEY=your-super-secret-key-change-this
JWT_SECRET_KEY=your-jwt-secret-Key-change-this
CORS_ORIGINS=https://yourbank.com,https://www.yourbank.com
ALLOWED_HOSTS=yourbank.com,www.yourbank.com

# frontend/.env
VITE_API_BASE_URL=https://api.yourbank.com/api
```

Load using `python-dotenv` (already installed):
```python
import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv('DATABASE_URL')
SECRET_KEY = os.getenv('SECRET_KEY')
```

### 4. Logging & Monitoring

**Add structured logging**:
```python
import logging

logging.basicConfig(
    filename='app.log',
    level=logging.INFO,
    format='%(asctime)s %(levelname)s: %(message)s'
)

@auth_bp.route('/login', methods=['POST'])
def login():
    try:
        user = User.query.filter_by(email=email).first()
        if user:
            logging.info(f"Login attempt for {email}")
            if user.check_password(password):
                logging.info(f"Login successful for {email}")
                ...
            else:
                logging.warning(f"Failed login attempt for {email}")
```

**Monitor transactions**:
```python
# Log all transfers for audit
logging.info(f"Transfer: {sender_id} → {receiver_id}, ₹{amount}")

# Track failed transfers
logging.error(f"Transfer failed: {sender_id}, error: {str(e)}")
```

### 5. API Documentation

**Add OpenAPI/Swagger**:
```python
from flasgger import Swagger

swagger = Swagger(app)

@auth_bp.route('/login', methods=['POST'])
def login():
    """
    User Login
    ---
    parameters:
      - name: email
        in: body
        type: string
        required: true
      - name: password
        in: body
        type: string
        required: true
    responses:
      200:
        description: Login successful
        schema:
          properties:
            success:
              type: boolean
            access_token:
              type: string
            user_id:
              type: string
    """
    ...
```

### 6. Deployment

**Backend Deployment**:
```bash
# Install production server (not Flask dev server)
pip install gunicorn  # WSGI server for production

# Run with Gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 "app:create_app()"
# -w 4: 4 worker processes for concurrency
# -b 0.0.0.0:5000: Listen on all interfaces, port 5000
```

**Frontend Deployment**:
```bash
# Build for production (minified, optimized)
npm run build

# Output in 'dist/' folder
# Serve with Nginx or any static file server
```

**Infrastructure**:
```
┌────────────────────────────────────────┐
│  Nginx (Reverse Proxy)                 │
│  - Routes /api/* to Flask              │
│  - Serves static files (React build)   │
│  - Handles HTTPS/SSL                   │
└──────────────────┬─────────────────────┘
         ↙         ↓         ↖
   ┌───────────────────────────────────┐
   │  Flask Backend                    │  ← 4 Gunicorn workers
   │  (Stateless, can scale)           │
   └──────────┬──────────────────┬─────┘
              ↓                  ↓
   ┌──────────────────────────────────┐
   │  MySQL Database                  │  ← Replicated for HA
   │  (Persistent storage)            │
   └──────────────────────────────────┘
```

### 7. Testing

**Add unit tests**:
```python
import unittest
from app import create_app, db

class TestAuth(unittest.TestCase):
    def setUp(self):
        self.app = create_app('testing')
        self.client = self.app.test_client()
        
    def test_register(self):
        response = self.client.post('/api/register', json={
            'name': 'Test User',
            'email': 'test@example.com',
            'password': 'Password123'
        })
        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.json['success'])
```

**Test coverage**:
```bash
pip install pytest pytest-cov
pytest --cov=app tests/
```

### 8. Improve Frontend

**TypeScript strict mode**:
```json
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "noImplicitThis": true,
    "strictNullChecks": true
  }
}
```

**Add error boundaries**:
```tsx
class ErrorBoundary extends React.Component {
  componentDidCatch(error, errorInfo) {
    logErrorToBackend(error);
  }
  render() {
    if (this.state.hasError) {
      return <ErrorPage />;
    }
    return this.props.children;
  }
}
```

**PWA (Progressive Web App)**:
```bash
npm install @vitejs/plugin-pwa
# Then users can install as app on mobile
```

### 9. 2FA (Two-Factor Authentication)

```python
import pyotp

# Generate QR code for authenticator app
@users_bp.route('/setup-2fa', methods=['POST'])
def setup_2fa():
    secret = pyotp.random_base32()
    totp = pyotp.TOTP(secret)
    qr_code = totp.provisioning_uri(name=user.email)
    # Return QR code to user
    
@auth_bp.route('/login', methods=['POST'])
def login():
    # ... verify password ...
    
    if user.has_2fa:
        return {
            'success': True,
            'requires_2fa': True,
            'temp_token': temp_token  # Valid for 5 minutes
        }
```

### 10. Checklist for Production Readiness

```
Database
  ☐ Migrate from SQLite to MySQL/PostgreSQL
  ☐ Set up automated backups
  ☐ Enable encryption at rest
  ☐ Set up read replicas
  
Security
  ☐ Enable HTTPS/SSL everywhere
  ☐ Add rate limiting
  ☐ Implement CSRF protection
  ☐ Add 2FA
  ☐ Implement API key-based access
  ☐ Run security audit
  
Logging & Monitoring
  ☐ Set up structured logging
  ☐ Add error tracking (Sentry)
  ☐ Add performance monitoring (New Relic)
  ☐ Set up alerts
  
Deployment
  ☐ Use Gunicorn instead of Flask dev server
  ☐ Set up CI/CD pipeline (GitHub Actions)
  ☐ Containerize with Docker
  ☐ Use orchestration (Kubernetes optional)
  ☐ Set up staging environment
  
Frontend
  ☐ Build optimization (gzip, minify)
  ☐ CDN for static files
  ☐ Service workers for offline support
  ☐ Error tracking (Sentry)
  
Testing
  ☐ Add unit tests (90%+ coverage)
  ☐ Add integration tests
  ☐ Add E2E tests (Selenium/Playwright)
  ☐ Load testing (k6, Apache JMeter)
  
Documentation
  ☐ API documentation (Swagger/OpenAPI)
  ☐ Setup guide for new developers
  ☐ Runbook for production issues
  ☐ Disaster recovery plan
```

---

## Summary

**Sterling Bank Portal** is a modern full-stack banking application using:
- **Frontend**: React + TypeScript with Vite for fast development
- **Backend**: Flask REST API with JWT authentication
- **Database**: SQLAlchemy ORM with SQLite (dev) / MySQL (production)
- **Communication**: Axios HTTP client + CORS for safe cross-origin requests
- **Architecture**: Stateless REST API + context-based state management

The application successfully demonstrates:
✓ Secure authentication with JWT tokens
✓ Multi-method payment systems (6 payment types)
✓ Transaction history and auditability
✓ Clean separation between frontend and backend
✓ Professional UI with Tailwind + shadcn components

For production, add HTTPS, MySQL database, rate limiting, monitoring, and automated testing.

