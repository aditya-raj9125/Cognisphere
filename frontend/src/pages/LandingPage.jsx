import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, useInView } from 'framer-motion';
import { FiCpu, FiGrid, FiMessageSquare, FiUploadCloud, FiLink, FiSearch, FiZap, FiLayers, FiBookOpen, FiGlobe, FiShield, FiArrowRight, FiArrowUpRight } from 'react-icons/fi';

/* ─── Reveal on scroll ─── */
const Reveal = ({ children, delay = 0 }) => {
  const ref = useRef(null);
  const inView = useInView(ref, { once: true, margin: '-60px' });
  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 28 }}
      animate={inView ? { opacity: 1, y: 0 } : { opacity: 0, y: 28 }}
      transition={{ duration: 0.55, delay, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  );
};

/* ─── Section pill badge ─── */
const SectionBadge = ({ label }) => (
  <div style={{
    display: 'inline-flex',
    alignItems: 'center',
    padding: '8px 20px',
    borderRadius: 24,
    border: '1px solid #e5e7eb',
    background: '#fff',
    color: '#6b7280',
    fontSize: 13,
    fontWeight: 500,
    letterSpacing: 0.3,
    marginBottom: 20,
  }}>
    {label}
  </div>
);

/* ─── Feature card with icon and arrow ─── */
const FeatureCard = ({ title, desc, delay = 0, stats }) => (
  <Reveal delay={delay}>
    <div style={{
      background: '#fff',
      borderRadius: 24,
      border: '1px solid #f0f0f0',
      padding: '48px 40px',
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
      height: '100%',
      transition: 'box-shadow 0.25s, transform 0.25s',
      position: 'relative',
    }}
      onMouseEnter={e => {
        e.currentTarget.style.boxShadow = '0 12px 40px rgba(0,0,0,0.06)';
        e.currentTarget.style.transform = 'translateY(-4px)';
      }}
      onMouseLeave={e => {
        e.currentTarget.style.boxShadow = 'none';
        e.currentTarget.style.transform = 'translateY(0)';
      }}
    >
      <h3 style={{ 
        fontSize: 32, 
        fontWeight: 400, 
        color: '#111827', 
        margin: 0,
        fontFamily: "'Playfair Display', Georgia, serif",
        lineHeight: 1.2,
      }}>
        {title}
      </h3>
      <p style={{ fontSize: 15, lineHeight: 1.8, color: '#6b7280', margin: 0 }}>{desc}</p>
      {stats && (
        <div style={{ marginTop: 20 }}>
          <div style={{ fontSize: 56, fontWeight: 700, color: '#FF7A3D', lineHeight: 1, marginBottom: 8 }}>
            {stats.value}
          </div>
          <div style={{ fontSize: 14, color: '#6b7280' }}>{stats.label}</div>
        </div>
      )}
      <div style={{
        width: 52,
        height: 52,
        borderRadius: '50%',
        background: '#FF7A3D',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'absolute',
        bottom: 32,
        right: 32,
        cursor: 'pointer',
        transition: 'transform 0.2s',
      }}
      onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.1)'}
      onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'}
      >
        <FiArrowRight size={24} color="#fff" />
      </div>
    </div>
  </Reveal>
);

/* ─── FAQ accordion ─── */
const FAQItem = ({ question, answer }) => {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ borderBottom: '1px solid #e5e7eb' }}>
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '24px 8px',
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
          textAlign: 'left',
        }}
      >
        <span style={{ fontSize: 17, fontWeight: 500, color: '#111827', lineHeight: 1.5, paddingRight: 20 }}>
          {question}
        </span>
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none"
          style={{ flexShrink: 0, transform: open ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.25s' }}>
          <path d="M5 8l5 5 5-5" stroke="#9ca3af" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      <motion.div
        initial={false}
        animate={{ height: open ? 'auto' : 0, opacity: open ? 1 : 0 }}
        transition={{ duration: 0.3 }}
        style={{ overflow: 'hidden' }}
      >
        <p style={{ padding: '0 8px 24px', margin: 0, fontSize: 15, lineHeight: 1.8, color: '#6b7280' }}>
          {answer}
        </p>
      </motion.div>
    </div>
  );
};

/* ═══════════════════════════════════════════ */
/* LANDING PAGE                                */
/* ═══════════════════════════════════════════ */
export const LandingPage = () => {
  const navigate = useNavigate();
  const go = () => navigate('/dashboard');

  return (
    <div style={{
      minHeight: '100vh',
      background: '#FF7A3D',
      padding: '8px',
      fontFamily: "'Inter','Segoe UI',system-ui,sans-serif",
      overflowX: 'hidden',
    }}>
      {/* Inner container with white background */}
      <div style={{
        minHeight: 'calc(100vh - 16px)',
        background: '#F8F9FA',
        borderRadius: 12,
        overflow: 'hidden',
        color: '#111827',
      }}>

      {/* ═══ NAVBAR ═══ */}
      <motion.nav
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35 }}
        style={{
          position: 'sticky', top: 0, zIndex: 100,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '0 clamp(32px, 6vw, 80px)',
          height: 80,
          background: 'rgba(248,249,250,0.95)',
          backdropFilter: 'blur(12px)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontWeight: 700, fontSize: 24, color: '#111827', letterSpacing: -0.5 }}>
            CogniSphere
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
          <a href="#features" style={{ fontSize: 15, color: '#4b5563', textDecoration: 'none', fontWeight: 500 }}>Features</a>
          <a href="#how-it-works" style={{ fontSize: 15, color: '#4b5563', textDecoration: 'none', fontWeight: 500 }}>How it works</a>
          <a href="#faq" style={{ fontSize: 15, color: '#4b5563', textDecoration: 'none', fontWeight: 500 }}>FAQ</a>
          <span style={{ fontSize: 15, color: '#4b5563', textDecoration: 'none', fontWeight: 500, cursor: 'pointer' }}>Login</span>
          <button
            onClick={go}
            style={{
              padding: '12px 28px',
              background: '#2C2C2C',
              color: '#fff',
              border: 'none',
              borderRadius: 50,
              fontWeight: 600,
              fontSize: 14,
              cursor: 'pointer',
              transition: 'all 0.2s',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
            onMouseEnter={e => { 
              e.currentTarget.style.background = '#FF7A3D';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={e => { 
              e.currentTarget.style.background = '#2C2C2C';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            Get early access
            <FiArrowUpRight size={16} />
          </button>
        </div>
      </motion.nav>

      {/* ═══ HERO ═══ */}
      <section style={{
        padding: '100px 32px 80px',
        textAlign: 'center',
        maxWidth: 1000,
        margin: '0 auto',
        background: `linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,122,61,0.03) 100%)`,
      }}>
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.1, duration: 0.4 }}
        >
          {/* Toggle switch */}
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            marginBottom: 32,
          }}>
            <div style={{
              width: 56,
              height: 32,
              borderRadius: 20,
              background: '#FF7A3D',
              position: 'relative',
              cursor: 'pointer',
              padding: 3,
            }}>
              <div style={{
                width: 26,
                height: 26,
                borderRadius: '50%',
                background: '#fff',
                position: 'absolute',
                left: 4,
                top: 3,
                transition: 'transform 0.3s',
              }} />
            </div>
          </div>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.6 }}
          style={{
            fontSize: 'clamp(44px, 7vw, 78px)',
            fontWeight: 400,
            lineHeight: 1.15,
            margin: '0 0 28px',
            letterSpacing: '-0.02em',
            color: '#111827',
            fontFamily: "'Playfair Display', Georgia, serif",
          }}
        >
          One portal for every<br />
          <span style={{ fontStyle: 'italic' }}>knowledge & insight</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
          style={{
            color: '#4b5563',
            fontSize: 'clamp(16px, 2vw, 18px)',
            lineHeight: 1.75,
            maxWidth: 680,
            margin: '0 auto 48px',
          }}
        >
          A modern knowledge workspace that transforms scattered information into
          a visual graph you can explore, query, and expand with AI—all in one clean, structured space
        </motion.p>

        <motion.button
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45 }}
          whileHover={{ scale: 1.03, y: -2 }}
          whileTap={{ scale: 0.98 }}
          onClick={go}
          style={{
            padding: '16px 48px',
            background: '#2C2C2C',
            color: '#fff',
            border: 'none',
            borderRadius: 50,
            fontWeight: 600,
            fontSize: 16,
            cursor: 'pointer',
            letterSpacing: -0.2,
            display: 'inline-flex',
            alignItems: 'center',
            gap: 10,
            transition: 'all 0.3s',
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#FF7A3D'}
          onMouseLeave={e => e.currentTarget.style.background = '#2C2C2C'}
        >
          Get early access
          <FiArrowUpRight size={18} />
        </motion.button>

        {/* Feature tags */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 32,
            marginTop: 64,
            flexWrap: 'wrap',
          }}
        >
          {[
            { icon: <FiGrid size={18} />, label: 'Knowledge Graph' },
            { icon: <FiUploadCloud size={18} />, label: 'Multi-Source Upload' },
            { icon: <FiMessageSquare size={18} />, label: 'AI Chat' },
            { icon: <FiZap size={18} />, label: 'Smart Insights' },
          ].map((item, i) => (
            <div key={i} style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '10px 20px',
              background: '#fff',
              borderRadius: 30,
              border: '1px solid #f0f0f0',
            }}>
              <div style={{ color: '#FF7A3D' }}>{item.icon}</div>
              <span style={{ fontSize: 14, fontWeight: 500, color: '#4b5563' }}>{item.label}</span>
            </div>
          ))}
        </motion.div>
      </section>

      {/* ═══ PRODUCT SHOWCASE ═══ */}
      <section style={{ padding: '80px 32px 100px' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto', textAlign: 'center', marginBottom: 80 }}>
          <Reveal>
            <SectionBadge label="The way knowledge management should work" />
            <h2 style={{
              fontSize: 'clamp(32px, 5vw, 52px)',
              fontWeight: 400,
              color: '#111827',
              margin: '0 0 20px',
              letterSpacing: '-0.02em',
              fontFamily: "'Playfair Display', Georgia, serif",
              lineHeight: 1.2,
            }}>
              From scattered notes to a<br />streamlined knowledge system
            </h2>
            <p style={{ color: '#6b7280', fontSize: 16, margin: '0 0 0', maxWidth: 600, margin: '0 auto' }}>
              A intelligent workflow where insights are always connected, accessible, and growing
            </p>
          </Reveal>
        </div>

        {/* Three column features */}
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 32, marginBottom: 32 }}>
            <FeatureCard
              title="Create your knowledge workspace"
              desc="Set up your workspace and add your sources. Everything starts in one organized visual graph where AI connects the dots automatically."
              delay={0}
            />
            <FeatureCard
              title="Share every insight clearly"
              desc="Upload files, add nodes, explore connections, and chat with AI—all in one clear portal your knowledge actually understands."
              delay={0.1}
            />
            <FeatureCard
              title="Align on every next step"
              desc="Your knowledge stays in sync with new discoveries, connections, and insights without manual organization."
              delay={0.2}
            />
          </div>
        </div>
      </section>

      {/* ═══ FEATURES SECTION ═══ */}
      <section id="features" style={{ padding: '100px 32px 120px', background: '#fff' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto', textAlign: 'center', marginBottom: 80 }}>
          <Reveal>
            <SectionBadge label="Powerful features built for knowledge clarity" />
            <h2 style={{
              fontSize: 'clamp(32px, 5vw, 52px)',
              fontWeight: 400,
              color: '#111827',
              margin: '0 0 20px',
              letterSpacing: '-0.02em',
              fontFamily: "'Playfair Display', Georgia, serif",
            }}>
              Simple, structured tools that help you understand every part<br/>of the project—without searching
            </h2>
          </Reveal>
        </div>

        {/* Feature grid - 2x2 */}
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(480px, 1fr))', gap: 32 }}>
            
            {/* Visual Knowledge Graph */}
            <FeatureCard
              title="Visual knowledge spaces"
              desc="Organized, interconnected spaces where you can follow all your concepts with support for multiple active topics in one clean view."
              delay={0}
            />
            
            {/* AI Chat with stats */}
            <FeatureCard
              title="Smart communication & updates"
              desc="Get meaningful insights — not noise. Every connection, concept, and discovery appears instantly and stays connected to the right context."
              stats={{ value: '3×', label: 'faster knowledge retrieval' }}
              delay={0.1}
            />
            
            {/* Multi-source upload */}
            <FeatureCard
              title="Unified knowledge hub"
              desc="Monitor sources, progress, and insight goals in one clear workspace — no scattered notes needed."
              stats={{ value: '40%', label: 'fewer searches needed' }}
              delay={0.15}
            />
            
            {/* Smart linking */}
            <FeatureCard
              title="AI-powered connections"
              desc="Give each concept a personalized, structured space with automatic connections, semantic clustering, and intelligent linking."
              delay={0.2}
            />
          </div>
        </div>
      </section>

      {/* ═══ HOW IT WORKS ═══ */}
      <section id="how-it-works" style={{ padding: '100px 32px 120px', background: '#F8F9FA' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', textAlign: 'center' }}>
          <Reveal>
            <SectionBadge label="Before → After" />
            <h2 style={{
              fontSize: 'clamp(32px, 5vw, 52px)',
              fontWeight: 400,
              color: '#111827',
              margin: '0 0 20px',
              letterSpacing: '-0.02em',
              fontFamily: "'Playfair Display', Georgia, serif",
              lineHeight: 1.2,
            }}>
              From scattered tools to a<br />streamlined knowledge experience
            </h2>
            <p style={{ color: '#6b7280', fontSize: 16, maxWidth: 700, margin: '0 auto 80px' }}>
              Clearer insights, fewer searches, and every concept always connected—without switching tools
            </p>
          </Reveal>

          {/* Before/After visualization */}
          <Reveal delay={0.15}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: '1fr auto 1fr',
              gap: 60,
              alignItems: 'center',
              maxWidth: 1000,
              margin: '0 auto 100px',
            }}>
              {/* Before - scattered */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
                alignItems: 'flex-end',
              }}>
                {['Notes', 'Bookmarks', 'Documents', 'Links', 'Research'].map((item, i) => (
                  <div key={i} style={{
                    padding: '12px 24px',
                    background: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: 12,
                    fontSize: 15,
                    color: '#6b7280',
                    opacity: 0.7,
                  }}>
                    {item}
                  </div>
                ))}
              </div>

              {/* Center - logo/icon */}
              <div style={{
                width: 80,
                height: 80,
                borderRadius: 24,
                background: '#FF7A3D',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 24,
                fontWeight: 700,
                color: '#fff',
                flexShrink: 0,
              }}>
                CS
              </div>

              {/* After - unified */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
                alignItems: 'flex-start',
              }}>
                {[
                  { icon: '🔍', label: 'Smart knowledge graph', color: '#FF7A3D' },
                  { icon: '💬', label: 'All insights in one place', color: '#8B5CF6' },
                  { icon: '📁', label: 'Sources (structured & searchable)', color: '#22C55E' },
                  { icon: '🔗', label: 'Auto-connected concepts', color: '#EF4444' },
                  { icon: '⚡', label: 'AI-powered discovery', color: '#3B82F6' },
                ].map((item, i) => (
                  <div key={i} style={{
                    padding: '12px 20px',
                    background: '#fff',
                    border: '2px solid #f0f0f0',
                    borderRadius: 12,
                    fontSize: 15,
                    color: '#111827',
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                  }}>
                    <span style={{ fontSize: 18 }}>{item.icon}</span>
                    {item.label}
                  </div>
                ))}
              </div>
            </div>
          </Reveal>

          {/* Three steps */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 48, textAlign: 'left' }}>
            {[
              { 
                num: '01', 
                title: 'Upload your sources', 
                desc: 'Add documents, URLs, audio, images, or paste text. CogniSphere extracts concepts and adds them to your graph automatically.' 
              },
              { 
                num: '02', 
                title: 'Explore & connect', 
                desc: 'Watch your knowledge graph grow. AI creates clusters, draws connections, and organizes information visually as you add more.' 
              },
              { 
                num: '03', 
                title: 'Chat & discover', 
                desc: 'Ask questions across your entire knowledge base. Get contextual answers and discover new connections you never noticed.' 
              },
            ].map((s, i) => (
              <Reveal key={i} delay={i * 0.1}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  <div style={{
                    fontSize: 48,
                    fontWeight: 700,
                    color: '#FF7A3D',
                    fontFamily: "'Playfair Display', Georgia, serif",
                    opacity: 0.3,
                  }}>
                    {s.num}
                  </div>
                  <h3 style={{
                    fontSize: 24,
                    fontWeight: 400,
                    color: '#111827',
                    margin: 0,
                    fontFamily: "'Playfair Display', Georgia, serif",
                  }}>
                    {s.title}
                  </h3>
                  <p style={{ fontSize: 15, lineHeight: 1.75, color: '#6b7280', margin: 0 }}>{s.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ PRIVACY ═══ */}
      <section style={{ padding: '100px 32px 120px', background: '#fff' }}>
        <div style={{ maxWidth: 800, margin: '0 auto', textAlign: 'center' }}>
          <Reveal>
            <SectionBadge label="Privacy & Security" />
            <h2 style={{
              fontSize: 'clamp(28px, 4vw, 44px)',
              fontWeight: 400,
              color: '#111827',
              margin: '0 0 20px',
              letterSpacing: '-0.02em',
              fontFamily: "'Playfair Display', Georgia, serif",
            }}>
              Your knowledge, your control
            </h2>
            <p style={{ color: '#6b7280', fontSize: 16, maxWidth: 600, margin: '0 auto 60px', lineHeight: 1.7 }}>
              Your knowledge graph and conversations are private and secure. Data is processed on AWS infrastructure and never used for model training.
            </p>
          </Reveal>
          
          <Reveal delay={0.15}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: 32,
              textAlign: 'left',
            }}>
              <div style={{
                background: '#F8F9FA',
                borderRadius: 20,
                padding: '32px',
                border: '2px solid #f0f0f0',
              }}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  background: '#22C55E',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 20,
                }}>
                  <FiShield size={24} color="#fff" />
                </div>
                <h3 style={{ fontSize: 20, fontWeight: 600, color: '#111827', margin: '0 0 12px' }}>
                  End-to-end secure
                </h3>
                <p style={{ fontSize: 14, color: '#6b7280', lineHeight: 1.7, margin: 0 }}>
                  AWS IAM authentication with encrypted Neo4j graph storage and secure Bedrock AI processing
                </p>
              </div>

              <div style={{
                background: '#F8F9FA',
                borderRadius: 20,
                padding: '32px',
                border: '2px solid #f0f0f0',
              }}>
                <div style={{
                  width: 48,
                  height: 48,
                  borderRadius: 12,
                  background: '#FF7A3D',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 20,
                }}>
                  <FiLayers size={24} color="#fff" />
                </div>
                <h3 style={{ fontSize: 20, fontWeight: 600, color: '#111827', margin: '0 0 12px' }}>
                  Private by design
                </h3>
                <p style={{ fontSize: 14, color: '#6b7280', lineHeight: 1.7, margin: 0 }}>
                  Your data stays yours. No cross-user sharing, no training data collection, no third-party access
                </p>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      {/* ═══ FAQ ═══ */}
      <section id="faq" style={{ padding: '100px 32px 120px', background: '#F8F9FA' }}>
        <div style={{ maxWidth: 760, margin: '0 auto' }}>
          <Reveal>
            <div style={{ textAlign: 'center', marginBottom: 64 }}>
              <SectionBadge label="FAQ" />
              <h2 style={{
                fontSize: 'clamp(28px, 4vw, 44px)',
                fontWeight: 400,
                color: '#111827',
                margin: '0 0 8px',
                letterSpacing: '-0.02em',
                fontFamily: "'Playfair Display', Georgia, serif",
              }}>
                Common questions
              </h2>
              <p style={{ color: '#9ca3af', fontSize: 15 }}>Everything you need to know about CogniSphere</p>
            </div>
          </Reveal>

          <Reveal delay={0.1}>
            <div>
              <FAQItem
                question="What makes CogniSphere different from other note-taking apps?"
                answer="CogniSphere transforms every piece of information into a connected node in a visual knowledge graph. The AI reasons across your entire graph to provide contextual answers, automatically links related concepts, and clusters topics—no manual organization required."
              />
              <FAQItem
                question="How does the knowledge graph work?"
                answer="When you upload a document, paste a URL, or have a conversation, CogniSphere parses the content, extracts key concepts, and creates nodes connected by semantic similarity using vector embeddings. The AI automatically finds relationships between ideas."
              />
              <FAQItem
                question="What file types and sources can I add?"
                answer="PDFs, images (via OCR), plain text, URLs, audio files (via transcription), and YouTube videos. Each source is parsed, summarized by AI, and integrated as connected nodes in your graph."
              />
              <FAQItem
                question="Is my data private and secure?"
                answer="Yes. Your data is processed on AWS infrastructure using IAM-controlled access with encrypted storage. Nothing is shared across users or used for model training. You maintain full control of your knowledge."
              />
              <FAQItem
                question="What AI models power CogniSphere?"
                answer="AWS Bedrock for language understanding and generation, Amazon Titan for vector embeddings, and Neo4j for graph storage. The entire AI pipeline runs serverless on AWS with enterprise-grade security."
              />
            </div>
          </Reveal>
        </div>
      </section>

      {/* ═══ BOTTOM CTA ═══ */}
      <section style={{
        padding: '100px 32px 120px',
        textAlign: 'center',
        background: '#fff',
      }}>
        <Reveal>
          <h2 style={{
            fontSize: 'clamp(32px, 5vw, 56px)',
            fontWeight: 400,
            color: '#111827',
            margin: '0 0 20px',
            letterSpacing: '-0.02em',
            fontFamily: "'Playfair Display', Georgia, serif",
            lineHeight: 1.2,
          }}>
            Start building your<br />knowledge graph today
          </h2>
          <p style={{ color: '#6b7280', fontSize: 16, marginBottom: 48, maxWidth: 500, margin: '0 auto 48px' }}>
            Transform scattered information into connected insights.<br />Free to explore. No credit card required.
          </p>
          <motion.button
            whileHover={{ scale: 1.03, y: -2 }}
            whileTap={{ scale: 0.98 }}
            onClick={go}
            style={{
              padding: '18px 56px',
              background: '#2C2C2C',
              color: '#fff',
              border: 'none',
              borderRadius: 50,
              fontWeight: 600,
              fontSize: 17,
              cursor: 'pointer',
              letterSpacing: -0.2,
              display: 'inline-flex',
              alignItems: 'center',
              gap: 10,
              transition: 'all 0.3s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#FF7A3D'}
            onMouseLeave={e => e.currentTarget.style.background = '#2C2C2C'}
          >
            Get early access
            <FiArrowUpRight size={20} />
          </motion.button>
        </Reveal>
      </section>

      {/* ═══ PARTNERS / TECH STACK ═══ */}
      <section style={{
        padding: '60px 32px',
        background: '#F8F9FA',
        borderTop: '1px solid #e5e7eb',
      }}>
        <div style={{ maxWidth: 1000, margin: '0 auto', textAlign: 'center' }}>
          <p style={{ fontSize: 13, color: '#9ca3af', marginBottom: 32, textTransform: 'uppercase', letterSpacing: 1.5, fontWeight: 600 }}>
            Built with
          </p>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 48,
            flexWrap: 'wrap',
            opacity: 0.5,
          }}>
            {['AWS Bedrock', 'Neo4j', 'LangChain4j', 'ReactFlow', 'Spring Boot'].map((tech, i) => (
              <div key={i} style={{ fontSize: 15, fontWeight: 600, color: '#6b7280' }}>
                {tech}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ FOOTER ═══ */}
      <footer style={{
        padding: '32px 48px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 16,
        background: '#F8F9FA',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: 700, fontSize: 18, color: '#111827' }}>CogniSphere</span>
        </div>
        <span style={{ color: '#9ca3af', fontSize: 13 }}>
          © 2026 CogniSphere. AI-powered knowledge workspace.
        </span>
      </footer>
      
      </div>
    </div>
  );
};