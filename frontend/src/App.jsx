import React, { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';
import { LandingPage } from './pages/LandingPage';
import { DashboardPage } from './pages/DashboardPage';

function App() {
  useEffect(() => {
    if (!sessionStorage.getItem('sessionId')) {
      sessionStorage.setItem('sessionId', uuidv4());
    }
  }, []);

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;