package com.spacenx.space.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.space.enums.MemberRole;
import com.spacenx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_space_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"space_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private MemberRole role;
}
