package com.spacenx.space.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.space.enums.SpaceType;
import com.spacenx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_space")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String spaceKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private SpaceType spaceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
