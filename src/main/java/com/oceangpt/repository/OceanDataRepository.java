package com.oceangpt.repository;

import com.oceangpt.model.OceanData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 海洋数据Repository接口
 */
@Repository
public interface OceanDataRepository extends JpaRepository<OceanData, Long> {
    
    /**
     * 根据时间范围查询海洋数据
     */
    List<OceanData> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据地理位置范围查询数据
     */
    @Query("SELECT od FROM OceanData od WHERE od.latitude BETWEEN :minLat AND :maxLat " +
           "AND od.longitude BETWEEN :minLon AND :maxLon")
    List<OceanData> findByLocationRange(@Param("minLat") Double minLatitude,
                                       @Param("maxLat") Double maxLatitude,
                                       @Param("minLon") Double minLongitude,
                                       @Param("maxLon") Double maxLongitude);
    
    /**
     * 根据数据源查询
     */
    List<OceanData> findByDataSource(String dataSource);
    
    /**
     * 查询最新的N条数据
     */
    @Query("SELECT od FROM OceanData od ORDER BY od.timestamp DESC")
    List<OceanData> findLatestData(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 根据污染指数范围查询
     */
    List<OceanData> findByPollutionIndexBetween(Double minIndex, Double maxIndex);
    
    /**
     * 统计指定时间范围内的数据数量
     */
    @Query("SELECT COUNT(od) FROM OceanData od WHERE od.timestamp BETWEEN :startTime AND :endTime")
    Long countByTimestampBetween(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
}