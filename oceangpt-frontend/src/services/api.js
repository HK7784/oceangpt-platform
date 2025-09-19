import axios from 'axios';

const rawBase = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const API_BASE_URL = rawBase.replace(/\/+$/, '');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    console.log('发送请求:', config);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    console.log('收到响应:', response);
    return response;
  },
  (error) => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 水质预测API
export const predictWaterQuality = async (predictionData) => {
  try {
    const response = await api.post('/api/v1/water-quality/predict', predictionData);
    return response.data;
  } catch (error) {
    throw new Error(`预测请求失败: ${error.response?.data?.message || error.message}`);
  }
};

// 生成报告API
export const generateReport = async (reportData) => {
  try {
    const response = await api.post('/api/v1/water-quality/analyze/report', reportData);
    return response.data;
  } catch (error) {
    throw new Error(`报告生成失败: ${error.response?.data?.message || error.message}`);
  }
};

// 健康检查API
export const healthCheck = async () => {
  try {
    const response = await api.get('/api/actuator/health');
    return response.data;
  } catch (error) {
    throw new Error(`健康检查失败: ${error.response?.data?.message || error.message}`);
  }
};

// 聊天对话API
export const chatWithOceanGPT = async (message, sessionId = null) => {
  try {
    const requestData = {
      message: message,
      sessionId: sessionId || `session_${Date.now()}`,
      language: 'zh'
    };
    const response = await api.post('/api/v1/chat/message', requestData);
    return response.data;
  } catch (error) {
    throw new Error(`聊天请求失败: ${error.response?.data?.message || error.message}`);
  }
};

// 获取聊天历史API
export const getChatHistory = async (sessionId) => {
  try {
    const response = await api.get(`/api/v1/chat/history/${sessionId}`);
    return response.data;
  } catch (error) {
    throw new Error(`获取聊天历史失败: ${error.response?.data?.message || error.message}`);
  }
};

// 清除聊天历史API
export const clearChatHistory = async (sessionId) => {
  try {
    const response = await api.delete(`/api/v1/chat/history/${sessionId}`);
    return response.data;
  } catch (error) {
    throw new Error(`清除聊天历史失败: ${error.response?.data?.message || error.message}`);
  }
};

export default api;