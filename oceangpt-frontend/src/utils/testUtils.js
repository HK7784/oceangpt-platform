import React from 'react';
import { render } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// 创建测试主题
const testTheme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
});

// 自定义渲染函数，包含主题提供者
export const renderWithTheme = (ui, options = {}) => {
  const Wrapper = ({ children }) => (
    <ThemeProvider theme={testTheme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );

  return render(ui, { wrapper: Wrapper, ...options });
};

// 模拟WebSocket客户端
export const mockStompClient = {
  activate: jest.fn(),
  deactivate: jest.fn(),
  publish: jest.fn(),
  subscribe: jest.fn(),
  onConnect: null,
  onStompError: null,
  onWebSocketError: null,
  onDisconnect: null
};

// 模拟水质数据
export const mockWaterQualityData = {
  id: 1,
  latitude: 36.0544,
  longitude: 120.3822,
  temperature: 18.5,
  salinity: 34.2,
  ph: 8.1,
  dissolvedOxygen: 7.8,
  chlorophyll: 2.5,
  quality: 'good',
  lastUpdate: '2024-01-15 10:30:00',
  predictionResult: {
    din: 0.125,
    srp: 0.032,
    ph: 8.1,
    qualityLevel: '良好',
    confidence: 0.85
  }
};

// 模拟聊天消息
export const mockChatMessage = {
  id: 1,
  type: 'bot',
  content: '您好！我是OceanGPT，您的海洋数据分析助手。',
  timestamp: new Date(),
  confidence: 0.9,
  suggestions: ['分析水质数据', '生成预测报告'],
  steps: ['数据处理', '模型预测', '结果分析'],
  predictionResult: mockWaterQualityData.predictionResult
};

// 模拟API响应
export const mockApiResponse = {
  success: true,
  data: mockWaterQualityData,
  message: '操作成功'
};

// 延迟函数，用于模拟异步操作
export const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// 模拟地理位置数据
export const mockLocationData = [
  {
    id: 1,
    name: '青岛近海监测点1',
    latitude: 36.0544,
    longitude: 120.3822,
    ...mockWaterQualityData
  },
  {
    id: 2,
    name: '大连近海监测点1',
    latitude: 38.9140,
    longitude: 121.6147,
    temperature: 16.8,
    salinity: 33.9,
    ph: 8.0,
    quality: 'moderate'
  }
];

export default {
  renderWithTheme,
  mockStompClient,
  mockWaterQualityData,
  mockChatMessage,
  mockApiResponse,
  mockLocationData,
  delay
};