package com.example.demo.security.user;

import com.example.demo.user.entity.Acount;
import com.example.demo.user.entity.AcountRole;
import com.example.demo.user.entity.Permission;
import com.example.demo.user.entity.RolePermission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
/// triển khai interface userDetails của spring security
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;

    /// constructor nhận Acount
    public CustomUserDetails(Acount acount){
        this.id = acount.getId();
        this.email = acount.getEmail();
        this.password = acount.getPassword();
        this.name = acount.getName();
        this.authorities = buildAuthorities(acount);
    }

    /// xây dựng dnah sách quyền cho user từ Acount
    /// role, permission
    private Collection<? extends GrantedAuthority> buildAuthorities(Acount acount){
        /// tạo Set để chứa quyền
        Set<GrantedAuthority> result = new LinkedHashSet<>();

        /// nếu acount không có role thì trả rỗng
        if(acount.getAcountRoles() == null){
            return result;
        }

        /// duyệt từng role của acount
        for(AcountRole acountRole : acount.getAcountRoles()){
            /// nếu role null thì bỏ qua
            if(acountRole.getRole() == null){
                continue;
            }

            /// lấy tên roll nếu hợp lệ thì thêm vào authorities
            String roleName = acountRole.getRole().getName();
            if(roleName != null && !roleName.isBlank()){
                result.add(new SimpleGrantedAuthority(roleName));
            }

            /// kiểm tra roll có permission không
            if(acountRole.getRole().getRolePermissions() != null){
                /// duyệt từng roll permission
                for (RolePermission rp : acountRole.getRole().getRolePermissions()){
                    Permission permission = rp.getPermission();
                    /// nếu permission null thfi bỏ qua
                    if(permission == null){
                        continue;
                    }

                    /// nếu permissionName hợp lệ thì thêm vào authorities
                    String permissionName = permission.getName();
                    if(permissionName != null && !permissionName.isBlank()){
                        result.add(new SimpleGrantedAuthority(permissionName));
                    }
                }
            }
        }
        return result;
    }


    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
