package com.spacenx.issue.entity;

import com.spacenx.space.entity.Space;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_label", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"space_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Column(nullable = false)
    private String name;

    private String color;
}
