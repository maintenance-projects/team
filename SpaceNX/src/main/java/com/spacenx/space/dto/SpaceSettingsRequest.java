package com.spacenx.space.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpaceSettingsRequest {
    private String name;
    private String spaceKey;
    private String description;
}
