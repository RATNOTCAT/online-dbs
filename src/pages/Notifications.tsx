import { useEffect, useState } from 'react';
import { Bell, CheckCircle2 } from 'lucide-react';
import { motion } from 'framer-motion';
import { notificationAPI } from '@/services/api';

const NotificationsPage = () => {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const loadNotifications = async () => {
    const res = await notificationAPI.getNotifications();
    setNotifications(res.data.notifications || []);
    setUnreadCount(res.data.unread_count || 0);
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const markRead = async (notificationId: string) => {
    await notificationAPI.markRead(notificationId);
    await loadNotifications();
  };

  const markAllRead = async () => {
    await notificationAPI.markAllRead();
    await loadNotifications();
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Notifications</h1>
          <p className="text-muted-foreground mt-1">Track account activity, security alerts, and transfer updates.</p>
        </div>
        <button
          type="button"
          onClick={markAllRead}
          className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-medium"
        >
          Mark all read
        </button>
      </motion.div>

      <div className="banking-card">
        <p className="text-sm text-muted-foreground mb-4">Unread: <span className="text-foreground font-semibold">{unreadCount}</span></p>
        <div className="space-y-3">
          {notifications.length === 0 && <p className="text-sm text-muted-foreground">No notifications yet.</p>}
          {notifications.map((item) => (
            <div key={item.id} className={`rounded-xl border p-4 ${item.is_read ? 'border-border bg-secondary/20' : 'border-primary/30 bg-primary/5'}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                    <Bell className="w-4 h-4 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{item.title}</p>
                    <p className="text-sm text-muted-foreground mt-1">{item.message}</p>
                    <p className="text-xs text-muted-foreground mt-2">{new Date(item.created_at).toLocaleString()}</p>
                  </div>
                </div>
                {!item.is_read && (
                  <button type="button" onClick={() => markRead(item.id)} className="text-sm text-primary font-medium">
                    Mark read
                  </button>
                )}
                {item.is_read && <CheckCircle2 className="w-4 h-4 text-success mt-1" />}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default NotificationsPage;
