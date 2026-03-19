import React, { ReactNode, Suspense, lazy } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { BankingProvider } from "@/contexts/BankingContext";
import BankingLayout from "@/components/BankingLayout";

const Index = lazy(() => import("./pages/Index"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Payments = lazy(() => import("./pages/Payments"));
const Transactions = lazy(() => import("./pages/Transactions"));
const CreditCardPage = lazy(() => import("./pages/CreditCardPage"));
const Profile = lazy(() => import("./pages/Profile"));
const NotificationsPage = lazy(() => import("./pages/Notifications"));
const AdminPage = lazy(() => import("./pages/Admin"));
const SavingsGoalsPage = lazy(() => import("./pages/SavingsGoals"));
const AssistantPage = lazy(() => import("./pages/Assistant"));
const NotFound = lazy(() => import("./pages/NotFound"));

const queryClient = new QueryClient();

// Error Boundary Component
class AppErrorBoundary extends React.Component<{ children: ReactNode }, { hasError: boolean; error: Error | null }> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error) {
    console.error("App Error:", error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: "40px", color: "white", background: "#1a1a1a", minHeight: "100vh" }}>
          <h2>Something went wrong</h2>
          <pre style={{ color: "red", marginTop: "20px" }}>{this.state.error?.message}</pre>
          <details style={{ marginTop: "20px", color: "#aaa" }}>
            <summary>Stack trace</summary>
            <pre>{this.state.error?.stack}</pre>
          </details>
        </div>
      );
    }

    return this.props.children;
  }
}

const PageFallback = () => (
  <div className="min-h-screen flex items-center justify-center bg-background text-muted-foreground">
    Loading page...
  </div>
);

const App = () => (
  <AppErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BankingProvider>
          <Suspense fallback={<PageFallback />}>
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<Index />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route element={<BankingLayout />}>
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/payments" element={<Payments />} />
                  <Route path="/transactions" element={<Transactions />} />
                  <Route path="/credit-card" element={<CreditCardPage />} />
                  <Route path="/notifications" element={<NotificationsPage />} />
                  <Route path="/assistant" element={<AssistantPage />} />
                  <Route path="/admin" element={<AdminPage />} />
                  <Route path="/goals" element={<SavingsGoalsPage />} />
                  <Route path="/profile" element={<Profile />} />
                </Route>
                <Route path="*" element={<NotFound />} />
              </Routes>
            </BrowserRouter>
          </Suspense>
        </BankingProvider>
      </TooltipProvider>
    </QueryClientProvider>
  </AppErrorBoundary>
);

export default App;
