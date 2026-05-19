package com.example.demo.groupcategory.repository.specification;

import com.example.demo.groupcategory.dto.request.GroupCategorySearchReq;
import com.example.demo.groupcategory.entity.GroupCategory;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
public class GroupCategorySpecification {

    public static Specification<GroupCategory> search(GroupCategorySearchReq req) {
        //root : đại diện cho chính bảng products trong Database. Dùng root để trỏ tới các cột vd: root.get("price" tương đương với cột price)
        //query(criteria querry): đại diện cho tổng thể câu lệnh (SELECT, FROM, WHERE, GROUP BY). ít đụng trừ khi dùng viết SUB-Querry
        //criteriaBuilder(CriteriaBuilder): Đây là "Người thợ xây" tạo ra các phép toán trong mệnh đề Where
        //Bất cứ khi nào cần các toán tử như LỊKE, =, <, >, and, or đều phải gọi criteriaBuilder
        return (root, query,    cb) -> {
            //Predicate khởi tạo danh sách các điều kiện
            //đại diện cho 1 điều kiên đơn lẻ
            List<Predicate> ps = new ArrayList<>();

            if (hasText(req.paramType())) {
                ps.add(cb.like(cb.upper(root.get("paramType")), "%" + req.paramType().trim().toUpperCase() + "%"));
            }
            if (hasText(req.paramValue())) {
                ps.add(cb.like(cb.upper(root.get("paramValue")), "%" + req.paramValue().trim().toUpperCase() + "%"));
            }
            if (hasText(req.paramName())) {
                ps.add(cb.like(cb.upper(root.get("paramName")), "%" + req.paramName().trim().toUpperCase() + "%"));
            }

            if (req.status() != null && !req.status().isEmpty()) {
                ps.add(root.get("status").in(req.status()));
            }

                if (req.isActive() != null && !req.isActive().isEmpty()) {
                ps.add(root.get("isActive").in(req.isActive()));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}