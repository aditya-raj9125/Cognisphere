import React, { useEffect } from 'react';

export const Toast = ({ message, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 3000);

    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div
      style={{
        position: 'fixed',
        top: '20px',
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: 'rgba(15, 20, 32, 0.95)',
        color: '#e2e8f0',
        padding: '12px 24px',
        borderRadius: '10px',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
        border: '1px solid rgba(99, 102, 241, 0.2)',
        backdropFilter: 'blur(16px)',
        zIndex: 9999,
        fontFamily: 'Inter, sans-serif',
        animation: 'slideIn 0.5s'
      }}
    >
      {message}
    </div>
  );
}; 