package com.orderapp.ordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderapp.ordering.entity.StationEntity;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, Long> {
    Optional<StationEntity> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);

    @Query("""
            select s
            from StationEntity s
            left join fetch s.area a
            where s.tenantId = :tenantId
              and (:namePattern = '' or lower(s.name) like :namePattern)
              and (:areaId is null or a.id = :areaId)
              and (:type is null or s.type = :type)
              and (:status is null or s.operationalStatus = :status)
              and (:active is null or (:active = true and s.status = 'ACTIVE') or (:active = false and s.status = 'DISABLED'))
            order by coalesce(a.displayOrder, 0), a.name, s.name
            """)
    List<StationEntity> searchStations(
            @Param("tenantId") Long tenantId,
            @Param("namePattern") String namePattern,
            @Param("areaId") Long areaId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("active") Boolean active);

    @Query(value = """
            select count(*)
            from locations l
            where l.tenant_id = :tenantId
            """, nativeQuery = true)
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = """
            select count(*)
            from locations l
            where l.tenant_id = :tenantId
              and l.status = 'ACTIVE'
              and coalesce(l.operational_status, 'AVAILABLE') = 'AVAILABLE'
            """, nativeQuery = true)
    long countAvailableByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = """
            select count(*)
            from locations l
            where l.tenant_id = :tenantId
              and l.status = 'ACTIVE'
              and coalesce(l.operational_status, 'AVAILABLE') = 'OCCUPIED'
            """, nativeQuery = true)
    long countOccupiedByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = """
            select count(*)
            from locations l
            where l.tenant_id = :tenantId
              and l.status = 'ACTIVE'
              and exists (
                  select 1
                  from orders o
                  where o.location_id = l.id
                    and o.tenant_id = l.tenant_id
                    and o.status not in ('DELIVERED', 'CANCELLED')
              )
            """, nativeQuery = true)
    long countWithActiveOrdersByTenantId(@Param("tenantId") Long tenantId);

    @Query(value = """
            select count(*)
            from locations l
            where l.tenant_id = :tenantId
              and (l.status = 'DISABLED' or coalesce(l.operational_status, 'AVAILABLE') = 'CLOSED')
            """, nativeQuery = true)
    long countClosedByTenantId(@Param("tenantId") Long tenantId);

                @Query(value = """
                                                select count(*)
                                                from orders o
                                                where o.tenant_id = :tenantId
                                                        and o.location_id = :stationId
                                                        and o.status not in ('DELIVERED', 'CANCELLED')
                                                """, nativeQuery = true)
                long countActiveOrdersByStationId(@Param("tenantId") Long tenantId, @Param("stationId") Long stationId);

                @Query(value = """
                                                select count(*)
                                                from orders o
                                                where o.tenant_id = :tenantId
                                                        and o.location_id = :stationId
                                                """, nativeQuery = true)
                long countOrdersByStationId(@Param("tenantId") Long tenantId, @Param("stationId") Long stationId);
}