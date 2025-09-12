import React, { useState, useEffect } from 'react';
import {
  Paper,
  Typography,
  Box,
  Grid,
  Card,
  CardContent,
  Chip,
  Alert
} from '@mui/material';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import L from 'leaflet';

// 修复 Leaflet 默认图标问题
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const MapPage = () => {
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [waterQualityData, setWaterQualityData] = useState([]);

  // 模拟水质监测点数据
  useEffect(() => {
    const mockData = [
      {
        id: 1,
        name: '青岛近海监测点1',
        latitude: 36.0544,
        longitude: 120.3822,
        temperature: 18.5,
        salinity: 34.2,
        ph: 8.1,
        dissolvedOxygen: 7.8,
        chlorophyll: 2.5,
        quality: 'good',
        lastUpdate: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        name: '青岛近海监测点2',
        latitude: 36.1544,
        longitude: 120.4822,
        temperature: 19.2,
        salinity: 33.8,
        ph: 8.0,
        dissolvedOxygen: 7.5,
        chlorophyll: 3.1,
        quality: 'moderate',
        lastUpdate: '2024-01-15 10:25:00'
      },
      {
        id: 3,
        name: '青岛近海监测点3',
        latitude: 35.9544,
        longitude: 120.2822,
        temperature: 17.8,
        salinity: 34.5,
        ph: 7.9,
        dissolvedOxygen: 6.8,
        chlorophyll: 4.2,
        quality: 'poor',
        lastUpdate: '2024-01-15 10:20:00'
      },
      {
        id: 4,
        name: '烟台近海监测点',
        latitude: 37.4638,
        longitude: 121.4478,
        temperature: 16.5,
        salinity: 34.8,
        ph: 8.2,
        dissolvedOxygen: 8.1,
        chlorophyll: 1.8,
        quality: 'excellent',
        lastUpdate: '2024-01-15 10:35:00'
      },
      {
        id: 5,
        name: '威海近海监测点',
        latitude: 37.5138,
        longitude: 122.1201,
        temperature: 15.9,
        salinity: 35.1,
        ph: 8.3,
        dissolvedOxygen: 8.5,
        chlorophyll: 1.5,
        quality: 'excellent',
        lastUpdate: '2024-01-15 10:40:00'
      }
    ];
    setWaterQualityData(mockData);
  }, []);

  const getQualityColor = (quality) => {
    switch (quality) {
      case 'excellent': return '#4caf50';
      case 'good': return '#8bc34a';
      case 'moderate': return '#ff9800';
      case 'poor': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  const getQualityLabel = (quality) => {
    switch (quality) {
      case 'excellent': return '优秀';
      case 'good': return '良好';
      case 'moderate': return '一般';
      case 'poor': return '较差';
      default: return '未知';
    }
  };

  const createCustomIcon = (quality) => {
    const color = getQualityColor(quality);
    return L.divIcon({
      className: 'custom-marker',
      html: `<div style="background-color: ${color}; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        海洋水质地图可视化
      </Typography>
      
      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Paper className="map-container" elevation={3}>
            <Typography variant="h6" gutterBottom>
              实时监测点分布
            </Typography>
            <MapContainer
              center={[36.5, 120.5]}
              zoom={8}
              style={{ height: '500px', width: '100%' }}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              
              {waterQualityData.map((point) => (
                <React.Fragment key={point.id}>
                  <Marker
                    position={[point.latitude, point.longitude]}
                    icon={createCustomIcon(point.quality)}
                    eventHandlers={{
                      click: () => setSelectedLocation(point)
                    }}
                  >
                    <Popup>
                      <div>
                        <Typography variant="subtitle1" fontWeight="bold">
                          {point.name}
                        </Typography>
                        <Typography variant="body2">
                          水质等级: <Chip 
                            label={getQualityLabel(point.quality)} 
                            size="small" 
                            style={{ backgroundColor: getQualityColor(point.quality), color: 'white' }}
                          />
                        </Typography>
                        <Typography variant="body2">温度: {point.temperature}°C</Typography>
                        <Typography variant="body2">盐度: {point.salinity} PSU</Typography>
                        <Typography variant="body2">pH: {point.ph}</Typography>
                        <Typography variant="body2">溶解氧: {point.dissolvedOxygen} mg/L</Typography>
                        <Typography variant="body2">叶绿素: {point.chlorophyll} mg/m³</Typography>
                        <Typography variant="caption" color="text.secondary">
                          更新时间: {point.lastUpdate}
                        </Typography>
                      </div>
                    </Popup>
                  </Marker>
                  
                  <Circle
                    center={[point.latitude, point.longitude]}
                    radius={5000}
                    pathOptions={{
                      color: getQualityColor(point.quality),
                      fillColor: getQualityColor(point.quality),
                      fillOpacity: 0.1,
                      weight: 2
                    }}
                  />
                </React.Fragment>
              ))}
            </MapContainer>
          </Paper>
        </Grid>
        
        <Grid item xs={12} lg={4}>
          <Box>
            <Typography variant="h6" gutterBottom>
              监测点信息
            </Typography>
            
            {selectedLocation ? (
              <Card elevation={3}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {selectedLocation.name}
                  </Typography>
                  
                  <Box sx={{ mb: 2 }}>
                    <Chip 
                      label={getQualityLabel(selectedLocation.quality)} 
                      style={{ 
                        backgroundColor: getQualityColor(selectedLocation.quality), 
                        color: 'white',
                        fontWeight: 'bold'
                      }}
                    />
                  </Box>
                  
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        纬度
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.latitude}°
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        经度
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.longitude}°
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        海表温度
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.temperature}°C
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        盐度
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.salinity} PSU
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        pH值
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.ph}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">
                        溶解氧
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.dissolvedOxygen} mg/L
                      </Typography>
                    </Grid>
                    <Grid item xs={12}>
                      <Typography variant="body2" color="text.secondary">
                        叶绿素浓度
                      </Typography>
                      <Typography variant="body1">
                        {selectedLocation.chlorophyll} mg/m³
                      </Typography>
                    </Grid>
                  </Grid>
                  
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                    最后更新: {selectedLocation.lastUpdate}
                  </Typography>
                </CardContent>
              </Card>
            ) : (
              <Alert severity="info">
                点击地图上的监测点查看详细信息
              </Alert>
            )}
            
            <Card sx={{ mt: 2 }} elevation={3}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  水质等级说明
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 16, height: 16, borderRadius: '50%', backgroundColor: '#4caf50' }} />
                    <Typography variant="body2">优秀 - 水质优良，适合各种海洋活动</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 16, height: 16, borderRadius: '50%', backgroundColor: '#8bc34a' }} />
                    <Typography variant="body2">良好 - 水质良好，轻微污染</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 16, height: 16, borderRadius: '50%', backgroundColor: '#ff9800' }} />
                    <Typography variant="body2">一般 - 水质一般，需要关注</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 16, height: 16, borderRadius: '50%', backgroundColor: '#f44336' }} />
                    <Typography variant="body2">较差 - 水质较差，需要治理</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default MapPage;