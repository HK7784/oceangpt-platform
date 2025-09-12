import React, { useState } from 'react';
import {
  Paper,
  TextField,
  Button,
  Typography,
  Grid,
  Alert,
  CircularProgress,
  Box,
  Divider,
  Card,
  CardContent
} from '@mui/material';
import { generateReport } from '../services/api';

const ReportPage = () => {
  const [formData, setFormData] = useState({
    // 基础信息
    latitude: '',
    longitude: '',
    timestamp: '',
    
    // 传感器数据
    chlorophyllConcentration: '',
    turbidity: '',
    
    // S2 MSI 光谱数据
    s2B1: '',
    s2B2: '',
    s2B3: '',
    s2B4: '',
    s2B5: '',
    s2B6: '',
    s2B7: '',
    s2B8: '',
    s2B8A: '',
    
    // S3 OLCI 光谱数据
    s3Oa01: '',
    s3Oa02: '',
    s3Oa03: '',
    s3Oa04: '',
    s3Oa05: '',
    s3Oa06: '',
    s3Oa07: '',
    s3Oa08: '',
    
    // 神经网络预测值
    chlNN: '',
    tsmNN: ''
  });
  
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setReport(null);

    try {
      // 转换数据类型
      const requestData = {
        ...formData,
        timestamp: formData.timestamp ? parseInt(formData.timestamp) : Date.now(),
        latitude: parseFloat(formData.latitude) || 0,
        longitude: parseFloat(formData.longitude) || 0,
        chlorophyllConcentration: parseFloat(formData.chlorophyllConcentration) || 0,
        turbidity: parseFloat(formData.turbidity) || 0,
        s2B1: parseFloat(formData.s2B1) || 0,
        s2B2: parseFloat(formData.s2B2) || 0,
        s2B3: parseFloat(formData.s2B3) || 0,
        s2B4: parseFloat(formData.s2B4) || 0,
        s2B5: parseFloat(formData.s2B5) || 0,
        s2B6: parseFloat(formData.s2B6) || 0,
        s2B7: parseFloat(formData.s2B7) || 0,
        s2B8: parseFloat(formData.s2B8) || 0,
        s2B8A: parseFloat(formData.s2B8A) || 0,
        s3Oa01: parseFloat(formData.s3Oa01) || 0,
        s3Oa02: parseFloat(formData.s3Oa02) || 0,
        s3Oa03: parseFloat(formData.s3Oa03) || 0,
        s3Oa04: parseFloat(formData.s3Oa04) || 0,
        s3Oa05: parseFloat(formData.s3Oa05) || 0,
        s3Oa06: parseFloat(formData.s3Oa06) || 0,
        s3Oa07: parseFloat(formData.s3Oa07) || 0,
        s3Oa08: parseFloat(formData.s3Oa08) || 0,
        chlNN: parseFloat(formData.chlNN) || 0,
        tsmNN: parseFloat(formData.tsmNN) || 0
      };

      const result = await generateReport(requestData);
      setReport(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fillSampleData = () => {
    setFormData({
      latitude: '36.0544',
      longitude: '120.3822',
      timestamp: '202401',
      chlorophyllConcentration: '2.5',
      turbidity: '15.3',
      s2B1: '0.0847',
      s2B2: '0.0833',
      s2B3: '0.0819',
      s2B4: '0.0780',
      s2B5: '0.0901',
      s2B6: '0.1304',
      s2B7: '0.1466',
      s2B8: '0.1490',
      s2B8A: '0.1518',
      s3Oa01: '0.0847',
      s3Oa02: '0.0833',
      s3Oa03: '0.0819',
      s3Oa04: '0.0780',
      s3Oa05: '0.0901',
      s3Oa06: '0.1304',
      s3Oa07: '0.1466',
      s3Oa08: '0.1490',
      chlNN: '2.1',
      tsmNN: '15.3'
    });
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        海洋水质报告生成
      </Typography>
      
      <Paper className="report-container" elevation={3}>
        <form onSubmit={handleSubmit}>
          <Box sx={{ mb: 2 }}>
            <Button 
              variant="outlined" 
              onClick={fillSampleData}
              sx={{ mr: 2 }}
            >
              填充示例数据
            </Button>
          </Box>
          
          <Divider sx={{ mb: 3 }} />
          
          {/* 基础信息 */}
          <Typography variant="h6" gutterBottom color="primary">
            基础信息
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="纬度"
                name="latitude"
                value={formData.latitude}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="经度"
                name="longitude"
                value={formData.longitude}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="时间戳"
                name="timestamp"
                value={formData.timestamp}
                onChange={handleInputChange}
                placeholder="请填写月份（如：202401表示2024年1月）"
              />
            </Grid>
          </Grid>
          
          {/* 传感器数据 */}
          <Typography variant="h6" gutterBottom color="primary">
            传感器数据
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="叶绿素浓度 (mg/m³)"
                name="chlorophyllConcentration"
                value={formData.chlorophyllConcentration}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="浊度 (NTU)"
                name="turbidity"
                value={formData.turbidity}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
          </Grid>
          
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={loading}
            sx={{ mt: 2 }}
          >
            {loading ? <CircularProgress size={24} /> : '生成报告'}
          </Button>
        </form>
      </Paper>
      
      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
      
      {report && (
        <Card sx={{ mt: 3 }} elevation={3}>
          <CardContent>
            <Typography variant="h5" gutterBottom>
              生成的报告
            </Typography>
            <Box sx={{ mt: 2 }}>
              {report.success ? (
                <div>
                  <Typography variant="h6" color="success.main" gutterBottom>
                    ✅ 报告生成成功
                  </Typography>
                  {report.reportContent && (
                    <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                      <Typography variant="body1" component="div" style={{ whiteSpace: 'pre-wrap' }}>
                        {report.reportContent}
                      </Typography>
                    </Box>
                  )}
                  {report.reportId && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      报告ID: {report.reportId}
                    </Typography>
                  )}
                </div>
              ) : (
                <div>
                  <Typography variant="h6" color="error.main" gutterBottom>
                    ❌ 报告生成失败
                  </Typography>
                  <Typography variant="body1" color="error.main">
                    {report.errorMessage || '未知错误'}
                  </Typography>
                </div>
              )}
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default ReportPage;