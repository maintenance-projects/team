package com.spacenx.user.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ng_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String displayName;

    @Column(length = 500)
    private String bio;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
