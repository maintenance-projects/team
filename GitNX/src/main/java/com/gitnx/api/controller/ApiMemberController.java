package com.gitnx.api.controller;

import com.gitnx.api.dto.AddMemberRequest;
import com.gitnx.api.dto.ChangeRoleRequest;
import com.gitnx.api.dto.MemberDto;
import com.gitnx.repository.service.RepositoryMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories/{owner}/{repo}/members")
@RequiredArgsConstructor
public class ApiMemberController {

    private final RepositoryMemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberDto>> listMembers(
            @PathVariable String owner,
            @PathVariable String repo) {
        List<MemberDto> members = memberService.getMembers(owner, repo)
                .stream()
                .map(MemberDto::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping
    public ResponseEntity<Void> addMember(
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody AddMemberRequest request) {
        memberService.addMember(owner, repo, request.getUsername(), request.getRole(), owner);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<Void> changeRole(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeRoleRequest request) {
        memberService.changeRole(owner, repo, memberId, request.getRole(), owner);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long memberId) {
        memberService.removeMember(owner, repo, memberId, owner);
        return ResponseEntity.noContent().build();
    }
}
