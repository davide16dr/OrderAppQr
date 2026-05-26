package com.orderapp.ordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.StationQrCodeEntity;

@Repository
public interface StationQrCodeRepository extends JpaRepository<StationQrCodeEntity, Long> {
    Optional<StationQrCodeEntity> findByCodeIgnoreCase(String code);

    List<StationQrCodeEntity> findByStationIdOrderByGeneratedAtDesc(Long stationId);

    @Query("""
            select q
            from StationQrCodeEntity q
            where q.station.id = :stationId
              and q.status = 'ACTIVE'
            order by q.generatedAt desc
            """)
    Optional<StationQrCodeEntity> findActiveByStationId(@Param("stationId") Long stationId);

    @Query("""
            select q
            from StationQrCodeEntity q
            where q.station.id = :stationId
              and q.status = 'ACTIVE'
            """)
    List<StationQrCodeEntity> findActiveByStationIdList(@Param("stationId") Long stationId);
}