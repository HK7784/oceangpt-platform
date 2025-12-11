import axios from 'axios';

const getBaseUrl = () => {
  let url = process.env.REACT_APP_API_URL || ((typeof window !== 'undefined' && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')) ? 'http://localhost:8080' : 'https://oceangpt-platform.onrender.com');
  while (url.endsWith('/')) url = url.slice(0, -1);
  // 防止双重 /api 路径 (如果环境变量中包含了 /api)
  if (url.endsWith('/api')) url = url.slice(0, -4);
  return url;
};

const API_BASE_URL = getBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // 30秒超时
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
    
    // 处理网络错误
    if (error.code === 'ECONNABORTED') {
      console.error('请求超时');
      return Promise.reject(new Error('请求超时，请稍后重试'));
    }
    
    // 处理网络连接错误
    if (!error.response) {
      console.error('网络连接错误');
      return Promise.reject(new Error('网络连接失败，请检查网络设置'));
    }
    
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

export default api;
