package com.gitnx.api.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.RepositoryDto;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class ApiOrganizationController {

    private final OrganizationService organizationService;
    private final GitRepositoryService gitRepositoryService;

    /**
     * 모든 Organization 목록 반환
     * 프론트엔드에서 레포 추가 시 org 목록을 구성하는 데 사용
     */
    @GetMapping
    public ResponseEntity<List<OrgDto>> listOrganizations() {
        List<Organization> orgs = organizationService.listAll();
        List<OrgDto> result = orgs.stream().map(OrgDto::from).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 Organization의 레포지토리 목록 반환
     */
    @GetMapping("/{orgName}/repositories")
    public ResponseEntity<List<RepositoryDto>> listOrgRepositories(@PathVariable String orgName) {
        Organization org = organizationService.getByName(orgName);
        List<RepositoryDto> repos = gitRepositoryService.listByOrganization(org.getId());
        return ResponseEntity.ok(repos);
    }

    @Getter
    @Builder
    public static class OrgDto {
        private Long id;
        private String name;
        private String description;
        private String ownerUsername;

        public static OrgDto from(Organization org) {
            return OrgDto.builder()
                    .id(org.getId())
                    .name(org.getName())
                    .description(org.getDescription())
                    .ownerUsername(org.getOwner().getUsername())
                    .build();
        }
    }
}
