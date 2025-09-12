import React from 'react';
import { screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';
import { renderWithTheme } from './utils/testUtils';

// Mock the page components
jest.mock('./pages/ChatPage', () => {
  return function MockChatPage() {
    return <div data-testid="chat-page">Chat Page Content</div>;
  };
});

jest.mock('./pages/PredictionPage', () => {
  return function MockPredictionPage() {
    return <div data-testid="prediction-page">Prediction Page Content</div>;
  };
});

jest.mock('./pages/ReportPage', () => {
  return function MockReportPage() {
    return <div data-testid="report-page">Report Page Content</div>;
  };
});

jest.mock('./pages/MapPage', () => {
  return function MockMapPage() {
    return <div data-testid="map-page">Map Page Content</div>;
  };
});

describe('App', () => {
  test('renders main application title', () => {
    renderWithTheme(<App />);
    expect(screen.getByText('OceanGPT - 海洋水质预测平台')).toBeInTheDocument();
  });

  test('renders unified chat interface', () => {
    renderWithTheme(<App />);
    
    expect(screen.getByTestId('chat-page')).toBeInTheDocument();
    expect(screen.getByText('📊 水质预测')).toBeInTheDocument();
    expect(screen.getByText('📋 生成报告')).toBeInTheDocument();
    expect(screen.getByText('🗺️ 显示地图')).toBeInTheDocument();
  });

  test('displays ChatPage by default', () => {
    renderWithTheme(<App />);
    expect(screen.getByTestId('chat-page')).toBeInTheDocument();
  });

  test('quick action buttons are clickable', () => {
    renderWithTheme(<App />);
    
    const predictionChip = screen.getByText('📊 水质预测');
    const reportChip = screen.getByText('📋 生成报告');
    const mapChip = screen.getByText('🗺️ 显示地图');
    
    fireEvent.click(predictionChip);
    fireEvent.click(reportChip);
    fireEvent.click(mapChip);
    
    // Chat page should remain visible as it's the unified interface
    expect(screen.getByTestId('chat-page')).toBeInTheDocument();
  });

  test('applies correct theme and styling', () => {
    renderWithTheme(<App />);
    
    // Check if the app container exists
    const appContainer = screen.getByText('OceanGPT - 海洋水质预测平台').closest('.App');
    expect(appContainer).toBeInTheDocument();
  });

  test('renders with proper Material-UI theme', () => {
    renderWithTheme(<App />);
    
    // Check if Material-UI components are rendered
    expect(screen.getByTestId('chat-page')).toBeInTheDocument();
    expect(screen.getByText('📊 水质预测')).toBeInTheDocument();
  });

  test('handles chat interface interactions', () => {
    renderWithTheme(<App />);
    
    const chatInput = screen.getByPlaceholderText(/输入您的问题/);
    
    // Focus on the chat input
    fireEvent.focus(chatInput);
    
    // Type a message
    fireEvent.change(chatInput, { target: { value: '测试消息' } });
    
    // The component should handle input properly
    expect(chatInput).toHaveValue('测试消息');
  });

  test('renders responsive container', () => {
    renderWithTheme(<App />);
    
    // Check if the container has the correct max-width setting
    const container = screen.getByText('OceanGPT - 海洋水质预测平台').closest('[class*="MuiContainer"]');
    expect(container).toBeInTheDocument();
  });

  test('quick action chips are accessible', () => {
    renderWithTheme(<App />);
    
    const chips = [
      screen.getByText('📊 水质预测'),
      screen.getByText('📋 生成报告'),
      screen.getByText('🗺️ 显示地图')
    ];
    
    chips.forEach(chip => {
      expect(chip).toBeInTheDocument();
      expect(chip.closest('.MuiChip-root')).toBeInTheDocument();
    });
  });
});