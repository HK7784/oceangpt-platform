// API基础配置
const API_BASE_URL = 'http://localhost:8080/api';

// 地图实例
let map;
let markers = [];

// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    initializeForms();
    initializeMap();
});

// 初始化表单事件
function initializeForms() {
    // 预测表单
    document.getElementById('predictionForm').addEventListener('submit', handlePrediction);
    
    // 报告表单
    document.getElementById('reportForm').addEventListener('submit', handleReport);
    
    // 标签页切换事件
    document.getElementById('map-tab').addEventListener('click', function() {
        setTimeout(() => {
            if (map) {
                map.invalidateSize();
            }
        }, 100);
    });
}

// 填充示例数据
function fillSampleData() {
    const form = document.getElementById('predictionForm');
    const sampleData = {
        latitude: '36.0544',
        longitude: '120.3822',
        timestamp: '202401',
        chlorophyllConcentration: '2.5',
        turbidity: '1.5',
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
    };
    
    Object.keys(sampleData).forEach(key => {
        const input = form.querySelector(`[name="${key}"]`);
        if (input) {
            input.value = sampleData[key];
        }
    });
}

// 填充报告示例数据
function fillReportSampleData() {
    const form = document.getElementById('reportForm');
    const sampleData = {
        latitude: '36.0544',
        longitude: '120.3822',
        timestamp: '202401',
        chlorophyllConcentration: '2.5',
        turbidity: '1.5'
    };
    
    Object.keys(sampleData).forEach(key => {
        const input = form.querySelector(`[name="${key}"]`);
        if (input) {
            input.value = sampleData[key];
        }
    });
}

// 处理预测请求
async function handlePrediction(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    const loading = submitBtn.querySelector('.loading');
    const resultDiv = document.getElementById('predictionResult');
    const outputPre = document.getElementById('predictionOutput');
    
    // 显示加载状态
    loading.style.display = 'inline-block';
    submitBtn.disabled = true;
    resultDiv.style.display = 'none';
    
    try {
        // 收集表单数据
        const formData = new FormData(form);
        const requestData = {};
        
        // 必需字段列表
        const requiredFields = ['latitude', 'longitude', 'timestamp'];
        
        for (let [key, value] of formData.entries()) {
            const trimmedValue = value.trim();
            
            if (trimmedValue !== '') {
                if (key === 'timestamp') {
                    requestData[key] = parseInt(trimmedValue) || Date.now();
                } else if (key === 'latitude' || key === 'longitude') {
                    // 确保经纬度字段存在且有效
                    const numValue = parseFloat(trimmedValue);
                    if (!isNaN(numValue)) {
                        requestData[key] = numValue;
                    }
                } else {
                    const numValue = parseFloat(trimmedValue);
                    if (!isNaN(numValue)) {
                        requestData[key] = numValue;
                    }
                }
            }
        }
        
        // 验证必需字段
        if (!requestData.latitude || !requestData.longitude) {
            throw new Error('请填写有效的经纬度信息');
        }
        
        // 确保时间戳字段存在
        if (!requestData.hasOwnProperty('timestamp')) {
            requestData.timestamp = Date.now();
        }
        
        console.log('发送预测请求:', requestData);
        
        // 发送请求
        const response = await fetch(`${API_BASE_URL}/v1/water-quality/predict`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestData)
        });
        
        const result = await response.json();
        console.log('预测响应:', result);
        
        // 显示结果
        outputPre.textContent = JSON.stringify(result, null, 2);
        resultDiv.style.display = 'block';
        
        // 滚动到结果
        resultDiv.scrollIntoView({ behavior: 'smooth' });
        
    } catch (error) {
        console.error('预测请求失败:', error);
        outputPre.textContent = `错误: ${error.message}`;
        resultDiv.style.display = 'block';
    } finally {
        // 隐藏加载状态
        loading.style.display = 'none';
        submitBtn.disabled = false;
    }
}

// 处理报告生成请求
async function handleReport(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    const loading = submitBtn.querySelector('.loading');
    const resultDiv = document.getElementById('reportResult');
    const outputDiv = document.getElementById('reportOutput');
    
    // 显示加载状态
    loading.style.display = 'inline-block';
    submitBtn.disabled = true;
    resultDiv.style.display = 'none';
    
    try {
        // 收集表单数据
        const formData = new FormData(form);
        const requestData = {};
        
        // 必需字段列表
        const requiredFields = ['latitude', 'longitude', 'timestamp'];
        
        for (let [key, value] of formData.entries()) {
            const trimmedValue = value.trim();
            
            if (trimmedValue !== '') {
                if (key === 'timestamp') {
                    requestData[key] = parseInt(trimmedValue) || Date.now();
                } else if (key === 'latitude' || key === 'longitude') {
                    // 确保经纬度字段存在且有效
                    const numValue = parseFloat(trimmedValue);
                    if (!isNaN(numValue)) {
                        requestData[key] = numValue;
                    }
                } else {
                    const numValue = parseFloat(trimmedValue);
                    if (!isNaN(numValue)) {
                        requestData[key] = numValue;
                    }
                }
            }
        }
        
        // 验证必需字段
        if (!requestData.latitude || !requestData.longitude) {
            throw new Error('请填写有效的经纬度信息');
        }
        
        // 确保时间戳字段存在
        if (!requestData.hasOwnProperty('timestamp')) {
            requestData.timestamp = Date.now();
        }
        
        console.log('发送报告请求:', requestData);
        
        // 发送请求
        const response = await fetch(`${API_BASE_URL}/v1/water-quality/analyze/report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestData)
        });
        
        const result = await response.json();
        console.log('报告响应:', result);
        
        // 显示结果
        if (result.success) {
            let reportHtml = `
                <div class="alert alert-success">
                    <h5>✅ 报告生成成功</h5>
                    <p><strong>处理时间:</strong> ${result.processingTimeMs}ms</p>
                    <p><strong>生成时间:</strong> ${new Date(result.reportTimestamp).toLocaleString()}</p>
                </div>
            `;
            
            // 报告标题
            if (result.title) {
                reportHtml += `
                    <div class="mt-4">
                        <h4 class="text-primary">${result.title}</h4>
                    </div>
                `;
            }
            
            // 执行摘要
            if (result.executiveSummary) {
                reportHtml += `
                    <div class="mt-4">
                        <h5 class="text-info">📋 执行摘要</h5>
                        <div class="bg-light p-3 rounded" style="white-space: pre-wrap;">${result.executiveSummary}</div>
                    </div>
                `;
            }
            
            // 详细分析
            if (result.detailedAnalysis) {
                reportHtml += `
                    <div class="mt-4">
                        <h5 class="text-info">🔬 详细分析</h5>
                        <div class="bg-light p-3 rounded" style="white-space: pre-wrap;">${result.detailedAnalysis}</div>
                    </div>
                `;
            }
            
            // 建议
            if (result.recommendations && result.recommendations.length > 0) {
                reportHtml += `
                    <div class="mt-4">
                        <h5 class="text-info">💡 建议</h5>
                        <ul class="list-group">
                `;
                result.recommendations.forEach(rec => {
                    reportHtml += `<li class="list-group-item">${rec}</li>`;
                });
                reportHtml += `
                        </ul>
                    </div>
                `;
            }
            
            // 风险评估
            if (result.riskAssessment) {
                reportHtml += `
                    <div class="mt-4">
                        <h5 class="text-warning">⚠️ 风险评估</h5>
                        <div class="bg-light p-3 rounded" style="white-space: pre-wrap;">${result.riskAssessment}</div>
                    </div>
                `;
            }
            
            // 预测数据
            if (result.predictions) {
                reportHtml += `
                    <div class="mt-4">
                        <h5 class="text-success">📊 预测数据</h5>
                        <div class="row">
                `;
                
                // 处理预测数据 - 正确处理PredictionResponse对象结构
                if (result.predictions && typeof result.predictions === 'object') {
                    Object.entries(result.predictions).forEach(([key, predictionObj]) => {
                        if (predictionObj && typeof predictionObj === 'object') {
                            // DIN数据
                            if (predictionObj.dinLevel !== null && predictionObj.dinLevel !== undefined) {
                                reportHtml += `
                                    <div class="col-md-4 mb-3">
                                        <div class="card">
                                            <div class="card-body text-center">
                                                <h6 class="card-title">DIN</h6>
                                                <h4 class="text-primary">${predictionObj.dinLevel.toFixed(3)} ${predictionObj.dinUnit || 'mg/L'}</h4>
                                                <span class="badge" style="background-color: ${getQualityColor(predictionObj.waterQualityLevel || predictionObj.qualityLevel)}; color: white;">${getQualityLabel(predictionObj.waterQualityLevel || predictionObj.qualityLevel || '三级')}</span>
                                            </div>
                                        </div>
                                    </div>
                                `;
                            }
                            
                            // SRP数据
                            if (predictionObj.srpLevel !== null && predictionObj.srpLevel !== undefined) {
                                reportHtml += `
                                    <div class="col-md-4 mb-3">
                                        <div class="card">
                                            <div class="card-body text-center">
                                                <h6 class="card-title">SRP</h6>
                                                <h4 class="text-primary">${predictionObj.srpLevel.toFixed(3)} ${predictionObj.srpUnit || 'mg/L'}</h4>
                                                <span class="badge" style="background-color: ${getQualityColor(predictionObj.waterQualityLevel || predictionObj.qualityLevel)}; color: white;">${getQualityLabel(predictionObj.waterQualityLevel || predictionObj.qualityLevel || '三级')}</span>
                                            </div>
                                        </div>
                                    </div>
                                `;
                            }
                            
                            // pH数据
                            if (predictionObj.phLevel !== null && predictionObj.phLevel !== undefined) {
                                reportHtml += `
                                    <div class="col-md-4 mb-3">
                                        <div class="card">
                                            <div class="card-body text-center">
                                                <h6 class="card-title">pH</h6>
                                                <h4 class="text-primary">${predictionObj.phLevel.toFixed(2)} ${predictionObj.phUnit || ''}</h4>
                                                <span class="badge" style="background-color: ${getQualityColor(predictionObj.waterQualityLevel || predictionObj.qualityLevel)}; color: white;">${getQualityLabel(predictionObj.waterQualityLevel || predictionObj.qualityLevel || '三级')}</span>
                                            </div>
                                        </div>
                                    </div>
                                `;
                            }
                        }
                    });
                }
                
                reportHtml += `
                        </div>
                    </div>
                `;
            }
            
            outputDiv.innerHTML = reportHtml;
        } else {
            outputDiv.innerHTML = `
                <div class="alert alert-danger">
                    <h5>❌ 报告生成失败</h5>
                    <p>${result.errorMessage || '未知错误'}</p>
                </div>
            `;
        }
        
        resultDiv.style.display = 'block';
        
        // 滚动到结果
        resultDiv.scrollIntoView({ behavior: 'smooth' });
        
    } catch (error) {
        console.error('报告请求失败:', error);
        outputDiv.innerHTML = `
            <div class="alert alert-danger">
                <h5>❌ 请求失败</h5>
                <p>${error.message}</p>
            </div>
        `;
        resultDiv.style.display = 'block';
    } finally {
        // 隐藏加载状态
        loading.style.display = 'none';
        submitBtn.disabled = false;
    }
}

// 初始化地图
function initializeMap() {
    // 初始化地图
    map = L.map('map').setView([36.5, 120.5], 8);
    
    // 添加瓦片图层
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);
    
    // 模拟监测点数据
    const monitoringPoints = [
        {
            id: 1,
            name: '青岛近海监测点1',
            lat: 36.0544,
            lng: 120.3822,
            temperature: 18.5,
            salinity: 34.2,
            ph: 8.1,
            dissolvedOxygen: 7.8,
            chlorophyll: 2.5,
            quality: 'excellent',
            lastUpdate: '2024-01-15 10:30:00'
        },
        {
            id: 2,
            name: '青岛近海监测点2',
            lat: 36.1544,
            lng: 120.4822,
            temperature: 19.2,
            salinity: 33.8,
            ph: 8.0,
            dissolvedOxygen: 7.5,
            chlorophyll: 3.1,
            quality: 'good',
            lastUpdate: '2024-01-15 10:25:00'
        },
        {
            id: 3,
            name: '青岛近海监测点3',
            lat: 35.9544,
            lng: 120.2822,
            temperature: 17.8,
            salinity: 34.5,
            ph: 7.9,
            dissolvedOxygen: 6.8,
            chlorophyll: 4.2,
            quality: 'moderate',
            lastUpdate: '2024-01-15 10:20:00'
        },
        {
            id: 4,
            name: '烟台近海监测点',
            lat: 37.4638,
            lng: 121.4478,
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
            lat: 37.5138,
            lng: 122.1201,
            temperature: 15.9,
            salinity: 35.1,
            ph: 8.3,
            dissolvedOxygen: 8.5,
            chlorophyll: 1.5,
            quality: 'excellent',
            lastUpdate: '2024-01-15 10:40:00'
        }
    ];
    
    // 添加监测点标记
    monitoringPoints.forEach(point => {
        const color = getQualityColor(point.quality);
        
        // 创建自定义图标
        const customIcon = L.divIcon({
            className: 'custom-marker',
            html: `<div style="background-color: ${color}; width: 20px; height: 20px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
            iconSize: [20, 20],
            iconAnchor: [10, 10]
        });
        
        // 添加标记
        const marker = L.marker([point.lat, point.lng], { icon: customIcon })
            .addTo(map)
            .bindPopup(`
                <div>
                    <h6>${point.name}</h6>
                    <p><strong>水质等级:</strong> <span style="color: ${color}; font-weight: bold;">${getQualityLabel(point.quality)}</span></p>
                    <p><strong>温度:</strong> ${point.temperature}°C</p>
                    <p><strong>盐度:</strong> ${point.salinity} PSU</p>
                    <p><strong>pH:</strong> ${point.ph}</p>
                    <p><strong>溶解氧:</strong> ${point.dissolvedOxygen} mg/L</p>
                    <p><strong>叶绿素:</strong> ${point.chlorophyll} mg/m³</p>
                    <small>更新时间: ${point.lastUpdate}</small>
                </div>
            `);
        
        // 添加点击事件
        marker.on('click', () => {
            showLocationInfo(point);
        });
        
        // 添加影响范围圆圈
        L.circle([point.lat, point.lng], {
            color: color,
            fillColor: color,
            fillOpacity: 0.1,
            radius: 5000
        }).addTo(map);
        
        markers.push(marker);
    });
}

// 显示位置信息
function showLocationInfo(point) {
    const infoDiv = document.getElementById('locationInfo');
    const color = getQualityColor(point.quality);
    
    infoDiv.innerHTML = `
        <h6>${point.name}</h6>
        <div class="mb-3">
            <span class="badge" style="background-color: ${color}; color: white;">${getQualityLabel(point.quality)}</span>
        </div>
        <div class="row">
            <div class="col-6">
                <small class="text-muted">纬度</small>
                <div>${point.lat}°</div>
            </div>
            <div class="col-6">
                <small class="text-muted">经度</small>
                <div>${point.lng}°</div>
            </div>
            <div class="col-6">
                <small class="text-muted">海表温度</small>
                <div>${point.temperature}°C</div>
            </div>
            <div class="col-6">
                <small class="text-muted">盐度</small>
                <div>${point.salinity} PSU</div>
            </div>
            <div class="col-6">
                <small class="text-muted">pH值</small>
                <div>${point.ph}</div>
            </div>
            <div class="col-6">
                <small class="text-muted">溶解氧</small>
                <div>${point.dissolvedOxygen} mg/L</div>
            </div>
            <div class="col-12">
                <small class="text-muted">叶绿素浓度</small>
                <div>${point.chlorophyll} mg/m³</div>
            </div>
        </div>
        <small class="text-muted d-block mt-2">最后更新: ${point.lastUpdate}</small>
    `;
}

// 获取水质等级颜色
function getQualityColor(quality) {
    switch (quality) {
        // 新的水质等级分类
        case '一级': return '#00FF00';
        case '二级': return '#7FFF00';
        case '三级': return '#FFFF00';
        case '四级': return '#FFA500';
        case '劣四级': return '#FF0000';
        // 兼容旧的分类
        case 'excellent': return '#28a745';
        case 'good': return '#6f42c1';
        case 'moderate': return '#fd7e14';
        case 'poor': return '#dc3545';
        default: return '#6c757d';
    }
}

// 获取水质等级标签
function getQualityLabel(quality) {
    switch (quality) {
        // 新的水质等级分类
        case '一级': return '一级 (优秀)';
        case '二级': return '二级 (良好)';
        case '三级': return '三级 (一般)';
        case '四级': return '四级 (较差)';
        case '劣四级': return '劣四级 (极差)';
        // 兼容旧的分类
        case 'excellent': return '优秀';
        case 'good': return '良好';
        case 'moderate': return '一般';
        case 'poor': return '较差';
        default: return quality || '未知';
    }
}