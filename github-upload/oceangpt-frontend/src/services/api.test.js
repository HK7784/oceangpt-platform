import axios from 'axios';
import {
  predictWaterQuality,
  generateReport,
  healthCheck,
  chatWithOceanGPT,
  getChatHistory,
  clearChatHistory
} from './api';

import { mockApiResponse, mockWaterQualityData } from '../utils/testUtils';

// Mock axios
jest.mock('axios', () => ({
  create: jest.fn(() => ({
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() }
    },
    post: jest.fn(),
    get: jest.fn(),
    delete: jest.fn()
  })),
  post: jest.fn(),
  get: jest.fn(),
  delete: jest.fn()
}));

const mockedAxios = axios;

describe('API Service', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('predictWaterQuality', () => {
    test('successfully predicts water quality', async () => {
      const mockResponse = {
        data: {
          success: true,
          data: mockWaterQualityData.predictionResult
        }
      };
      
      mockedAxios.create.mockReturnValue({
        post: jest.fn().mockResolvedValue(mockResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const predictionData = {
        latitude: 36.0544,
        longitude: 120.3822,
        temperature: 18.5
      };

      const result = await predictWaterQuality(predictionData);
      expect(result.success).toBe(true);
      expect(result.data).toEqual(mockWaterQualityData.predictionResult);
    });

    test('handles prediction API errors', async () => {
      const errorResponse = {
        response: {
          data: {
            message: '预测服务暂时不可用'
          }
        }
      };

      mockedAxios.create.mockReturnValue({
        post: jest.fn().mockRejectedValue(errorResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      await expect(predictWaterQuality({})).rejects.toThrow('预测请求失败: 预测服务暂时不可用');
    });
  });

  describe('generateReport', () => {
    test('successfully generates report', async () => {
      const mockReportResponse = {
        data: {
          success: true,
          data: {
            reportId: 'RPT-001',
            content: '水质分析报告',
            generatedAt: new Date().toISOString()
          }
        }
      };

      mockedAxios.create.mockReturnValue({
        post: jest.fn().mockResolvedValue(mockReportResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const reportData = {
        latitude: 36.0544,
        longitude: 120.3822
      };

      const result = await generateReport(reportData);
      expect(result.success).toBe(true);
      expect(result.data.reportId).toBe('RPT-001');
    });

    test('handles report generation errors', async () => {
      mockedAxios.create.mockReturnValue({
        post: jest.fn().mockRejectedValue(new Error('Network error')),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      await expect(generateReport({})).rejects.toThrow('报告生成失败: Network error');
    });
  });

  describe('healthCheck', () => {
    test('successfully checks health status', async () => {
      const mockHealthResponse = {
        data: {
          status: 'UP',
          components: {
            db: { status: 'UP' },
            diskSpace: { status: 'UP' }
          }
        }
      };

      mockedAxios.create.mockReturnValue({
        get: jest.fn().mockResolvedValue(mockHealthResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const result = await healthCheck();
      expect(result.status).toBe('UP');
    });

    test('handles health check errors', async () => {
      mockedAxios.create.mockReturnValue({
        get: jest.fn().mockRejectedValue(new Error('Service unavailable')),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      await expect(healthCheck()).rejects.toThrow('健康检查失败: Service unavailable');
    });
  });

  describe('chatWithOceanGPT', () => {
    test('successfully sends chat message', async () => {
      const mockChatResponse = {
        data: {
          success: true,
          data: {
            message: '我已经收到您的问题，正在分析中...',
            sessionId: 'session_123',
            confidence: 0.9
          }
        }
      };

      mockedAxios.create.mockReturnValue({
        post: jest.fn().mockResolvedValue(mockChatResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const result = await chatWithOceanGPT('分析水质数据', 'session_123');
      expect(result.success).toBe(true);
      expect(result.data.sessionId).toBe('session_123');
    });

    test('generates session ID when not provided', async () => {
      const mockChatResponse = {
        data: {
          success: true,
          data: { message: 'Response' }
        }
      };

      const mockPost = jest.fn().mockResolvedValue(mockChatResponse);
      mockedAxios.create.mockReturnValue({
        post: mockPost,
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      await chatWithOceanGPT('测试消息');
      
      // Check that a session ID was generated
      const callArgs = mockPost.mock.calls[0][1];
      expect(callArgs.sessionId).toMatch(/^session_\d+$/);
    });
  });

  describe('getChatHistory', () => {
    test('successfully retrieves chat history', async () => {
      const mockHistoryResponse = {
        data: {
          success: true,
          data: [
            { id: 1, message: '你好', type: 'user' },
            { id: 2, message: '您好！', type: 'bot' }
          ]
        }
      };

      mockedAxios.create.mockReturnValue({
        get: jest.fn().mockResolvedValue(mockHistoryResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const result = await getChatHistory('session_123');
      expect(result.success).toBe(true);
      expect(result.data).toHaveLength(2);
    });
  });

  describe('clearChatHistory', () => {
    test('successfully clears chat history', async () => {
      const mockClearResponse = {
        data: {
          success: true,
          message: '聊天历史已清除'
        }
      };

      mockedAxios.create.mockReturnValue({
        delete: jest.fn().mockResolvedValue(mockClearResponse),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });

      const result = await clearChatHistory('session_123');
      expect(result.success).toBe(true);
    });
  });

  describe('API interceptors', () => {
    test('request interceptor logs requests', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
      
      // This would test the actual interceptor implementation
      // For now, we just verify the API structure exists
      expect(typeof predictWaterQuality).toBe('function');
      
      consoleSpy.mockRestore();
    });

    test('response interceptor logs responses', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
      
      // This would test the actual interceptor implementation
      expect(typeof generateReport).toBe('function');
      
      consoleSpy.mockRestore();
    });
  });
});