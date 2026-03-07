import React, { useState, useRef, useEffect } from 'react';
import { sendMessage } from '../api/chat';
import ReactMarkdown from 'react-markdown';
import { fetchNodeDetail } from '../api';
import { FiSend, FiZap, FiBook, FiGlobe } from 'react-icons/fi';
import { useTheme } from '../context/ThemeContext';

/* ──────────────────────────────────────────────────────────────────── */
/* Typing indicator dots                                              */
/* ──────────────────────────────────────────────────────────────────── */
const TypingDots = () => {
  const { t } = useTheme();
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '10px 14px' }}>
      {[0, 1, 2].map(i => (
        <div
          key={i}
          style={{
            width: 7,
            height: 7,
            borderRadius: '50%',
            background: t.accent,
            animation: 'typingBounce 1.2s infinite',
            animationDelay: `${i * 0.2}s`,
          }}
        />
      ))}
    </div>
  );
};

/* ──────────────────────────────────────────────────────────────────── */
/* Message bubble                                                     */
/* ──────────────────────────────────────────────────────────────────── */
const MessageBubble = ({ message, onButtonClick, isLoading }) => {
  const { t } = useTheme();
  const isUser = message.role === 'user';

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      gap: 6,
      maxWidth: '100%',
      alignSelf: isUser ? 'flex-end' : 'flex-start',
    }}>
      {/* sender label */}
      <div style={{
        fontSize: 11,
        fontWeight: 600,
        color: t.textMuted,
        letterSpacing: 0.5,
        paddingLeft: isUser ? 0 : 4,
        paddingRight: isUser ? 4 : 0,
        textAlign: isUser ? 'right' : 'left',
      }}>
        {isUser ? 'You' : '✦ CogniSphere'}
      </div>

      {/* bubble */}
      <div style={{
        padding: '10px 14px',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        background: isUser ? t.userBubbleBg : t.aiBubbleBg,
        color: isUser ? '#fff' : t.text,
        border: isUser ? 'none' : `1px solid ${t.border}`,
        fontSize: 14,
        lineHeight: 1.65,
        maxWidth: '88%',
        wordBreak: 'break-word',
      }}>
        <ReactMarkdown
          components={{
            p: ({ children }) => <p style={{ margin: 0 }}>{children}</p>,
            h1: ({ children }) => <h1 style={{ fontSize: '1.3rem', margin: '6px 0', color: isUser ? '#fff' : t.accentLight }}>{children}</h1>,
            h2: ({ children }) => <h2 style={{ fontSize: '1.1rem', margin: '6px 0', color: isUser ? '#fff' : t.accentLight }}>{children}</h2>,
            h3: ({ children }) => <h3 style={{ fontSize: '1rem', margin: '4px 0', color: isUser ? '#fff' : t.accent }}>{children}</h3>,
            ul: ({ children }) => <ul style={{ margin: '6px 0', paddingLeft: '1.4rem' }}>{children}</ul>,
            ol: ({ children }) => <ol style={{ margin: '6px 0', paddingLeft: '1.4rem' }}>{children}</ol>,
            li: ({ children }) => <li style={{ margin: '2px 0' }}>{children}</li>,
            code: ({ children }) => (
              <code style={{
                background: 'rgba(255,255,255,0.12)',
                padding: '1px 5px',
                borderRadius: 4,
                fontFamily: 'monospace',
                fontSize: 13,
              }}>{children}</code>
            ),
            pre: ({ children }) => (
              <pre style={{
                background: 'rgba(0,0,0,0.25)',
                padding: '10px 12px',
                borderRadius: 8,
                overflowX: 'auto',
                margin: '8px 0',
                fontSize: 13,
              }}>{children}</pre>
            ),
            blockquote: ({ children }) => (
              <blockquote style={{
                borderLeft: `3px solid ${t.accent}`,
                margin: '6px 0',
                paddingLeft: '10px',
                fontStyle: 'italic',
                color: t.textMuted,
              }}>{children}</blockquote>
            ),
            a: ({ href, children }) => (
              <a href={href} target="_blank" rel="noopener noreferrer"
                style={{ color: isUser ? '#fde8d0' : t.accent, textDecoration: 'underline' }}>
                {children}
              </a>
            ),
            strong: ({ children }) => <strong style={{ color: isUser ? '#fff' : t.accentLight, fontWeight: 700 }}>{children}</strong>,
          }}
        >
          {message.content}
        </ReactMarkdown>
      </div>

      {/* action buttons */}
      {message.buttons && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, paddingLeft: 4 }}>
          {message.buttons.map((button, i) => (
            <button
              key={i}
              onClick={() => onButtonClick(button)}
              disabled={isLoading}
              style={{
                padding: '6px 14px',
                background: 'rgba(255,122,61,0.10)',
                color: t.accent,
                border: `1px solid rgba(255,122,61,0.25)`,
                borderRadius: 20,
                cursor: isLoading ? 'not-allowed' : 'pointer',
                fontSize: 13,
                fontWeight: 600,
                transition: 'all 0.2s',
              }}
            >
              {button.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

/* ──────────────────────────────────────────────────────────────────── */
/* Welcome screen                                                     */
/* ──────────────────────────────────────────────────────────────────── */
const WelcomeScreen = ({ onQuickAction }) => {
  const { t } = useTheme();
  return (
    <div style={{
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px 24px',
      gap: 24,
      textAlign: 'center',
    }}>
      <div style={{
        width: 64,
        height: 64,
        borderRadius: 18,
        background: t.accentGradient,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: 30,
      }}>
        🧠
      </div>
      <div>
        <h2 style={{ color: t.text, fontSize: 20, fontWeight: 700, margin: '0 0 8px' }}>
          Ask CogniSphere anything
        </h2>
        <p style={{ color: t.textMuted, fontSize: 14, lineHeight: 1.7, margin: 0 }}>
          Your AI assistant searches your knowledge graph,<br />
          browses the web, and captures new insights as nodes.
        </p>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, width: '100%', maxWidth: 320 }}>
        {[
          { icon: '🔍', text: 'Find knowledge' },
          { icon: '🌐', text: 'Search the web' },
          { icon: '🗂️', text: 'Create nodes' },
          { icon: '🔗', text: 'Link concepts' },
        ].map((tip, i) => (
          <button key={i} onClick={() => onQuickAction && onQuickAction(tip.text)} style={{
            padding: '10px 12px',
            background: t.name === 'dark' ? 'rgba(255,122,61,0.06)' : 'rgba(255,122,61,0.04)',
            border: `1px solid ${t.border}`,
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: t.textMuted,
            fontSize: 13,
            fontWeight: 500,
            cursor: 'pointer',
            transition: 'background 0.2s',
          }}>
            <span>{tip.icon}</span> {tip.text}
          </button>
        ))}
      </div>
    </div>
  );
};

/* Graph-updated notification badge                                   */
const GraphUpdateBadge = ({ visible }) => {
  const { t } = useTheme();
  return (
    <div style={{
      position: 'absolute',
      top: 10,
      right: 10,
      background: t.accentGradient,
      color: '#fff',
      fontSize: 11,
      fontWeight: 700,
      padding: '4px 10px',
      borderRadius: 20,
      opacity: visible ? 1 : 0,
      transform: visible ? 'translateY(0)' : 'translateY(-8px)',
      transition: 'all 0.4s ease',
      pointerEvents: 'none',
      zIndex: 20,
      display: 'flex',
      alignItems: 'center',
      gap: 5,
    }}>
      <FiZap size={11} /> Graph updated
    </div>
  );
};

/* ──────────────────────────────────────────────────────────────────── */
/* Main ChatDialog component                                          */
/* ──────────────────────────────────────────────────────────────────── */
export const ChatDialog = () => {
  const { t } = useTheme();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [graphUpdated, setGraphUpdated] = useState(false);
  const [knowledgeOnly, setKnowledgeOnly] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const showGraphBadge = () => {
    setGraphUpdated(true);
    setTimeout(() => setGraphUpdated(false), 3000);
  };

  const convertToVideoUrl = (videoId) => {
    const baseurl = "https://www.videoindexer.ai/embed/player/914a5e40-8e73-4e7a-8d13-ff0193a05e75/videoId/?&locale=en&location=trial";
    return baseurl.replace('videoId', videoId);
  };

  const refreshFlow = async (selectedNode) => {
    if (!window.refreshFlow) return;
    const currentSelectedNode = selectedNode || window.getSelectedNode?.();
    if (currentSelectedNode) {
      await window.refreshFlow();
      try {
        let detail = await fetchNodeDetail(currentSelectedNode.id);
        const extra_formed = {
          ...detail.extra,
          video_urls: [
            ...(detail.extra.video_ids ? detail.extra.video_ids.map(convertToVideoUrl) : []),
            ...(detail.extra.youtube_urls || []),
          ],
        };
        window.setSelectedNode?.({ ...currentSelectedNode, detail: { ...detail, extra: extra_formed } });
      } catch (_) { /* silently skip detail refresh */ }
    } else {
      window.refreshFlow();
    }
    showGraphBadge();
  };

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);
    const sentInput = input;
    setInput('');
    setIsLoading(true);

    try {
      const sessionId = sessionStorage.getItem('sessionId');
      const selectedNode = window.getSelectedNode?.();
      const response = await sendMessage(sentInput, {
        sessionId,
        selectedNodeId: selectedNode?.id,
        knowledgeOnly,
      });

      const aiContent = response.response || response.content || 'No response received.';

      setMessages(prev => [...prev, {
        role: 'assistant',
        content: aiContent,
        buttons: response.buttons || null,
      }]);

      await refreshFlow(selectedNode);
    } catch (err) {
      console.error('Chat error:', err);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, there was an error processing your message. Please try again.',
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleButtonClick = async (button) => {
    if (isLoading) return;

    setMessages(prev => [...prev, { role: 'user', content: button.label }]);
    setIsLoading(true);

    try {
      const sessionId = sessionStorage.getItem('sessionId');
      const selectedNode = window.getSelectedNode?.();
      const response = await sendMessage(button.value || button.label, {
        sessionId,
        selectedNodeId: selectedNode?.id,
        knowledgeOnly,
      });

      setMessages(prev => [...prev, {
        role: 'assistant',
        content: response.response || response.content || 'Done.',
        buttons: response.buttons || null,
      }]);

      await refreshFlow(selectedNode);
    } catch (err) {
      console.error('Button click error:', err);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, there was an error processing your selection.',
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      background: t.panelContainerBg,
      fontFamily: "'Inter','Segoe UI',sans-serif",
      position: 'relative',
    }}>
      {/* CSS animations */}
      <style>{`
        @keyframes typingBounce {
          0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
          40%           { transform: translateY(-6px); opacity: 1; }
        }
        .chat-input:focus { outline: none; border-color: ${t.accent} !important; box-shadow: 0 0 0 3px rgba(255,122,61,0.15); }
        .send-btn:hover:not(:disabled) { filter: brightness(1.12); }
        .chat-messages::-webkit-scrollbar { width: 4px; }
        .chat-messages::-webkit-scrollbar-track { background: transparent; }
        .chat-messages::-webkit-scrollbar-thumb { background: rgba(128,128,128,0.2); border-radius: 4px; }
      `}</style>

      {/* Graph update badge */}
      <GraphUpdateBadge visible={graphUpdated} />

      {/* messages */}
      <div className="chat-messages" style={{
        flex: 1,
        overflowY: 'auto',
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
      }}>
        {messages.length === 0 && !isLoading && <WelcomeScreen onQuickAction={(text) => { setInput(text); }} />}

        {messages.map((msg, i) => (
          <MessageBubble
            key={i}
            message={msg}
            onButtonClick={handleButtonClick}
            isLoading={isLoading}
          />
        ))}

        {isLoading && (
          <div style={{ alignSelf: 'flex-start' }}>
            <div style={{
              padding: '2px 4px',
              borderRadius: '16px 16px 16px 4px',
              background: t.name === 'dark' ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.04)',
              border: `1px solid ${t.border}`,
            }}>
              <TypingDots />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* input area */}
      <div style={{
        padding: '12px 16px 10px',
        borderTop: `1px solid ${t.panelHeaderBorder}`,
        background: t.panelContainerBg,
        flexShrink: 0,
      }}>
        {/* Source toggle row */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 4,
          marginBottom: 10,
        }}>
          <button
            onClick={() => setKnowledgeOnly(false)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 5,
              padding: '5px 12px',
              fontSize: 12,
              fontWeight: 600,
              fontFamily: 'inherit',
              borderRadius: 20,
              border: `1.5px solid ${!knowledgeOnly ? t.accent : t.border}`,
              background: !knowledgeOnly ? 'rgba(255,122,61,0.10)' : 'transparent',
              color: !knowledgeOnly ? t.accent : t.textMuted,
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <FiGlobe size={13} />
            All Sources
          </button>
          <button
            onClick={() => setKnowledgeOnly(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 5,
              padding: '5px 12px',
              fontSize: 12,
              fontWeight: 600,
              fontFamily: 'inherit',
              borderRadius: 20,
              border: `1.5px solid ${knowledgeOnly ? t.accent : t.border}`,
              background: knowledgeOnly ? 'rgba(255,122,61,0.10)' : 'transparent',
              color: knowledgeOnly ? t.accent : t.textMuted,
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <FiBook size={13} />
            My Knowledge
          </button>
        </div>

        {/* Input row */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <textarea
            className="chat-input"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder={knowledgeOnly ? "Search your knowledge graph…" : "Ask anything…"}
            disabled={isLoading}
            rows={1}
            style={{
              flex: 1,
              padding: '10px 14px',
              border: `1px solid ${t.border}`,
              borderRadius: 12,
              backgroundColor: t.inputBg,
              color: t.text,
              fontSize: 14,
              lineHeight: 1.5,
              resize: 'none',
              fontFamily: 'inherit',
              transition: 'border-color 0.2s, box-shadow 0.2s',
              minHeight: 42,
              maxHeight: 120,
              overflowY: 'auto',
            }}
          />
          <button
            className="send-btn"
            onClick={handleSend}
            disabled={isLoading || !input.trim()}
            style={{
              width: 42,
              height: 42,
              flexShrink: 0,
              borderRadius: 12,
              border: 'none',
              background: isLoading || !input.trim()
                ? t.sendBtnInactive
                : t.sendBtnActive,
              color: isLoading || !input.trim() ? t.textMuted : '#fff',
              cursor: isLoading || !input.trim() ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'all 0.2s',
            }}
          >
            <FiSend size={17} />
          </button>
        </div>
        <div style={{ marginTop: 6, color: t.textMuted, fontSize: 11, textAlign: 'center', opacity: 0.6 }}>
          {knowledgeOnly ? 'Searching only your knowledge nodes' : 'AI may be wrong – verify important information'}
        </div>
      </div>
    </div>
  );
};
