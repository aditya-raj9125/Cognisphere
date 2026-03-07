import React from 'react';
import { Handle, Position } from 'reactflow';
import { useTheme } from '../context/ThemeContext';

/* Category colour map (must stay in sync with Flow.jsx) */
const CATEGORY_COLORS = {
  science:    '#3b82f6',
  technology: '#6366f1',
  news:       '#f97316',
  academics:  '#10b981',
  arts:       '#ec4899',
  health:     '#ef4444',
  business:   '#f59e0b',
  history:    '#92400e',
  general:    '#64748b',
  chat:       '#8b5cf6',
};

const getCatColor = (cat) => CATEGORY_COLORS[cat] || CATEGORY_COLORS.general;

const invisibleHandle = {
  top: '50%', left: '50%',
  transform: 'translate(-50%, -50%)',
  background: 'transparent',
  width: 0, height: 0,
  borderRadius: '50%',
  pointerEvents: 'none',
  zIndex: 0,
  opacity: 0,
};

export const MarkdownNode = ({ data }) => {
  const { t } = useTheme();
  const categoryColor = getCatColor(data.category || 'general');
  const isSelected = !!data.isSelected;
  const isRec = !!data.isRecommendation;

  /* ─ background ─ */
  const bg = isSelected
    ? t.bgNodeSelected
    : isRec
      ? (t.name === 'dark' ? 'rgba(15,20,32,0.75)' : 'rgba(255,255,255,0.80)')
      : t.bgNode;

  /* ─ border ─ */
  const border = isSelected
    ? `1.5px solid ${categoryColor}`
    : isRec
      ? `1px dashed ${categoryColor}66`
      : `1px solid ${t.name === 'dark' ? categoryColor + '30' : categoryColor + '45'}`;

  return (
    <div
      style={{
        padding: '8px 12px',
        borderRadius: 16,
        backgroundColor: bg,
        border,
        boxShadow: isSelected
          ? `0 0 0 2px ${categoryColor}30, ${t.shadowNode}`
          : t.shadowNode,
        fontFamily: 'Inter, sans-serif',
        position: 'relative',
        width: `${data.size * 1.5}px`,
        minHeight: `${data.size * 0.6}px`,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        zIndex: 2,
        transition: 'border 0.2s ease, box-shadow 0.2s ease',
        outline: 'none',
        overflow: 'visible',
        backdropFilter: t.name === 'dark' ? 'blur(8px)' : 'none',
      }}
    >
      {/* Category dot */}
      <div style={{
        position: 'absolute',
        top: 6,
        right: 8,
        width: 6,
        height: 6,
        borderRadius: '50%',
        backgroundColor: categoryColor,
        opacity: 0.75,
        flexShrink: 0,
      }} />

      <Handle type="target" position={Position.Top} style={invisibleHandle} isConnectable={false} id="target" />

      <div style={{
        fontSize: 12,
        fontWeight: 500,
        color: t.text,
        fontFamily: 'Inter, sans-serif',
        maxWidth: '100%',
        overflow: 'visible',
        whiteSpace: 'normal',
        padding: '0 4px',
        lineHeight: 1.4,
        letterSpacing: 0.1,
      }}>
        {data.label}
      </div>

      <Handle type="source" position={Position.Top} style={invisibleHandle} isConnectable={false} id="source" />
    </div>
  );
};