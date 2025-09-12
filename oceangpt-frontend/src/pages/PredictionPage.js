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
  Divider
} from '@mui/material';
import { predictWaterQuality } from '../services/api';

const PredictionPage = () => {
  const [formData, setFormData] = useState({
    // 基础信息
    latitude: '',
    longitude: '',
    timestamp: '',
    
    // 传感器数据
    sensorData: '',
    seaSurfaceTemperature: '',
    salinity: '',
    phLevel: '',
    dissolvedOxygen: '',
    chlorophyllConcentration: '',
    
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
  
  const [prediction, setPrediction] = useState(null);
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
    setPrediction(null);

    try {
      // 验证必需字段
      if (!formData.latitude || !formData.longitude) {
        setError('请填写纬度和经度信息，或点击"填充示例数据"按钮');
        return;
      }

      // 转换数据类型
      const requestData = {
        ...formData,
        timestamp: formData.timestamp ? parseInt(formData.timestamp) : Date.now(),
        latitude: parseFloat(formData.latitude),
        longitude: parseFloat(formData.longitude),
        sensorData: parseFloat(formData.sensorData) || 0,
        seaSurfaceTemperature: parseFloat(formData.seaSurfaceTemperature) || 0,
        salinity: parseFloat(formData.salinity) || 0,
        phLevel: parseFloat(formData.phLevel) || 0,
        dissolvedOxygen: parseFloat(formData.dissolvedOxygen) || 0,
        chlorophyllConcentration: parseFloat(formData.chlorophyllConcentration) || 0,
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

      const result = await predictWaterQuality(requestData);
      setPrediction(result);
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
      timestamp: Date.now().toString(),
      sensorData: '25.5',
      seaSurfaceTemperature: '18.5',
      salinity: '34.2',
      phLevel: '8.1',
      dissolvedOxygen: '7.8',
      chlorophyllConcentration: '2.5',
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
        海洋水质预测
      </Typography>
      
      <Paper className="prediction-form" elevation={3}>
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
                type="number"
              />
            </Grid>
          </Grid>
          
          {/* 传感器数据 */}
          <Typography variant="h6" gutterBottom color="primary">
            传感器数据
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="传感器数据"
                name="sensorData"
                value={formData.sensorData}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="海表温度 (°C)"
                name="seaSurfaceTemperature"
                value={formData.seaSurfaceTemperature}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="盐度 (PSU)"
                name="salinity"
                value={formData.salinity}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="pH值"
                name="phLevel"
                value={formData.phLevel}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="溶解氧 (mg/L)"
                name="dissolvedOxygen"
                value={formData.dissolvedOxygen}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
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
          </Grid>
          
          {/* S2 MSI 光谱数据 */}
          <Typography variant="h6" gutterBottom color="primary">
            Sentinel-2 MSI 光谱数据
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            {['s2B1', 's2B2', 's2B3', 's2B4', 's2B5', 's2B6', 's2B7', 's2B8', 's2B8A'].map((band) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={band}>
                <TextField
                  fullWidth
                  label={band.toUpperCase()}
                  name={band}
                  value={formData[band]}
                  onChange={handleInputChange}
                  type="number"
                  inputProps={{ step: "any" }}
                />
              </Grid>
            ))}
          </Grid>
          
          {/* S3 OLCI 光谱数据 */}
          <Typography variant="h6" gutterBottom color="primary">
            Sentinel-3 OLCI 光谱数据
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            {['s3Oa01', 's3Oa02', 's3Oa03', 's3Oa04', 's3Oa05', 's3Oa06', 's3Oa07', 's3Oa08'].map((band) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={band}>
                <TextField
                  fullWidth
                  label={band.toUpperCase()}
                  name={band}
                  value={formData[band]}
                  onChange={handleInputChange}
                  type="number"
                  inputProps={{ step: "any" }}
                />
              </Grid>
            ))}
          </Grid>
          
          {/* 神经网络预测值 */}
          <Typography variant="h6" gutterBottom color="primary">
            神经网络预测值
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="叶绿素神经网络预测值"
                name="chlNN"
                value={formData.chlNN}
                onChange={handleInputChange}
                type="number"
                inputProps={{ step: "any" }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="总悬浮物神经网络预测值"
                name="tsmNN"
                value={formData.tsmNN}
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
            {loading ? <CircularProgress size={24} /> : '开始预测'}
          </Button>
        </form>
      </Paper>
      
      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
      
      {prediction && (
        <Paper className="prediction-result" elevation={3}>
          <Typography variant="h5" gutterBottom>
            预测结果
          </Typography>
          <pre style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(prediction, null, 2)}
          </pre>
        </Paper>
      )}
    </Box>
  );
};

export default PredictionPage;