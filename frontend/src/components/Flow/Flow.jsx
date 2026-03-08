import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import ReactFlow, {
  useNodesState,
  useEdgesState,
  useReactFlow,
  ReactFlowProvider,
  SelectionMode,
  Background
} from 'reactflow';
import { forceSimulation, forceLink, forceManyBody, forceCenter, forceCollide } from 'd3-force';
import 'reactflow/dist/style.css';
import { fetchGraphData, fetchNodeDetail, fetchRecommendation, confirmNode, mergeNodes as mergeNodesApi, relinkGraph } from '../../api';
import { MarkdownNode } from '../MarkdownNode';
import { NodeDetails } from './NodeDetails';
import { UploadBox } from './UploadBox';
import { useNodeManagement } from '../../hooks/useNodeManagement';
import { Toast } from '../Toast';
import './Flow.css';
import { useTheme } from '../../context/ThemeContext';

const nodeTypes = {
  markdown: MarkdownNode,
};

/* ─────────── Category palette ─────────── */
const CATEGORY_CONFIG = {
  science: { color: '#3b82f6', bg: '#eff6ff', label: '🔬 Science' },
  technology: { color: '#6366f1', bg: '#eef2ff', label: '💻 Technology' },
  news: { color: '#f97316', bg: '#fff7ed', label: '📰 News' },
  academics: { color: '#10b981', bg: '#ecfdf5', label: '📚 Academics' },
  arts: { color: '#ec4899', bg: '#fdf2f8', label: '🎨 Arts' },
  health: { color: '#ef4444', bg: '#fef2f2', label: '🏥 Health' },
  business: { color: '#f59e0b', bg: '#fffbeb', label: '💼 Business' },
  history: { color: '#92400e', bg: '#fefce8', label: '🏛️ History' },
  general: { color: '#64748b', bg: '#f8fafc', label: '🌐 General' },
  chat: { color: '#8b5cf6', bg: '#f5f3ff', label: '💬 Chat' },
};

const getCategoryColor = (category) =>
  (CATEGORY_CONFIG[category] || CATEGORY_CONFIG.general).color;

const getCategoryBg = (category) =>
  (CATEGORY_CONFIG[category] || CATEGORY_CONFIG.general).bg;

/* ─────────── Category cluster anchor positions ─────────── */
const CATEGORY_ANCHORS = {
  science: { x: -200, y: -150 },
  technology: { x: 100, y: -150 },
  news: { x: 200, y: 0 },
  academics: { x: -200, y: 100 },
  arts: { x: 0, y: 180 },
  health: { x: 180, y: 120 },
  business: { x: 50, y: -220 },
  history: { x: -100, y: 180 },
  general: { x: 0, y: 0 },
  chat: { x: -50, y: -80 },
};

const getAnchor = (category) =>
  CATEGORY_ANCHORS[category] || CATEGORY_ANCHORS.general;

const nodePositions = new Map();

const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

const getForceLayoutedElements = (nodes, edges) => {
  const nodesCopy = nodes.map(node => ({ ...node }));
  const edgesCopy = edges.map(edge => ({ ...edge }));

  // Custom force: attract each node toward its category anchor
  const categoryClusterForce = (alpha) => {
    for (const node of nodesCopy) {
      const category = node.data?.category || 'general';
      const anchor = getAnchor(category);
      // Strength proportional to alpha for gradual convergence
      const strength = 0.12;
      node.vx = (node.vx || 0) + (anchor.x - (node.x || 0)) * strength * alpha;
      node.vy = (node.vy || 0) + (anchor.y - (node.y || 0)) * strength * alpha;
    }
  };

  const simulation = forceSimulation(nodesCopy)
    .force('link', forceLink(edgesCopy)
      .id(d => d.id)
      .distance(120)
      .strength(0.4)
    )
    .force('charge', forceManyBody()
      .strength(-300)
    )
    .force('center', forceCenter(0, 0).strength(0.15))
    .force('collision', forceCollide()
      .radius(70)
      .strength(1)
    )
    .force('cluster', categoryClusterForce);

  simulation.tick(350);

  const layoutedNodes = nodesCopy.map(node => ({
    ...node,
    position: {
      x: node.x,
      y: node.y
    }
  }));

  const layoutedEdges = edges.map(edge => ({
    ...edge,
    source: edge.source,
    target: edge.target
  }));

  return { nodes: layoutedNodes, edges: layoutedEdges };
};

/* ─────────── Cluster Legend overlay ─────────── */
const ClusterLegend = ({ activeCategories }) => {
  const [collapsed, setCollapsed] = useState(false);
  if (!activeCategories || activeCategories.length === 0) return null;
  return (
    <div style={{
      position: 'absolute', bottom: 90, left: 12, zIndex: 1000,
      background: 'rgba(30,30,50,0.95)',
      border: '1px solid rgba(255,122,61,0.15)',
      borderRadius: 12,
      padding: collapsed ? '6px 10px' : '10px 14px',
      boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
      backdropFilter: 'blur(12px)',
      minWidth: collapsed ? 'auto' : 150,
      transition: 'all 0.2s',
    }}>
      <button
        onClick={() => setCollapsed(c => !c)}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          fontSize: 11, fontWeight: 700, color: '#94a3b8',
          display: 'flex', alignItems: 'center', gap: 4,
          padding: 0, marginBottom: collapsed ? 0 : 8,
        }}
      >
        {collapsed ? '▲' : '▼'}{!collapsed && <span style={{ marginLeft: 2 }}>Clusters</span>}
      </button>
      {!collapsed && activeCategories.map(cat => {
        const cfg = CATEGORY_CONFIG[cat] || CATEGORY_CONFIG.general;
        return (
          <div key={cat} style={{
            display: 'flex', alignItems: 'center', gap: 7,
            marginBottom: 4, fontSize: 12, color: '#cbd5e1',
          }}>
            <div style={{
              width: 10, height: 10, borderRadius: '50%',
              background: cfg.color, flexShrink: 0,
              boxShadow: `0 0 6px ${cfg.color}66`,
            }} />
            {cfg.label}
          </div>
        );
      })}
    </div>
  );
};

const FlowInner = () => {
  const { t } = useTheme();
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedNodes, setSelectedNodes] = useState([]);
  const [isPanMode, setIsPanMode] = useState(true);
  const [hasPendingRecommendations, setHasPendingRecommendations] = useState(false);
  const [recommendationNodes, setRecommendationNodes] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isMerging, setIsMerging] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);
  const [isRelinking, setIsRelinking] = useState(false);
  const previousDataRef = useRef(null);

  // Compute unique categories currently in the graph for the legend
  const activeCategories = useMemo(
    () => [...new Set(nodes.map(n => n.data?.category).filter(Boolean))],
    [nodes]
  );

  const reactFlowInstance = useReactFlow();

  const fetchData = useCallback(async () => {
    try {
      setIsRefreshing(true);
      const data = await fetchGraphData();

      const dataString = JSON.stringify(data);
      if (dataString === JSON.stringify(previousDataRef.current)) {
        setIsRefreshing(false);
        return;
      }

      previousDataRef.current = data;

      if (data) {
        const initialNodes = data.nodes.map(node => ({
          id: node.uuid,
          type: 'markdown',
          data: {
            label: node.title,
            content: node.title,
            isRecommendation: false,
            size: 48,
            isSelected: false,
            category: node.category || 'general',
            subcategory: node.subcategory || 'knowledge',
          }
        }));

        const initialEdges = data.edges.map(edge => {
          const score = typeof edge.score === 'number' ? edge.score : 0;
          // Stronger connections = thicker + more opaque
          const opacity = Math.max(0.25, Math.min(1, score * 1.2));
          const width = score > 0.6 ? 2.5 : score > 0.4 ? 2 : 1.5;
          return {
            id: `edge-${edge.source}-${edge.target}`,
            source: edge.source,
            target: edge.target,
            sourceHandle: 'source',
            targetHandle: 'target',
            type: 'straight',
            animated: false,
            label: edge.label || '',
            labelStyle: { fontSize: 11, fill: '#ffffff', fontWeight: 400, fontFamily: 'Inter, sans-serif' },
            labelBgStyle: { fill: '#16162a', fillOpacity: 1 },
            labelBgPadding: [5, 3],
            labelBgBorderRadius: 4,
            style: {
              stroke: `rgba(255, 122, 61, ${opacity})`,
              strokeWidth: width,
              zIndex: 1,
            },
          };
        });

        const { nodes: layoutedNodes, edges: layoutedEdges } = getForceLayoutedElements(
          initialNodes,
          initialEdges
        );

        requestAnimationFrame(() => {
          setNodes(layoutedNodes);
          setEdges(layoutedEdges);
          // Fit viewport after nodes are set so they're visible
          setTimeout(() => {
            reactFlowInstance.fitView({ padding: 0.5, duration: 400 });
          }, 80);
        });
      }
    } catch (error) {
      console.error('Error fetching graph data:', error);
    } finally {
      setIsRefreshing(false);
    }
  }, [setNodes, setEdges]);

  const debouncedRefresh = useCallback(
    debounce(() => {
      fetchData();
    }, 300),
    [fetchData]
  );

  // ── AI Relink: discover missing connections across entire graph ──
  const handleRelink = useCallback(async () => {
    setIsRelinking(true);
    setToastMessage('Analyzing node relationships with AI...');
    try {
      const result = await relinkGraph();
      const count = result?.newEdgesCreated ?? 0;
      setToastMessage(count > 0 ? `Found ${count} new connection${count > 1 ? 's' : ''}!` : 'No new connections found.');
      if (count > 0) fetchData();
    } catch (err) {
      console.error('Relink failed:', err);
      setToastMessage('Relink failed. Check console.');
    } finally {
      setIsRelinking(false);
    }
  }, [fetchData]);

  const { handleRecommendation, handleConfirmRecommendation, handleDeleteNode } = useNodeManagement({
    nodes,
    setNodes,
    edges,
    setEdges,
    selectedNode,
    setSelectedNode,
    hasPendingRecommendations,
    setHasPendingRecommendations,
    recommendationNodes,
    setRecommendationNodes,
    fetchData: debouncedRefresh,
    setIsLoading
  });

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    console.log('Edges updated:', edges);
  }, [edges]);

  const convertToVideoUrl = (videoId) => {
    const baseurl = "https://www.videoindexer.ai/embed/player/914a5e40-8e73-4e7a-8d13-ff0193a05e75/videoId/?&locale=en&location=trial";
    return baseurl.replace('videoId', videoId);
  }

  const onNodeClick = useCallback(async (event, node) => {
    if (!isPanMode) {
      event.preventDefault();
      event.stopPropagation();


      if (selectedNodes.includes(node.id)) {
        const newSelectedNodes = selectedNodes.filter(id => id !== node.id);
        setSelectedNodes(newSelectedNodes);
        setNodes(nds =>
          nds.map(n => ({
            ...n,
            data: {
              ...n.data,
              isSelected: newSelectedNodes.includes(n.id)
            }
          }))
        );
      } else {

        if (selectedNodes.length < 3) {
          const newSelectedNodes = [...selectedNodes, node.id];
          setSelectedNodes(newSelectedNodes);
          setNodes(nds =>
            nds.map(n => ({
              ...n,
              data: {
                ...n.data,
                isSelected: newSelectedNodes.includes(n.id)
              }
            }))
          );
        } else {
          setToastMessage('Maximum 3 nodes can be selected for demo');
        }
      }
      return;
    }

    if (node.data.isRecommendation) {
      setSelectedNode(node);
      return;
    }

    try {
      let detail = await fetchNodeDetail(node.id);
      const extra_formed = {
        ...detail.extra,
        video_urls: [
          ...(detail.extra.video_ids ? detail.extra.video_ids.map(item => convertToVideoUrl(item)) : []),
          ...(detail.extra.youtube_urls || [])
        ],
      }
      detail = {
        ...detail,
        extra: extra_formed
      }
      setSelectedNode({
        ...node,
        detail,
      });
    } catch (error) {
      console.error('Error fetching node detail:', error);
    }
  }, [isPanMode, selectedNodes, setNodes]);

  const onSelectionChange = useCallback(({ nodes: selectedNodes }) => {
    if (!isPanMode) {
      const selectedIds = selectedNodes.map(node => node.id);
      setSelectedNodes(selectedIds);
      setNodes(nds =>
        nds.map(n => ({
          ...n,
          data: {
            ...n.data,
            isSelected: selectedIds.includes(n.id)
          }
        }))
      );
    }
  }, [isPanMode, setNodes]);

  const onNodeDrag = useCallback((event, node) => {
    setNodes((nds) =>
      nds.map((n) => {
        if (n.id === node.id) {
          return {
            ...n,
            position: node.position,
          };
        }
        return n;
      })
    );
  }, [setNodes]);

  const onNodeDragStop = useCallback((event, node) => {
    const newPosition = node.position;
    nodePositions.set(node.id, newPosition);

    setNodes((nds) =>
      nds.map((n) => {
        if (n.id === node.id) {
          return {
            ...n,
            position: newPosition,
          };
        }
        return n;
      })
    );

    setEdges((eds) =>
      eds.map((edge) => {
        if (edge.source === node.id || edge.target === node.id) {
          return {
            ...edge,
            id: `${edge.id}-${Date.now()}`,
          };
        }
        return edge;
      })
    );
  }, [setNodes, setEdges]);

  const handleModeChange = useCallback((isPan) => {
    setIsPanMode(isPan);
    setSelectedNode(null);
    setSelectedNodes([]);
    setNodes((nds) =>
      nds.map((n) => ({
        ...n,
        data: {
          ...n.data,
          isSelected: false
        }
      }))
    );
  }, [setNodes]);

  const mergeNodes = useCallback(async () => {
    try {
      setIsMerging(true);
      const selectedNodes = nodes.filter(node => node.data.isSelected);

      if (selectedNodes.length < 2) {
        setToastMessage('Please select at least 2 nodes to merge');
        return;
      }

      if (selectedNodes.length > 3) {
        setToastMessage('Too many nodes to merge. Max allowed: 3');
        return;
      }

      const nodeIds = selectedNodes.map(node => node.id);
      const result = await mergeNodesApi(nodeIds);

      await fetchData();

      setSelectedNodes([]);
      setNodes(nds =>
        nds.map(n => ({
          ...n,
          data: {
            ...n.data,
            isSelected: false
          }
        }))
      );

      setToastMessage('Nodes merged successfully');
    } catch (error) {
      console.error('Error merging nodes:', error);
      setToastMessage(error.message || 'Failed to merge nodes');
    } finally {
      setIsMerging(false);
    }
  }, [nodes, fetchData]);

  window.refreshFlow = debouncedRefresh;
  window.fitGraphView = () => {
    setTimeout(() => {
      reactFlowInstance.fitView({ padding: 0.5, duration: 400 });
    }, 200);
  };
  window.getSelectedNode = () => selectedNode;
  window.setSelectedNode = (node) => {
    if (isPanMode && node) {
      setSelectedNode(node);
    }
  };
  window.clearRecommendations = () => {
    setNodes(nds => nds.filter(n => !n.data.isRecommendation));
    setRecommendationNodes([]);
    setHasPendingRecommendations(false);
  };

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <style>
        {`
          @keyframes bounce {
            0% {
              transform: translateY(0);
            }
            100% {
              transform: translateY(-20px);
            }
          }

          @keyframes pulse {
            0% {
              transform: scale(0.95);
              opacity: 0.5;
            }
            50% {
              transform: scale(1);
              opacity: 0.8;
            }
            100% {
              transform: scale(0.95);
              opacity: 0.5;
            }
          }

          .react-flow__node {
            transition: none;
          }

          .react-flow__edge {
            transition: none;
          }

          .react-flow__node.selected {
            box-shadow: 0 0 0 2px rgba(255,122,61,0.5);
          }

          .react-flow__node:hover {
            box-shadow: 0 0 0 2px rgba(255,122,61,0.35);
          }

          .react-flow__edge-path {
            transition: none;
          }

          .loading-overlay {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: rgba(26, 26, 46, 0.7);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 1000;
            opacity: 0;
            transition: opacity 0.3s ease-in-out;
          }

          .loading-overlay.visible {
            opacity: 1;
          }

          .loading-spinner {
            width: 40px;
            height: 40px;
            border: 3px solid #FF7A3D;
            border-top: 3px solid transparent;
            border-radius: 50%;
            animation: spin 1s linear infinite;
          }

          @keyframes spin {
            0% {
              transform: rotate(0deg);
            }
            100% {
              transform: rotate(360deg);
            }
          }

          .node-details-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 70%;
            height: 100%;
            background-color: rgba(26, 26, 46, 0.95);
            z-index: 1000;
            transition: opacity 0.3s ease-in-out;
          }

          .mode-switch {
            position: absolute;
            top: 20px;
            right: 20px;
            z-index: 1000;
            display: flex;
            gap: 8px;
          }
          .mode-button {
            padding: 8px 16px;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            font-family: 'Inter, sans-serif';
            display: flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          }
          .mode-button.active {
            background-color: rgba(255,122,61,0.9);
            color: #fffffe;
          }
          .mode-button:not(.active) {
            background-color: rgba(30,30,50,0.85);
            color: #FFa06a;
            border: 1px solid rgba(255,122,61,0.3);
          }
          @keyframes slideIn {
            from {
              transform: translate(-50%, -100%);
              opacity: 0;
            }
            to {
              transform: translate(-50%, 0);
              opacity: 1;
            }
          }
        `}
      </style>

      <div className="wrapper">
        <div className="option">
          <input
            className="input"
            type="radio"
            name="mode"
            value="pan"
            checked={isPanMode}
            onChange={() => handleModeChange(true)}
          />
          <div className="btn">
            <span className="span">Discover</span>
          </div>
        </div>
        <div className="option">
          <input
            className="input"
            type="radio"
            name="mode"
            value="merge"
            checked={!isPanMode}
            onChange={() => handleModeChange(false)}
          />
          <div className="btn">
            <span className="span">Merge</span>
          </div>
        </div>
      </div>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        onNodeDrag={onNodeDrag}
        onNodeDragStop={onNodeDragStop}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.5 }}
        proOptions={{ hideAttribution: true }}
        nodesDraggable={isPanMode}
        nodesConnectable={false}
        elementsSelectable={false}
        selectionMode={SelectionMode.Full}
        panOnDrag={isPanMode}
        zoomOnScroll={true}
        zoomOnDoubleClick={true}
        selectionOnDrag={false}
        selectionKeyCode={null}
        multiSelectionKeyCode={null}
        nodesFocusable={false}
        edgesFocusable={false}
        edgesUpdatable={false}
        onInit={(instance) => {
          // Let fitView handle viewport positioning
        }}
        style={{ backgroundColor: t.bgCanvas }}
        defaultEdgeOptions={{
          type: 'straight',
          animated: false,
          style: {
            stroke: 'rgba(255, 122, 61, 0.45)',
            strokeWidth: 2,
            zIndex: 1
          }
        }}
        minZoom={0.1}
        maxZoom={4}
        defaultViewport={{ x: 0, y: 0, zoom: 1 }}
      >
        <Background color={t.bgDot} gap={20} size={1.5} />
        {/* <VantaBackground /> */}
        {/* <Controls /> */}
        {/* <MiniMap /> */}
        {selectedNode && isPanMode && (
          <div>
            <div style={{ pointerEvents: 'auto' }}>
              <NodeDetails
                selectedNode={selectedNode}
                onClose={() => setSelectedNode(null)}
                onRecommend={handleRecommendation}
                onConfirm={handleConfirmRecommendation}
                onDelete={handleDeleteNode}
                hasRecommendations={selectedNode?.data?.isRecommendation}
                hasPendingRecommendations={hasPendingRecommendations}
                isLoading={isLoading}
              />
            </div>
          </div>
        )}
      </ReactFlow>

      {/* ── Category Cluster Legend ── */}
      {activeCategories.length > 0 && (
        <ClusterLegend activeCategories={activeCategories} />
      )}

      {/* ── AI Relink Button ── */}
      {nodes.length >= 2 && (
        <button
          onClick={handleRelink}
          disabled={isRelinking}
          title="AI finds missing connections between existing nodes"
          style={{
            position: 'absolute',
            bottom: 56,
            left: 12,
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            padding: '7px 14px',
            background: isRelinking ? 'rgba(255,122,61,0.15)' : 'rgba(30,30,50,0.95)',
            border: '1px solid rgba(255,122,61,0.2)',
            borderRadius: 10,
            fontSize: 12,
            fontWeight: 600,
            color: isRelinking ? '#FF7A3D' : '#FFa06a',
            cursor: isRelinking ? 'wait' : 'pointer',
            boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
            backdropFilter: 'blur(12px)',
            transition: 'all 0.2s',
            opacity: isRelinking ? 0.8 : 1,
          }}
        >
          {isRelinking ? (
            <>
              <span style={{
                display: 'inline-block', width: 12, height: 12,
                border: '2px solid #FF7A3D', borderTopColor: 'transparent',
                borderRadius: '50%', animation: 'spin 0.8s linear infinite',
              }} />
              Analyzing...
            </>
          ) : (
            <>🔗 Relink Nodes</>
          )}
        </button>
      )}

      <div className={`loading-overlay ${isLoading ? 'visible' : ''}`} style={{ pointerEvents: 'none' }}>
        <div className="loading-spinner" />
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', pointerEvents: 'none' }}>
        <UploadBox
          onUploadSuccess={debouncedRefresh}
          isLoading={isLoading}
          setIsLoading={setIsLoading}
          selectedNodes={selectedNodes}
          mergeNodes={mergeNodes}
          isMerging={isMerging}
        />

        {toastMessage && (
          <Toast
            message={toastMessage}
            onClose={() => setToastMessage(null)}
          />
        )}
      </div>
    </div>
  );
};

export const Flow = () => (
  <ReactFlowProvider>
    <FlowInner />
  </ReactFlowProvider>
);