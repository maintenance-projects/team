package com.gitnx.repository.entity;

import com.gitnx.common.entity.BaseTimeEntity;
import com.gitnx.organization.entity.Organization;
import com.gitnx.repository.enums.RepositoryVisibility;
import com.gitnx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ng_repository", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_id", "name", "organization_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRepository extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RepositoryVisibility visibility = RepositoryVisibility.PRIVATE;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String defaultBranch = "main";

    @Column(nullable = false)
    private String diskPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
