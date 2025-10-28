package com.techhub.app.userservice.entity;

import com.techhub.app.commonservice.jpa.BooleanToYNStringConverter;
import com.techhub.app.commonservice.jpa.PostgreSQLEnumType;
import com.techhub.app.userservice.enums.UserRoleEnum;
import com.techhub.app.userservice.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "userRoles" })
public class User {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", unique = true, length = 255)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.userservice.enums.UserRoleEnum"))
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    private UserRoleEnum role;

    @Type(type = "pgsql_enum", parameters = @Parameter(name = "enumClass", value = "com.techhub.app.userservice.enums.UserStatus"))
    @Column(name = "status", columnDefinition = "user_status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Convert(converter = BooleanToYNStringConverter.class)
    @Column(name = "is_active", nullable = false, length = 1)
    private Boolean isActive = true;

    // Fix: Use the correct UserRole entity for the relationship
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserRole> userRoles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }
}
