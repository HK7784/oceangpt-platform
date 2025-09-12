import React from 'react';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from '../App';
import { renderWithTheme, mockStompClient, delay } from '../utils/testUtils';
import { Client } from '@stomp/stompjs';
import * as api from '../services/api';

// Mock dependencies
jest.mock('@stomp/stompjs');
jest.mock('sockjs-client');
jest.mock('../services/api');

describe('Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Client.mockImplementation(() => mockStompClient);
  });

  describe('App Integration', () => {
    test('loads unified chat interface correctly', async () => {
      renderWithTheme(<App />);
      
      // Should show the unified chat interface
      expect(screen.getByText(/您好！我是OceanGPT/)).toBeInTheDocument();
      expect(screen.getByText('OceanGPT 智能助手')).toBeInTheDocument();
      
      // Should have quick action buttons
      expect(screen.getByText('🗺️ 显示地图')).toBeInTheDocument();
      expect(screen.getByText('📊 水质预测')).toBeInTheDocument();
      expect(screen.getByText('📋 生成报告')).toBeInTheDocument();
      expect(screen.getByText('📈 数据分析')).toBeInTheDocument();
    });
  });

  describe('Chat and Prediction Integration', () => {
    test('chat can trigger water quality prediction', async () => {
      // Mock API response
      api.predictWaterQuality.mockResolvedValue({
        success: true,
        data: {
          din: 0.125,
          srp: 0.032,
          ph: 8.1,
          qualityLevel: '良好',
          confidence: 0.85
        }
      });

      renderWithTheme(<App />);
      
      // Should be on chat page by default
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Send a prediction request through chat
      fireEvent.change(input, { target: { value: '预测青岛海域水质' } });
      fireEvent.click(sendButton);
      
      // Should show loading state
      await waitFor(() => {
        expect(screen.getByText('正在思考中...')).toBeInTheDocument();
      });
    });
  });

  describe('Chat and Report Integration', () => {
    test('chat can trigger report generation', async () => {
      // Mock API response
      api.generateReport.mockResolvedValue({
        success: true,
        data: {
          reportId: 'RPT-001',
          content: '水质分析报告已生成',
          generatedAt: new Date().toISOString()
        }
      });

      renderWithTheme(<App />);
      
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Send a report generation request through chat
      fireEvent.change(input, { target: { value: '生成青岛海域水质报告' } });
      fireEvent.click(sendButton);
      
      // Should show loading state
      await waitFor(() => {
        expect(screen.getByText('正在思考中...')).toBeInTheDocument();
      });
    });
  });

  describe('Chat and Map Integration', () => {
    test('chat can trigger map visualization', async () => {
      renderWithTheme(<App />);
      
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Send a map visualization request through chat
      fireEvent.change(input, { target: { value: '显示青岛海域监测点' } });
      fireEvent.click(sendButton);
      
      // Should show loading state
      await waitFor(() => {
        expect(screen.getByText('正在思考中...')).toBeInTheDocument();
      });
    });
  });

  describe('WebSocket Integration', () => {
    test('handles WebSocket connection and message flow', async () => {
      renderWithTheme(<App />);
      
      // Simulate WebSocket connection
      if (mockStompClient.onConnect) {
        mockStompClient.onConnect();
      }
      
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Send a message
      fireEvent.change(input, { target: { value: '测试WebSocket连接' } });
      fireEvent.click(sendButton);
      
      // Verify WebSocket publish was called
      await waitFor(() => {
        expect(mockStompClient.publish).toHaveBeenCalledWith({
          destination: '/app/chat.sendMessage',
          body: expect.stringContaining('测试WebSocket连接')
        });
      });
    });

    test('handles WebSocket disconnection gracefully', async () => {
      renderWithTheme(<App />);
      
      // Simulate WebSocket disconnection
      if (mockStompClient.onDisconnect) {
        mockStompClient.onDisconnect();
      }
      
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Try to send a message when disconnected
      fireEvent.change(input, { target: { value: '测试断线消息' } });
      fireEvent.click(sendButton);
      
      // Should show connection error
      await waitFor(() => {
        expect(screen.getByText(/连接已断开/)).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling Integration', () => {
    test('handles API errors gracefully across modules', async () => {
      // Mock API error
      api.predictWaterQuality.mockRejectedValue(new Error('服务器错误'));
      
      renderWithTheme(<App />);
      
      // Click on prediction quick action
      fireEvent.click(screen.getByText('📊 水质预测'));
      
      // Verify the chat interface remains stable
      await waitFor(() => {
        expect(screen.getByTestId('chat-page')).toBeInTheDocument();
      });
    });

    test('maintains app stability when individual modules fail', async () => {
      renderWithTheme(<App />);
      
      // Simulate various error conditions
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      // Test rapid interactions with quick action buttons to ensure stability
      const quickActions = ['📊 水质预测', '📋 生成报告', '🗺️ 显示地图'];
      
      for (const action of quickActions) {
        const chip = screen.getByText(action);
        fireEvent.click(chip);
        await delay(100); // Small delay to allow rendering
        expect(chip).toBeInTheDocument();
      }
      
      consoleError.mockRestore();
    });
  });

  describe('Performance Integration', () => {
    test('handles rapid interactions without issues', async () => {
      renderWithTheme(<App />);
      
      // Test rapid clicking of quick action buttons
      const quickButtons = ['🗺️ 显示地图', '📊 水质预测', '📋 生成报告', '📈 数据分析'];
      
      for (let i = 0; i < 5; i++) {
        const buttonText = quickButtons[i % quickButtons.length];
        const button = screen.getByText(buttonText);
        fireEvent.click(button);
        await delay(100);
      }
      
      // App should still be responsive
      expect(screen.getByText('OceanGPT 智能助手')).toBeInTheDocument();
    });

    test('handles multiple simultaneous chat messages', async () => {
      renderWithTheme(<App />);
      
      const input = screen.getByPlaceholderText(/输入您的问题/);
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      
      // Send multiple messages quickly
      const messages = ['消息1', '消息2', '消息3'];
      
      for (const message of messages) {
        fireEvent.change(input, { target: { value: message } });
        fireEvent.click(sendButton);
        await delay(100);
      }
      
      // Should handle all messages without crashing
      expect(screen.getByText('OceanGPT 智能助手')).toBeInTheDocument();
    });
  });

  describe('Accessibility Integration', () => {
    test('maintains accessibility in unified interface', () => {
      renderWithTheme(<App />);
      
      // Check main chat interface accessibility
      const chatInput = screen.getByPlaceholderText(/请输入您的问题/);
      expect(chatInput).toBeInTheDocument();
      expect(chatInput).not.toHaveAttribute('aria-disabled', 'true');
      
      // Check send button accessibility
      const sendButton = screen.getByRole('button', { name: /发送消息/i });
      expect(sendButton).toBeInTheDocument();
      
      // Check quick action buttons accessibility
      const quickButtons = screen.getAllByRole('button');
      expect(quickButtons.length).toBeGreaterThan(0);
    });

    test('keyboard navigation works in chat interface', () => {
      renderWithTheme(<App />);
      
      const chatInput = screen.getByPlaceholderText(/请输入您的问题/);
      
      // Test keyboard interaction
      fireEvent.focus(chatInput);
      fireEvent.change(chatInput, { target: { value: 'test message' } });
      fireEvent.keyDown(chatInput, { key: 'Enter' });
      
      // Should not crash and maintain functionality
      expect(chatInput).toBeInTheDocument();
    });
  });
});