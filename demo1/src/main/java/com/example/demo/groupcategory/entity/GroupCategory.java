package com.example.demo.groupcategory.entity;

import com.example.demo.groupcategory.constant.GroupCategoryConstant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PMH_GROUP_CATEGORY")
public class GroupCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmh_group_cat_seq")
    @SequenceGenerator(
            name = "pmh_group_cat_seq",
            sequenceName = "PMH_GROUP_CATEGORY_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARAM_NAME", length = 255, nullable = false )
    private String paramName;

    @Column(name = "PARAM_VALUE", length = 255, nullable = false)
    private String paramValue;

    @Column (name = "PARAM_TYPE", length = 255, nullable = false)
    private String paramType;

    @Column (name = "DESCRIPTION", length = 4000)
    private String description;

    @Column (name = "COMPONENT_CODE", length = 255)
    private String componentCode;

    @Column (name = "STATUS", nullable = false)
    private Integer status;

    @Column (name = "IS_ACTIVE", nullable = false)
    private Integer isActive;

    @Column (name = "IS_DISPLAY")
    private Integer isDisplay;

    @Column(name = "NEW_DATA", length = 4000)
    private String newData;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDate effectiveDate;

    @Column (name = "END_EFFECTIVE_DATE")
    private LocalDate endEffectiveDate;

    public void markAsDraft() {
        this.status = GroupCategoryConstant.STATUS_DRAFT;
    }

    public void markAsPending() {
        this.status = GroupCategoryConstant.STATUS_PENDING;
    }

    public void markAsApproved() {
        this.status = GroupCategoryConstant.STATUS_APPROVED;
    }

    public void markAsRejected() {
        this.status = GroupCategoryConstant.STATUS_REJECTED;
    }

    public void markAsCancelApproved() {
        this.status = GroupCategoryConstant.STATUS_CANCEL_APPROVE;
    }

    public void applyDefaultFlags(Integer isActive, Integer isDisplay) {
        this.isActive = isActive == null ? GroupCategoryConstant.ACTIVE_DEFAULT : isActive;
        this.isDisplay = isDisplay == null ? GroupCategoryConstant.DISPLAY_HIDDEN : isDisplay;
    }

    public void updateOptionalFlags(Integer isActive, Integer isDisplay) {
        if (isActive != null) {
            this.isActive = isActive;
        }
        if (isDisplay != null) {
            this.isDisplay = isDisplay;
        }
    }

    public void show() {
        this.isDisplay = GroupCategoryConstant.DISPLAY_VISIBLE;
    }

    public void hide() {
        this.isDisplay = GroupCategoryConstant.DISPLAY_HIDDEN;
    }

    public void replaceNewData(String newData) {
        this.newData = newData;
    }

    public void clearNewData() {
        this.newData = null;
    }

    public boolean isPending() {
        return Objects.equals(this.status, GroupCategoryConstant.STATUS_PENDING);
    }

    public boolean isPublished() {
        return Objects.equals(this.status, GroupCategoryConstant.STATUS_APPROVED)
                || Objects.equals(this.isDisplay, GroupCategoryConstant.DISPLAY_VISIBLE);
    }
}
