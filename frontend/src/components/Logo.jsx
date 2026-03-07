import React from 'react';

export const Logo = () => {
  return (
    <div style={{
      width: '100%',
      height: '60px',
      backgroundColor: '#e3f6f5',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
      top: 0,
      zIndex: 10,
      padding: '6px 20px',
      position: 'sticky',
      boxSizing: 'border-box',
    }}>
      {/* Left: App Name */}
      <span style={{
        fontFamily: "'Segoe UI', 'Inter', sans-serif",
        fontSize: '26px',
        fontWeight: 700,
        color: '#272343',
        letterSpacing: '1px',
        userSelect: 'none',
      }}>
        CogniSphere
      </span>

      {/* Right: Sign In Button */}
      <button
        style={{
          padding: '8px 22px',
          fontSize: '15px',
          fontWeight: 600,
          color: '#fff',
          backgroundColor: '#272343',
          border: 'none',
          borderRadius: '8px',
          cursor: 'pointer',
          letterSpacing: '0.5px',
          transition: 'background-color 0.2s ease',
        }}
        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#3a3563'}
        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#272343'}
        onClick={() => { /* TODO: implement sign-in logic */ }}
      >
        Sign In
      </button>
    </div>
  );
};