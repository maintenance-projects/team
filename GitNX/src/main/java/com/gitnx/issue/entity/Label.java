package com.gitnx.issue.entity;

import com.gitnx.common.entity.BaseTimeEntity;
import com.gitnx.repository.entity.GitRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ng_label", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository gitRepository;
}
