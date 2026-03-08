import React, { useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Flow } from '../components/Flow/Flow';
import { ChatDialog } from '../components/ChatDialog';
import {
  FiMessageSquare, FiGrid, FiSun, FiMoon,
  FiChevronLeft, FiChevronRight,
} from 'react-icons/fi';
import { ThemeProvider, useTheme } from '../context/ThemeContext';

/* ─── Layout constants ─── */
const NAVBAR_H = 60;
const PANEL_RADIUS = 16;
const CONTENT_PAD = 10;
const DIVIDER_W = 10;
const MIN_PCT = 20;  // minimum graph panel %
const MAX_PCT = 82;  // maximum graph panel %

/* ─── Spring config — feels like a window snapping into place ─── */
const SPRING = { type: 'spring', stiffness: 320, damping: 32, mass: 0.75 };
/* Fade for content inside panels */
const FADE = { type: 'spring', stiffness: 280, damping: 30, mass: 0.6 };

/* ─────────────────────────────────────────────────────────────── */
/*  Dashboard inner                                               */
/* ─────────────────────────────────────────────────────────────── */
const Dashboard = () => {
  const { t, themeName, toggle } = useTheme();
  const navigate = useNavigate();

  const [activePanel, setActivePanel] = useState('both');
  const [splitPct, setSplitPct] = useState(75);   // graph % — default 75/25 split

  const isDragging = useRef(false);
  const containerRef = useRef(null);

  const graphVisible = activePanel !== 'chat';
  const chatVisible = activePanel !== 'graph';

  /* ── Collapse helpers ─────────────────── */
  const collapseGraph = () =>
    setActivePanel(p => (p === 'chat' ? 'both' : 'chat'));
  const collapseChat = () =>
    setActivePanel(p => (p === 'graph' ? 'both' : 'graph'));

  /* ── Flex-basis values (used by framer layout animation) ─────── */
  const graphFlex = activePanel === 'both' ? splitPct
    : activePanel === 'graph' ? 100
      : 0;
  const chatFlex = activePanel === 'both' ? 100 - splitPct
    : activePanel === 'chat' ? 100
      : 0;

  /* ── Drag-to-resize divider ──────────────────────────────────── */
  const onDividerMouseDown = useCallback((e) => {
    e.preventDefault();
    isDragging.current = true;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    const onMove = (ev) => {
      if (!isDragging.current || !containerRef.current) return;
      const r = containerRef.current.getBoundingClientRect();
      const raw = ((ev.clientX - r.left) / r.width) * 100;
      setSplitPct(Math.max(MIN_PCT, Math.min(MAX_PCT, raw)));
    };
    const onUp = () => {
      isDragging.current = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  }, []);

  return (
    <div style={{
      display: 'flex', flexDirection: 'column',
      width: '100%', height: '100%', overflow: 'hidden',
      background: t.bgPrimary,
      fontFamily: "'Inter','Segoe UI',system-ui,sans-serif",
    }}>

      {/* ═══ NAVBAR ═══ */}
      <nav style={{
        height: NAVBAR_H,
        background: t.bgNav,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 24px',
        borderBottom: `1px solid ${t.bgNavBorder}`,
        flexShrink: 0, zIndex: 50,
        backdropFilter: 'blur(12px)',
      }}>
        <span
          onClick={() => navigate('/')}
          style={{
            fontWeight: 700, fontSize: 20, letterSpacing: -0.5,
            color: t.text, cursor: 'pointer', transition: 'opacity 0.2s',
          }}
          onMouseEnter={e => e.currentTarget.style.opacity = '0.65'}
          onMouseLeave={e => e.currentTarget.style.opacity = '1'}
        >
          CogniSphere
        </span>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <NavPill icon={<FiGrid size={14} />} label="Graph" active={graphVisible}
            onClick={() => setActivePanel(p => p === 'graph' ? 'both' : p === 'both' ? 'chat' : 'both')}
            t={t}
          />
          <NavPill icon={<FiMessageSquare size={14} />} label="Chat" active={chatVisible}
            onClick={() => setActivePanel(p => p === 'chat' ? 'both' : p === 'both' ? 'graph' : 'both')}
            t={t}
          />
          <div style={{ width: 1, height: 24, background: t.border, margin: '0 4px' }} />
          <motion.button
            whileHover={{ scale: 1.08 }} whileTap={{ scale: 0.9 }}
            onClick={toggle}
            style={{
              width: 36, height: 36, borderRadius: '50%',
              border: `1px solid ${t.toggleBorder}`,
              background: t.toggleBg, color: t.toggleColor,
              cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            {themeName === 'dark' ? <FiSun size={15} /> : <FiMoon size={15} />}
          </motion.button>
        </div>
      </nav>

      {/* ═══ PANELS ═══ */}
      <div
        ref={containerRef}
        style={{
          flex: 1, display: 'flex',
          padding: CONTENT_PAD,
          gap: 0,
          overflow: 'visible',   /* allow borders to show at edges */
          minHeight: 0,
        }}
      >

        {/* ─── LEFT: Graph ─── */}
        <motion.div
          layout
          transition={SPRING}
          style={{
            flexGrow: graphFlex,
            flexShrink: 1,
            flexBasis: 0,
            minWidth: 0,
            overflow: 'hidden',
            borderRadius: PANEL_RADIUS,
            background: t.panelContainerBg,
            border: `1px solid ${t.panelBorder}`,
            boxShadow: t.shadow,
            display: 'flex', flexDirection: 'column',
            visibility: graphVisible ? 'visible' : 'hidden',
          }}
        >
          {/* Header always visible */}
          <PanelHeader
            title="Knowledge Graph"
            icon={<FiGrid size={14} />}
            onCollapse={collapseGraph}
            collapseDir="left"
            visible={graphVisible}
            t={t}
          />
          {/* Content fades in/out without unmounting */}
          <motion.div
            animate={{ opacity: graphVisible ? 1 : 0 }}
            transition={FADE}
            style={{ flex: 1, overflow: 'hidden', position: 'relative', pointerEvents: graphVisible ? 'auto' : 'none' }}
          >
            <Flow />
          </motion.div>
        </motion.div>

        {/* ─── DRAG DIVIDER ─── */}
        <AnimatePresence>
          {activePanel === 'both' && (
            <motion.div
              key="divider"
              initial={{ opacity: 0, scaleY: 0.6 }}
              animate={{ opacity: 1, scaleY: 1 }}
              exit={{ opacity: 0, scaleY: 0.6 }}
              transition={FADE}
              onMouseDown={onDividerMouseDown}
              style={{
                width: DIVIDER_W,
                flexShrink: 0,
                cursor: 'col-resize',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                zIndex: 10,
              }}
            >
              <DividerHandle t={t} />
            </motion.div>
          )}
        </AnimatePresence>

        {/* ─── RIGHT: Chat ─── */}
        <motion.div
          layout
          transition={SPRING}
          style={{
            flexGrow: chatFlex,
            flexShrink: 1,
            flexBasis: 0,
            minWidth: 0,
            overflow: 'hidden',
            borderRadius: PANEL_RADIUS,
            background: t.panelContainerBg,
            border: `1px solid ${t.panelBorder}`,
            boxShadow: t.shadow,
            display: 'flex', flexDirection: 'column',
            visibility: chatVisible ? 'visible' : 'hidden',
          }}
        >
          <PanelHeader
            title="Chat"
            icon={<FiMessageSquare size={14} />}
            onCollapse={collapseChat}
            collapseDir="right"
            visible={chatVisible}
            t={t}
          />
          <motion.div
            animate={{ opacity: chatVisible ? 1 : 0 }}
            transition={FADE}
            style={{ flex: 1, overflow: 'hidden', pointerEvents: chatVisible ? 'auto' : 'none' }}
          >
            <ChatDialog />
          </motion.div>
        </motion.div>

      </div>
    </div>
  );
};

/* ─────────────────── Drag handle visual ─────────────────────── */
const DividerHandle = ({ t }) => {
  const [hovered, setHovered] = useState(false);
  return (
    <motion.div
      animate={{
        background: hovered ? t.accent : t.panelBorder,
        height: hovered ? 56 : 40,
      }}
      transition={{ type: 'spring', stiffness: 400, damping: 28 }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width: 4, borderRadius: 4,
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 4,
      }}
    >
      {[0, 1, 2].map(i => (
        <motion.div
          key={i}
          animate={{ opacity: hovered ? 0.9 : 0.35, scale: hovered ? 1.2 : 1 }}
          transition={{ delay: i * 0.04, type: 'spring', stiffness: 400, damping: 26 }}
          style={{ width: 3, height: 3, borderRadius: '50%', background: '#fff' }}
        />
      ))}
    </motion.div>
  );
};

/* ─────────────────── Panel Header ───────────────────────────── */
const PanelHeader = ({ title, icon, onCollapse, collapseDir, visible, t }) => {
  const chevron = collapseDir === 'left'
    ? (visible ? <FiChevronLeft size={15} /> : <FiChevronRight size={15} />)
    : (visible ? <FiChevronRight size={15} /> : <FiChevronLeft size={15} />);

  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 16px',
      borderBottom: `1px solid ${t.panelHeaderBorder}`,
      background: t.panelHeaderBg,
      flexShrink: 0,
      borderRadius: `${PANEL_RADIUS}px ${PANEL_RADIUS}px 0 0`,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ color: t.accent, display: 'flex' }}>{icon}</span>
        <span style={{ fontSize: 14, fontWeight: 600, color: t.text, letterSpacing: -0.2 }}>
          {title}
        </span>
      </div>
      <motion.button
        whileHover={{ scale: 1.12 }}
        whileTap={{ scale: 0.88 }}
        onClick={onCollapse}
        style={{
          width: 28, height: 28, borderRadius: 8, border: 'none',
          background: t.collapseBtnBg, color: t.collapseBtnColor,
          cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
        onMouseEnter={e => e.currentTarget.style.background = t.collapseBtnHover}
        onMouseLeave={e => e.currentTarget.style.background = t.collapseBtnBg}
      >
        <motion.span
          key={String(visible)}
          initial={{ rotate: -45, opacity: 0 }}
          animate={{ rotate: 0, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 350, damping: 28 }}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
        >
          {chevron}
        </motion.span>
      </motion.button>
    </div>
  );
};

/* ─────────────────── Navbar pill ────────────────────────────── */
const NavPill = ({ icon, label, active, onClick, t }) => (
  <motion.button
    whileHover={{ scale: 1.03 }}
    whileTap={{ scale: 0.96 }}
    onClick={onClick}
    style={{
      display: 'flex', alignItems: 'center', gap: 6,
      padding: '7px 16px', borderRadius: 24,
      border: active ? `1px solid ${t.navBtnActiveBorder}` : `1px solid ${t.navBtnInactiveBorder}`,
      background: active ? t.navBtnActiveBg : t.navBtnInactiveBg,
      color: active ? t.navBtnActiveColor : t.navBtnInactiveColor,
      fontSize: 13, fontWeight: 500, cursor: 'pointer',
      transition: 'background 0.2s ease, color 0.2s ease, border-color 0.2s ease',
      letterSpacing: 0.2,
    }}
  >
    {icon}
    <span>{label}</span>
  </motion.button>
);

/* ─────────────────── Export ─────────────────────────────────── */
export const DashboardPage = () => (
  <ThemeProvider>
    <Dashboard />
  </ThemeProvider>
);