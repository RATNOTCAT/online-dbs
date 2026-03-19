import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Bot, SendHorizonal, Sparkles, ShieldAlert, Trash2 } from 'lucide-react';
import { chatAPI } from '@/services/api';

type ChatMessage = {
  role: 'user' | 'assistant';
  content: string;
  suggested_route?: string | null;
  provider?: string;
  warning?: string | null;
};

const starterPrompts = [
  'Show my current balance summary',
  'Explain the difference between IMPS, NEFT and RTGS',
  'How do savings goals work in this app?',
  'What should I check if a transfer fails?',
  'Summarize my recent account activity',
];

const AssistantPage = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content: 'I am your Vibe Bank assistant. Ask about balances, transactions, transfers, beneficiaries, goals, notifications, credit card details, or banking terms related to this app.',
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [statusMode, setStatusMode] = useState<'checking' | 'ready'>('checking');
  const [insights, setInsights] = useState<string[]>([]);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        await chatAPI.getStatus();
        const insightsRes = await chatAPI.getInsights();
        setInsights(insightsRes.data.insights || []);
        const historyRes = await chatAPI.getHistory();
        const historyMessages = (historyRes.data.messages || []).map((item: any) => ({
          role: item.role,
          content: item.content,
          suggested_route: item.suggested_route,
          provider: item.provider,
        }));
        if (historyMessages.length > 0) {
          setMessages(historyMessages);
        }
      } finally {
        setStatusMode('ready');
      }
    };
    bootstrap();
  }, []);

  const history = useMemo(
    () => messages
      .filter((message) => message.role === 'user' || message.role === 'assistant')
      .slice(-8)
      .map((message) => ({ role: message.role, content: message.content })),
    [messages]
  );

  const sendMessage = async (text?: string) => {
    const content = (text ?? input).trim();
    if (!content || loading) {
      return;
    }

    const nextUserMessage: ChatMessage = { role: 'user', content };
    const nextHistory = [...history, { role: 'user', content }];
    setMessages((prev) => [...prev, nextUserMessage]);
    setInput('');
    setLoading(true);

    try {
      const response = await chatAPI.sendMessage({ message: content, history: nextHistory.slice(-8) });
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: response.data.message,
          suggested_route: response.data.suggested_route,
          provider: response.data.provider,
          warning: response.data.warning,
        },
      ]);
    } catch (error: any) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: error?.response?.data?.message || 'The assistant could not respond right now. Please try again.',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const clearHistory = async () => {
    await chatAPI.clearHistory();
    setMessages([
      {
        role: 'assistant',
        content: 'Assistant history cleared. Ask about balances, transactions, transfers, beneficiaries, goals, notifications, credit card details, or banking terms related to this app.',
      },
    ]);
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Vibe Bank Assistant</h1>
          <p className="text-muted-foreground mt-1">An app-focused chatbot for banking help, your account context, and guided navigation.</p>
        </div>
        <div className="flex items-center gap-2">
          <button type="button" onClick={clearHistory} className="rounded-full border border-border bg-secondary px-4 py-2 text-sm text-muted-foreground flex items-center gap-2">
            <Trash2 className="w-4 h-4" />
            Clear history
          </button>
          <div className="flex items-center gap-2 rounded-full border border-border bg-secondary px-4 py-2 text-sm text-muted-foreground">
            <Sparkles className="w-4 h-4 text-primary" />
            {statusMode === 'checking' ? 'Checking assistant...' : 'Assistant ready'}
          </div>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 xl:grid-cols-[280px,1fr] gap-6">
        <aside className="banking-card space-y-4">
          <div>
            <h2 className="text-lg font-semibold text-foreground">Try asking</h2>
            <p className="text-sm text-muted-foreground mt-1">These prompts are scoped to the app and banking workflow.</p>
          </div>
          <div className="space-y-2">
            {starterPrompts.map((prompt) => (
              <button
                key={prompt}
                type="button"
                onClick={() => sendMessage(prompt)}
                className="w-full text-left rounded-xl border border-border px-3 py-3 text-sm text-foreground hover:bg-secondary transition-colors"
              >
                {prompt}
              </button>
            ))}
          </div>
          <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3">
            <div className="flex items-center gap-2 text-amber-400">
              <ShieldAlert className="w-4 h-4" />
              <span className="text-sm font-medium">Scope</span>
            </div>
            <p className="text-xs text-muted-foreground mt-2">
              This assistant is meant for Vibe Bank usage, banking concepts, and your app data. It should not be used as a general chatbot or for investment advice.
            </p>
          </div>
          <div className="rounded-xl border border-border p-3">
            <h3 className="text-sm font-semibold text-foreground">Quick Insights</h3>
            <div className="space-y-2 mt-3">
              {insights.length === 0 && <p className="text-xs text-muted-foreground">Insights will appear once your account data is loaded.</p>}
              {insights.map((item, index) => (
                <p key={index} className="text-xs text-muted-foreground leading-5">{item}</p>
              ))}
            </div>
          </div>
        </aside>

        <section className="banking-card flex flex-col min-h-[640px]">
          <div className="flex-1 space-y-4 overflow-y-auto pr-1">
            {messages.map((message, index) => (
              <div
                key={`${message.role}-${index}`}
                className={`rounded-2xl px-4 py-3 max-w-[85%] ${
                  message.role === 'user'
                    ? 'ml-auto bg-primary text-primary-foreground'
                    : 'bg-secondary text-foreground'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  {message.role === 'assistant' && <Bot className="w-4 h-4 text-primary" />}
                  <span className="text-xs uppercase tracking-wide opacity-70">{message.role}</span>
                  {message.provider && message.role === 'assistant' && (
                    <span className="text-[10px] px-2 py-1 rounded-full bg-background/60 text-muted-foreground">
                      {message.provider}
                    </span>
                  )}
                </div>
                <p className="text-sm whitespace-pre-wrap leading-6">{message.content}</p>
                {message.warning && (
                  <p className="text-xs text-amber-400 mt-2">{message.warning}</p>
                )}
                {message.suggested_route && message.role === 'assistant' && (
                  <div className="mt-3">
                    <Link to={message.suggested_route} className="text-xs text-primary hover:underline">
                      Open {message.suggested_route}
                    </Link>
                  </div>
                )}
              </div>
            ))}
            {loading && (
              <div className="rounded-2xl px-4 py-3 max-w-[85%] bg-secondary text-foreground">
                <div className="flex items-center gap-2 mb-2">
                  <Bot className="w-4 h-4 text-primary" />
                  <span className="text-xs uppercase tracking-wide opacity-70">assistant</span>
                </div>
                <p className="text-sm text-muted-foreground">Thinking...</p>
              </div>
            )}
          </div>

          <div className="mt-6 border-t border-border pt-4">
            <div className="flex gap-3">
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendMessage();
                  }
                }}
                placeholder="Ask about balances, payments, goals, alerts, statements, or banking concepts..."
                className="flex-1 min-h-24 resize-none rounded-xl border border-border bg-secondary px-4 py-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
              />
              <button
                type="button"
                onClick={() => sendMessage()}
                disabled={loading || !input.trim()}
                className="banking-button-primary self-end disabled:opacity-60"
              >
                <SendHorizonal className="w-4 h-4" />
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default AssistantPage;
