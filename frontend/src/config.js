// If VITE_API_URL was set at build time, use it.
// Otherwise, assume the backend is on the same host at port 8080.
const resolveBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  // Local dev (npm run dev) — backend runs on 8081
  if (window.location.port === '5173' || window.location.port === '5174') {
    return 'http://localhost:8081';
  }
  // Docker / EC2 — backend on same host, port 8080
  return `${window.location.protocol}//${window.location.hostname}:8080`;
};

export const config = {
  BASE_URL: resolveBaseUrl(),
};