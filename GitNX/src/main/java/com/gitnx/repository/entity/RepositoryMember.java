package com.gitnx.repository.entity;

import com.gitnx.common.entity.BaseTimeEntity;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ng_repository_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository gitRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepositoryRole role;
}
