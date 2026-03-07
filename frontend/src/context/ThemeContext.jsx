import React, { createContext, useContext, useState } from 'react';

export const THEMES = {
  dark: {
    name: 'dark',
    bgPrimary: '#1a1a2e',
    bgPanel: '#1e1e32',
    bgNav: '#16162a',
    bgNavBorder: 'rgba(255,122,61,0.10)',
    bgNode: 'rgba(30, 30, 50, 0.95)',
    bgNodeHover: 'rgba(255,122,61,0.08)',
    bgNodeSelected: 'rgba(255,122,61,0.14)',
    bgCanvas: '#16162a',
    bgDot: '#2a2a44',
    text: '#e2e8f0',
    textMuted: '#94a3b8',
    textAccent: '#FFa06a',
    textBrand: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    navBtnActiveBg: 'rgba(255,122,61,0.15)',
    navBtnActiveBorder: 'rgba(255,122,61,0.4)',
    navBtnActiveColor: '#FFa06a',
    navBtnInactiveBg: 'rgba(255,255,255,0.04)',
    navBtnInactiveBorder: 'rgba(255,255,255,0.08)',
    navBtnInactiveColor: '#475569',
    divider: 'rgba(255,122,61,0.12)',
    dividerHover: 'rgba(255,122,61,0.4)',
    border: 'rgba(255,255,255,0.08)',
    borderAccent: 'rgba(255,122,61,0.25)',
    panelBorder: 'rgba(255,255,255,0.06)',
    shadow: '0 4px 16px rgba(0,0,0,0.35)',
    shadowNode: '0 2px 12px rgba(0,0,0,0.3)',
    logoShadow: '0 0 16px rgba(255,122,61,0.3)',
    toggleBg: 'rgba(255,255,255,0.06)',
    toggleBorder: 'rgba(255,255,255,0.08)',
    toggleColor: '#94a3b8',
    // Panel-specific tokens
    panelContainerBg: '#1e1e32',
    panelHeaderBg: 'rgba(255,255,255,0.03)',
    panelHeaderBorder: 'rgba(255,255,255,0.06)',
    collapseBtnBg: 'rgba(255,255,255,0.06)',
    collapseBtnHover: 'rgba(255,122,61,0.15)',
    collapseBtnColor: '#94a3b8',
    accent: '#FF7A3D',
    accentLight: '#FFa06a',
    accentGradient: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    userBubbleBg: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    aiBubbleBg: 'rgba(30, 30, 50, 0.95)',
    inputBg: 'rgba(255,255,255,0.05)',
    sendBtnActive: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    sendBtnInactive: 'rgba(255,255,255,0.08)',
  },
  light: {
    name: 'light',
    bgPrimary: '#F8F9FA',
    bgPanel: '#ffffff',
    bgNav: 'rgba(248,249,250,0.95)',
    bgNavBorder: 'rgba(0,0,0,0.06)',
    bgNode: 'rgba(255,255,255,0.98)',
    bgNodeHover: 'rgba(255,122,61,0.05)',
    bgNodeSelected: 'rgba(255,122,61,0.09)',
    bgCanvas: '#F0F1F3',
    bgDot: '#d1d5db',
    text: '#111827',
    textMuted: '#6b7280',
    textAccent: '#FF7A3D',
    textBrand: '#111827',
    navBtnActiveBg: 'rgba(255,122,61,0.10)',
    navBtnActiveBorder: 'rgba(255,122,61,0.3)',
    navBtnActiveColor: '#FF7A3D',
    navBtnInactiveBg: 'rgba(0,0,0,0.03)',
    navBtnInactiveBorder: 'rgba(0,0,0,0.08)',
    navBtnInactiveColor: '#94a3b8',
    divider: 'rgba(0,0,0,0.08)',
    dividerHover: 'rgba(255,122,61,0.3)',
    border: 'rgba(0,0,0,0.08)',
    borderAccent: 'rgba(255,122,61,0.25)',
    panelBorder: 'rgba(0,0,0,0.06)',
    shadow: '0 4px 16px rgba(0,0,0,0.06)',
    shadowNode: '0 2px 10px rgba(0,0,0,0.06)',
    logoShadow: '0 0 12px rgba(255,122,61,0.2)',
    toggleBg: 'rgba(0,0,0,0.04)',
    toggleBorder: 'rgba(0,0,0,0.08)',
    toggleColor: '#475569',
    // Panel-specific tokens
    panelContainerBg: '#ffffff',
    panelHeaderBg: '#F8F9FA',
    panelHeaderBorder: 'rgba(0,0,0,0.06)',
    collapseBtnBg: 'rgba(0,0,0,0.04)',
    collapseBtnHover: 'rgba(255,122,61,0.10)',
    collapseBtnColor: '#6b7280',
    accent: '#FF7A3D',
    accentLight: '#FF9A62',
    accentGradient: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    userBubbleBg: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    aiBubbleBg: '#F8F9FA',
    inputBg: 'rgba(0,0,0,0.03)',
    sendBtnActive: 'linear-gradient(135deg, #FF7A3D, #FF9A62)',
    sendBtnInactive: 'rgba(0,0,0,0.06)',
  },
};

export const ThemeContext = createContext({
  themeName: 'dark',
  t: THEMES.dark,
  toggle: () => { },
});

export const ThemeProvider = ({ children }) => {
  const [themeName, setThemeName] = useState('dark');
  const toggle = () => setThemeName(n => (n === 'dark' ? 'light' : 'dark'));
  return (
    <ThemeContext.Provider value={{ themeName, t: THEMES[themeName], toggle }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
