import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";

try {
  const root = createRoot(document.getElementById("root")!);
  root.render(<App />);
} catch (error) {
  console.error("Failed to render app:", error);
  document.body.innerHTML = `<div style="color: red; padding: 20px; font-family: monospace;">
    <h2>Error Loading App</h2>
    <pre>${error instanceof Error ? error.message : String(error)}</pre>
  </div>`;
}
