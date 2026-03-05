package com.spacenx.shortcut.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.space.entity.Space;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_shortcut")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shortcut extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    private String description;
}
