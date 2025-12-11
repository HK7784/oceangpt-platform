import React, { useState, useEffect, useRef } from 'react';
import {
  Paper,
  TextField,
  Typography,
  Box,
  Avatar,
  IconButton,
  CircularProgress,
  Chip,
  Card,
  CardContent,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid
} from '@mui/material';
import {
  Send as SendIcon,
  SmartToy as BotIcon,
  Person as UserIcon,
  Clear as ClearIcon,
  Refresh as RefreshIcon,
  ExpandMore as ExpandMoreIcon,
  CheckCircle as CheckCircleIcon,
  Assessment as AssessmentIcon,
  Map as MapIcon
} from '@mui/icons-material';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import L from 'leaflet';
import '../styles/ChatPage.css';
// import { chatWithOceanGPT, getChatHistory } from '../services/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const ChatPage = () => {
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: 'bot',
      content: '您好！我是OceanGPT，您的海洋数据分析助手。我现在集成了多种功能，可以帮您：\n\n🌊 分析海洋水质数据\n📊 生成水质预测报告\n🗺️ 地图可视化监测点\n📈 实时数据分析\n🔍 智能问答解答\n\n所有功能都可以通过自然语言对话来操作，请问有什么可以帮助您的吗？',
      timestamp: new Date(),
      suggestions: [
        '显示青岛海域的监测点分布',
        '预测未来一周的水质变化',
        '生成当前海域水质报告',
        '分析DIN和SRP浓度趋势'
      ]
    }
  ]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId] = useState(() => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
  const [stompClient, setStompClient] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);
  // eslint-disable-next-line no-unused-vars
  const [showMap, setShowMap] = useState(false);
  // eslint-disable-next-line no-unused-vars
  const [mapData, setMapData] = useState([]);

  // 修复 Leaflet 默认图标问题
  useEffect(() => {
    delete L.Icon.Default.prototype._getIconUrl;
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
      iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
      shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
    });
  }, []);

  // 获取地图质量颜色
  const getQualityColor = (quality) => {
    // 支持新的水质等级分类
    switch (quality) {
      // 新的水质等级分类
      case '一级': return '#00FF00';
      case '二级': return '#7FFF00';
      case '三级': return '#FFFF00';
      case '四级': return '#FFA500';
      case '劣四级': return '#FF0000';
      // 兼容旧的分类
      case 'excellent': return '#4caf50';
      case 'good': return '#8bc34a';
      case 'moderate': return '#ff9800';
      case 'poor': return '#f44336';
      default: return '#9e9e9e';
    }
  };
  
  // 获取水质等级标签
  const getQualityLabel = (quality) => {
    switch (quality) {
      case '一级': return '一级 (优秀)';
      case '二级': return '二级 (良好)';
      case '三级': return '三级 (一般)';
      case '四级': return '四级 (较差)';
      case '劣四级': return '劣四级 (极差)';
      case 'excellent': return '优秀';
      case 'good': return '良好';
      case 'moderate': return '中等';
      case 'poor': return '较差';
      default: return quality || '未知';
    }
  };

  // 根据地图数据计算地图中心点
  const getMapCenter = (mapData) => {
    if (!mapData || mapData.length === 0) {
      return [36.0544, 120.3822]; // 默认青岛坐标
    }
    
    // 兼容两种数据格式：lat/lng 和 latitude/longitude
    const avgLat = mapData.reduce((sum, point) => {
      return sum + (point.lat || point.latitude || 0);
    }, 0) / mapData.length;
    
    const avgLng = mapData.reduce((sum, point) => {
      return sum + (point.lng || point.longitude || 0);
    }, 0) / mapData.length;
    
    return [avgLat, avgLng];
  };

  // 根据地图数据计算合适的缩放级别
  const getMapZoom = (mapData) => {
    if (!mapData || mapData.length === 0) {
      return 8; // 默认缩放级别
    }
    
    if (mapData.length === 1) {
      return 10; // 单个点时放大显示
    }
    
    // 兼容两种数据格式：lat/lng 和 latitude/longitude
    const lats = mapData.map(point => point.lat || point.latitude || 0);
    const lngs = mapData.map(point => point.lng || point.longitude || 0);
    const latRange = Math.max(...lats) - Math.min(...lats);
    const lngRange = Math.max(...lngs) - Math.min(...lngs);
    const maxRange = Math.max(latRange, lngRange);
    
    // 根据范围确定缩放级别（针对全国范围优化）
    if (maxRange > 15) return 4;  // 全国范围
    if (maxRange > 8) return 5;   // 大区域范围
    if (maxRange > 4) return 6;   // 省级范围
    if (maxRange > 2) return 7;   // 市级范围
    if (maxRange > 1) return 8;
    if (maxRange > 0.5) return 9;
    if (maxRange > 0.2) return 10;
    if (maxRange > 0.1) return 11;
    return 12;
  };

  useEffect(() => {
    // 统一与 REST 相同的回退策略，确保线上可用
    let apiBase = process.env.REACT_APP_API_URL || 'https://oceangpt-platform.onrender.com';
    while (apiBase.endsWith('/')) apiBase = apiBase.slice(0, -1);
    // 防止双重 /api 路径 (如果环境变量中包含了 /api)
    if (apiBase.endsWith('/api')) apiBase = apiBase.slice(0, -4);
    
    const sockJsUrl = `${apiBase}/api/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl, null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling']
      }),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000, // 增加心跳间隔
      heartbeatOutgoing: 10000,
      connectionTimeout: 60000, // 增加连接超时到60秒
      debug: function (str) {
        console.log('STOMP Debug: ' + str);
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      }
    });

    client.onConnect = () => {
      console.log('WebSocket connected successfully');
      setStompClient(client);
      setIsConnected(true);
      
      client.subscribe(`/topic/chat.${sessionId}`, (message) => {
        console.log('Received message:', message.body);
        try {
          const receivedMessage = JSON.parse(message.body);
          
          // 确保content始终是字符串
          let messageContent = receivedMessage.message;
          if (typeof messageContent === 'object' && messageContent !== null) {
            messageContent = JSON.stringify(messageContent, null, 2);
          } else if (typeof messageContent !== 'string') {
            messageContent = String(messageContent);
          }
          
          const botMessage = {
            id: Date.now(),
            type: 'bot',
            content: messageContent,
            timestamp: new Date(),
            confidence: receivedMessage.confidence,
            suggestions: receivedMessage.followUpQueries,
            steps: receivedMessage.steps,
            relatedData: receivedMessage.relatedData,
            technicalDetails: receivedMessage.technicalDetails,
            predictionResult: receivedMessage.relatedData?.predictionResult,
            reportId: receivedMessage.relatedData?.reportId,
            satelliteData: receivedMessage.relatedData?.satelliteData,
            mapData: receivedMessage.relatedData?.mapData
          };
          
          // 如果消息包含地图数据，显示地图
          if (receivedMessage.relatedData?.mapData) {
            setMapData(receivedMessage.relatedData.mapData);
            setShowMap(true);
          }
          
          setMessages(prev => [...prev, botMessage]);
          setIsLoading(false);
        } catch (error) {
          console.error('Error parsing message:', error);
          setIsLoading(false);
        }
      });

      client.publish({
        destination: `/app/chat.addUser`,
        body: JSON.stringify({ 
          sessionId: sessionId,
          userId: 'user_' + sessionId,
          sender: 'user', 
          type: 'JOIN' 
        })
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      setIsConnected(false);
      // 显示错误消息给用户
      const errorMessage = {
        id: Date.now(),
        type: 'bot',
        content: '连接出现问题，正在尝试重新连接...',
        timestamp: new Date(),
        isError: true
      };
      setMessages(prev => [...prev, errorMessage]);
    };

    client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
      setIsConnected(false);
    };

    client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      setStompClient(null);
      setIsConnected(false);
    };

    client.activate();

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, [sessionId]);

  // 自动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 发送消息
  const handleSendMessage = async (message = inputMessage) => {
    if (!message.trim()) {
      console.log('Empty message, not sending');
      return;
    }
    
    if (isLoading) {
      console.log('Already loading, not sending');
      return;
    }
    
    if (!stompClient || !isConnected) {
      console.error('STOMP client not connected');
      // 显示连接状态提示
      const errorMessage = {
        id: Date.now(),
        type: 'bot',
        content: '连接已断开，正在尝试重新连接...',
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
      setIsLoading(false);
      return;
    }

    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: message.trim(),
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    const messagePayload = { 
      message: message.trim(), 
      sessionId: sessionId, 
      userId: 'user_' + sessionId,
      language: 'zh'
    };
    
    console.log('Sending message:', messagePayload);
    
    try {
      stompClient.publish({
        destination: "/app/chat.sendMessage",
        body: JSON.stringify(messagePayload)
      });
      console.log('Message sent successfully');
    } catch (error) {
      console.error('Error sending message:', error);
      setIsLoading(false);
    }

    // The bot's response will be handled by the WebSocket subscription,
    // so we can remove the old API call and response handling.
    // We'll set isLoading to false in the subscription callback when the bot's message is received.
  };

  // 处理键盘事件
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // 清空对话
  const handleClearChat = () => {
    setMessages([
      {
        id: 1,
        type: 'bot',
        content: '对话已清空。有什么新的问题需要我帮助您吗？',
        timestamp: new Date()
      }
    ]);
  };

  // 使用建议问题
  const handleSuggestionClick = (suggestion) => {
    handleSendMessage(suggestion);
  };

  // 格式化时间
  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <Box data-testid="chat-page" sx={{ height: '80vh', display: 'flex', flexDirection: 'column' }}>
      {/* 聊天标题栏 */}
      <Paper elevation={1} sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar sx={{ bgcolor: 'primary.main' }}>
              <BotIcon />
            </Avatar>
            <Box>
              <Typography variant="h6">OceanGPT 智能助手</Typography>
              <Typography variant="caption" color={isConnected ? "success.main" : "error.main"}>
                海洋数据分析专家 • {isConnected ? '已连接' : '连接中...'}
              </Typography>
            </Box>
          </Box>
          <Box>
            <IconButton onClick={handleClearChat} title="清空对话">
              <ClearIcon />
            </IconButton>
            <IconButton onClick={() => window.location.reload()} title="刷新">
              <RefreshIcon />
            </IconButton>
          </Box>
        </Box>
      </Paper>

      {/* 消息列表 */}
      <Paper 
        elevation={1} 
        sx={{ 
          flex: 1, 
          overflow: 'auto', 
          p: 2, 
          mb: 2,
          backgroundColor: '#fafafa'
        }}
      >
        {messages.map((message) => (
          <Box key={message.id} sx={{ mb: 2 }}>
            <Box
              sx={{
                display: 'flex',
                justifyContent: message.type === 'user' ? 'flex-end' : 'flex-start',
                alignItems: 'flex-start',
                gap: 1
              }}
            >
              {message.type === 'bot' && (
                <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
                  <BotIcon fontSize="small" />
                </Avatar>
              )}
              
              <Box sx={{ maxWidth: '70%' }}>
                <Paper
                  elevation={message.type === 'user' ? 2 : 1}
                  sx={{
                    p: 2,
                    backgroundColor: message.type === 'user' ? 'primary.main' : 'grey.50',
                    color: message.type === 'user' ? 'white' : 'text.primary',
                    borderRadius: message.type === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                    border: message.type === 'user' ? 'none' : '1px solid',
                    borderColor: message.type === 'user' ? 'transparent' : 'grey.200',
                    transition: 'all 0.2s ease-in-out',
                    '&:hover': {
                      transform: 'translateY(-1px)',
                      boxShadow: message.type === 'user' ? 3 : 2,
                    },
                  }}
                >
                  <Typography 
                    variant="body1" 
                    sx={{ 
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word'
                    }}
                  >
                    {message.content}
                  </Typography>
                  
                  {/* 置信度显示 */}
                  {message.confidence && (
                    <Box sx={{ mt: 1 }}>
                      <Chip 
                        size="small" 
                        label={`置信度: ${(message.confidence * 100).toFixed(0)}%`}
                        color={message.confidence > 0.8 ? 'success' : 'warning'}
                      />
                    </Box>
                  )}
                  
                  {/* 处理步骤显示 */}
                  {message.steps && message.steps.length > 0 && (
                    <Accordion sx={{ mt: 1 }}>
                      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography variant="body2">处理步骤 ({message.steps.length})</Typography>
                      </AccordionSummary>
                      <AccordionDetails>
                        <Box>
                          {message.steps.map((step, index) => (
                            <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                              <CheckCircleIcon color="success" sx={{ mr: 1, fontSize: 16 }} />
                              <Typography variant="body2">{step}</Typography>
                            </Box>
                          ))}
                        </Box>
                      </AccordionDetails>
                    </Accordion>
                  )}
                  
                  {/* 预测结果可视化 */}
                  {message.predictionResult && (
                    <Card sx={{ mt: 1 }}>
                      <CardContent>
                        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                          <AssessmentIcon color="primary" sx={{ mr: 1 }} />
                          <Typography variant="h6">预测结果</Typography>
                        </Box>
                        
                        <Grid container spacing={2}>
                          <Grid item xs={6}>
                            <Box sx={{ textAlign: 'center', p: 1, bgcolor: 'primary.light', borderRadius: 1 }}>
                              <Typography variant="body2" color="white">DIN浓度</Typography>
                              <Typography variant="h6" color="white">
                                {message.predictionResult.din?.toFixed(3)} mg/L
                              </Typography>
                            </Box>
                          </Grid>
                          <Grid item xs={6}>
                            <Box sx={{ textAlign: 'center', p: 1, bgcolor: 'secondary.light', borderRadius: 1 }}>
                              <Typography variant="body2" color="white">SRP浓度</Typography>
                              <Typography variant="h6" color="white">
                                {message.predictionResult.srp?.toFixed(3)} mg/L
                              </Typography>
                            </Box>
                          </Grid>
                          <Grid item xs={6}>
                            <Box sx={{ textAlign: 'center', p: 1, bgcolor: 'info.light', borderRadius: 1 }}>
                              <Typography variant="body2" color="white">pH值</Typography>
                              <Typography variant="h6" color="white">
                                {message.predictionResult.ph?.toFixed(2)}
                              </Typography>
                            </Box>
                          </Grid>
                          <Grid item xs={6}>
                            <Box sx={{ 
                              textAlign: 'center', 
                              p: 1, 
                              bgcolor: getQualityColor(message.predictionResult.waterQualityLevel || message.predictionResult.qualityLevel), 
                              borderRadius: 1 
                            }}>
                              <Typography variant="body2" color="white">水质等级</Typography>
                              <Typography variant="h6" color="white">
                                {getQualityLabel(message.predictionResult.waterQualityLevel || message.predictionResult.qualityLevel || '三级')}
                              </Typography>
                            </Box>
                          </Grid>
                        </Grid>
                        
                        {message.predictionResult.confidence && (
                          <Box sx={{ mt: 2, textAlign: 'center' }}>
                            <Chip 
                              label={`预测置信度: ${(message.predictionResult.confidence * 100).toFixed(1)}%`}
                              color={message.predictionResult.confidence > 0.8 ? 'success' : 'warning'}
                            />
                          </Box>
                        )}
                      </CardContent>
                    </Card>
                  )}
                  
                  {/* 卫星数据信息 */}
                  {message.satelliteData && (
                    <Accordion sx={{ mt: 1 }}>
                      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Typography variant="body2">卫星数据详情</Typography>
                      </AccordionSummary>
                      <AccordionDetails>
                        <Grid container spacing={1}>
                          <Grid item xs={6}>
                            <Typography variant="caption" color="text.secondary">卫星来源</Typography>
                            <Typography variant="body2">{message.satelliteData.source}</Typography>
                          </Grid>
                          <Grid item xs={6}>
                            <Typography variant="caption" color="text.secondary">采集时间</Typography>
                            <Typography variant="body2">{new Date(message.satelliteData.timestamp).toLocaleDateString()}</Typography>
                          </Grid>
                          <Grid item xs={12}>
                            <Typography variant="caption" color="text.secondary">覆盖区域</Typography>
                            <Typography variant="body2">{message.satelliteData.region}</Typography>
                          </Grid>
                        </Grid>
                      </AccordionDetails>
                    </Accordion>
                  )}
                  
                  <Typography variant="caption" sx={{ display: 'block', mt: 1, textAlign: 'right', opacity: 0.7 }}>
                    {formatTime(message.timestamp)}
                  </Typography>
                </Paper>
              </Box>
              
              {message.type === 'user' && (
                <Avatar sx={{ bgcolor: 'secondary.main', width: 32, height: 32 }}>
                  <UserIcon fontSize="small" />
                </Avatar>
              )}
            </Box>
          </Box>
        ))}
        <div ref={messagesEndRef} />
      </Paper>

      {/* 地图显示区域 */}
      {showMap && mapData && mapData.length > 0 && (
        <Paper elevation={1} sx={{ height: '300px', mb: 2, overflow: 'hidden' }}>
          <MapContainer 
            center={getMapCenter(mapData)} 
            zoom={getMapZoom(mapData)} 
            style={{ height: '100%', width: '100%' }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            />
            {mapData.map((point, index) => (
              <Marker 
                key={index} 
                position={[point.lat || point.latitude, point.lng || point.longitude]}
              >
                <Popup>
                  <Typography variant="subtitle2">{point.name || '监测点'}</Typography>
                  <Typography variant="body2">
                    水质: {getQualityLabel(point.quality)}
                  </Typography>
                  {point.value && (
                    <Typography variant="body2">
                      数值: {point.value}
                    </Typography>
                  )}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </Paper>
      )}

      {/* 输入区域 */}
      <Paper elevation={1} sx={{ p: 2, mt: 'auto' }}>
        {/* 建议问题 */}
        {messages.length > 0 && messages[messages.length - 1].suggestions && (
          <Box sx={{ mb: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {messages[messages.length - 1].suggestions.map((suggestion, index) => (
              <Chip
                key={index}
                label={suggestion}
                onClick={() => handleSuggestionClick(suggestion)}
                variant="outlined"
                color="primary"
                size="small"
                sx={{ cursor: 'pointer' }}
              />
            ))}
          </Box>
        )}

        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField
            fullWidth
            placeholder="输入您的问题，例如：分析青岛海域的水质情况..."
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={isLoading}
            variant="outlined"
            size="small"
            multiline
            maxRows={4}
          />
          <IconButton 
            color="primary" 
            onClick={() => handleSendMessage()}
            disabled={!inputMessage.trim() || isLoading || !isConnected}
            sx={{ 
              bgcolor: 'primary.main', 
              color: 'white',
              '&:hover': { bgcolor: 'primary.dark' },
              '&.Mui-disabled': { bgcolor: 'action.disabledBackground' }
            }}
          >
            {isLoading ? <CircularProgress size={24} color="inherit" /> : <SendIcon />}
          </IconButton>
        </Box>
      </Paper>
    </Box>
  );
};

export default ChatPage;
