package com.example.demo.groupcategory.repository;

import com.example.demo.groupcategory.entity.GroupCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GroupCategoryRepository extends JpaRepository<GroupCategory, Long>, JpaSpecificationExecutor<GroupCategory> {

    boolean existsByParamNameAndParamValueAndParamType(
            String paramName,
            String paramValue,
            String paramType
    );

    boolean existsByParamNameAndParamValueAndParamTypeAndIdNot(
            String paramName,
            String paramValue,
            String paramType,
            Long id
    );

    @Query("""
        select g from GroupCategory g
        where upper(trim(g.paramName)) in :paramNames
          and upper(trim(g.paramValue)) in :paramValues
          and upper(trim(g.paramType)) in :paramTypes
    """)
    List<GroupCategory> findForImportDuplicateCheck(
            Set<String> paramNames,
            Set<String> paramValues,
            Set<String> paramTypes
    );

    @Query("""
        select distinct trim(g.componentCode)
        from GroupCategory g
        where g.isActive = :isActive
          and g.componentCode is not null
          and length(trim(g.componentCode)) > 0
        order by trim(g.componentCode)
    """)
    List<String> findDistinctComponentCodesByIsActive(@Param("isActive") Integer isActive);

    /// Lấy next value từ Oracle sequence
    @Query(value = "SELECT PMH_GROUP_CATEGORY_SEQ.NEXTVAL FROM dual", nativeQuery = true)
    Long nextIdNative();

    /// Select by id bằng native query
    @Query(value = """
        SELECT * 
        FROM PMH_GROUP_CATEGORY
        WHERE ID = :id
        """, nativeQuery = true)
    Optional<GroupCategory> findNativeById(@Param("id") Long id);

    /// Lấy toàn bộ data có phân trang bằng native query
    @Query(
            value = """
                    SELECT *
                    FROM PMH_GROUP_CATEGORY
                    ORDER BY ID DESC
                  """,
            countQuery = """
                    SELECT COUNT(1)
                    FROM PMH_GROUP_CATEGORY
                  """,
            nativeQuery = true
    )
    Page<GroupCategory> findAllNative(Pageable pageable);

    /// insertNative
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO PMH_GROUP_CATEGORY (
            ID,
            PARAM_NAME,
            PARAM_VALUE,
            PARAM_TYPE,
            DESCRIPTION,
            COMPONENT_CODE,
            STATUS,
            IS_ACTIVE,
            IS_DISPLAY,
            NEW_DATA,
            EFFECTIVE_DATE,
            END_EFFECTIVE_DATE    
        ) VALUES (
            :id,
            :paramName,
            :paramValue,
            :paramType,
            :description,
            :componentCode,
            :status,
            :isActive,
            :isDisplay,
            :newData,
            :effectiveDate,
            :endEffectiveDate
            )        
    """, nativeQuery = true)
    int insertNative(
            @Param("id") Long id,
            @Param("paramName") String paramName,
            @Param("paramValue") String paramValue,
            @Param("paramType") String paramType,
            @Param("description") String description,
            @Param("componentCode") String componentCode,
            @Param("status") Integer status,
            @Param("isActive") Integer isActive,
            @Param("isDisplay") Integer isDisplay,
            @Param("newData") String newData,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("endEffectiveDate") LocalDate endEffectiveDate
    );

    /// updateNative
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE PMH_GROUP_CATEGORY
        SET
            PARAM_NAME = :paramName,
            PARAM_VALUE = :paramValue,
            PARAM_TYPE = :paramType,
            DESCRIPTION = :description,
            COMPONENT_CODE = :componentCode,
            STATUS = :status,
            IS_ACTIVE = :isActive,
            IS_DISPLAY = :isDisplay,
            NEW_DATA = :newData,
            EFFECTIVE_DATE = :effectiveDate,
            END_EFFECTIVE_DATE = :endEffectiveDate
        WHERE ID = :id
        """, nativeQuery = true)
    int updateNative(
            @Param("id") Long id,
            @Param("paramName") String paramName,
            @Param("paramValue") String paramValue,
            @Param("paramType") String paramType,
            @Param("description") String description,
            @Param("componentCode") String componentCode,
            @Param("status") Integer status,
            @Param("isActive") Integer isActive,
            @Param("isDisplay") Integer isDisplay,
            @Param("newData") String newData,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("endEffectiveDate")


            LocalDate endEffectiveDate
    );

    /// Delete by id Native Query
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM PMH_GROUP_CATEGORY
            WHERE ID = :id
        """, nativeQuery = true)
    int deleteNativeById(@Param("id") Long id);

    /// Native Search
    @Query(
            value = """
            SELECT *
            FROM PMH_GROUP_CATEGORY
            WHERE (:paramName IS NULL OR UPPER(PARAM_NAME) LIKE UPPER('%' || :paramName || '%'))
              AND (:paramValue IS NULL OR UPPER(PARAM_VALUE) LIKE UPPER('%' || :paramValue || '%'))
              AND (:paramType IS NULL OR UPPER(PARAM_TYPE) LIKE UPPER('%' || :paramType || '%'))
            ORDER BY ID DESC
            """,
            countQuery = """
            SELECT COUNT(1)
            FROM PMH_GROUP_CATEGORY
            WHERE (:paramName IS NULL OR UPPER(PARAM_NAME) LIKE UPPER('%' || :paramName || '%'))
              AND (:paramValue IS NULL OR UPPER(PARAM_VALUE) LIKE UPPER('%' || :paramValue || '%'))
              AND (:paramType IS NULL OR UPPER(PARAM_TYPE) LIKE UPPER('%' || :paramType || '%'))
            """,
            nativeQuery = true
    )
    Page<GroupCategory> searchNativeSimple(
            @Param("paramName") String paramName,
            @Param("paramValue") String paramValue,
            @Param("paramType") String paramType,
            Pageable pageable
    );
}
