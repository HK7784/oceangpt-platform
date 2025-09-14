package com.oceangpt.repository;

import com.oceangpt.model.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预测结果Repository接口
 */
@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    
    /**
     * 根据预测时间范围查询结果
     */
    List<PredictionResult> findByPredictionTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据目标时间范围查询预测结果
     */
    List<PredictionResult> findByTargetTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据地理位置查询预测结果
     */
    @Query("SELECT pr FROM PredictionResult pr WHERE pr.latitude BETWEEN :minLat AND :maxLat " +
           "AND pr.longitude BETWEEN :minLon AND :maxLon")
    List<PredictionResult> findByLocationRange(@Param("minLat") Double minLatitude,
                                              @Param("maxLat") Double maxLatitude,
                                              @Param("minLon") Double minLongitude,
                                              @Param("maxLon") Double maxLongitude);
    
    /**
     * 根据置信度阈值查询高质量预测结果
     */
    List<PredictionResult> findByConfidenceScoreGreaterThanEqual(Double minConfidence);
    
    /**
     * 根据污染等级查询预测结果
     */
    List<PredictionResult> findByPredictedPollutionLevel(String pollutionLevel);
    
    /**
     * 根据模型版本查询预测结果
     */
    List<PredictionResult> findByModelVersion(String modelVersion);
    
    /**
     * 查询最新的预测结果
     */
    @Query("SELECT pr FROM PredictionResult pr ORDER BY pr.predictionTimestamp DESC")
    List<PredictionResult> findLatestPredictions(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 统计指定时间范围内的预测数量
     */
    @Query("SELECT COUNT(pr) FROM PredictionResult pr WHERE pr.predictionTimestamp BETWEEN :startTime AND :endTime")
    Long countByPredictionTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算平均置信度
     */
    @Query("SELECT AVG(pr.confidenceScore) FROM PredictionResult pr WHERE pr.predictionTimestamp BETWEEN :startTime AND :endTime")
    Double calculateAverageConfidence(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
}