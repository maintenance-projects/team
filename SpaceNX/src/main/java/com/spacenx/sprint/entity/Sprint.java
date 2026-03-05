package com.spacenx.sprint.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.issue.entity.Issue;
import com.spacenx.space.entity.Space;
import com.spacenx.sprint.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "snx_sprint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    @OneToMany(mappedBy = "sprint")
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    public Integer getTotalPoints() {
        return issues.stream()
                .filter(i -> i.getStoryPoints() != null)
                .mapToInt(Issue::getStoryPoints)
                .sum();
    }
}
