package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.ParamsCreateApiRequest;
import io.github.maksim0840.apigateway.dto.api.ParamsEditApiRequest;
import io.github.maksim0840.apigateway.dto.api.ParamsRenameApiRequest;
import io.github.maksim0840.apigateway.security.JwtPrincipal;
import io.github.maksim0840.apigateway.service.ParsingParamRemoteService;
import io.github.maksim0840.internalapi.user.v1.dto.ParsingParamDTO;
import io.github.maksim0840.parsing_param.v1.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/params")
public class ParamsController {

    private final ParsingParamRemoteService parsingParamRemoteService;

    public ParamsController(ParsingParamRemoteService parsingParamRemoteService) {
        this.parsingParamRemoteService = parsingParamRemoteService;
    }

    @PostMapping("/create")
    public void createParam(@RequestBody ParamsCreateApiRequest request, @AuthenticationPrincipal JwtPrincipal principal) {
        parsingParamRemoteService.createParsingParam(
                principal.userId(),
                request.name(),
                request.htmlParserParams(),
                request.htmlPreprocessingParams(),
                request.llmParams()
        );
    }

    @PostMapping("/edit")
    public void editParam(@RequestBody ParamsEditApiRequest request, @AuthenticationPrincipal JwtPrincipal principal) {
        parsingParamRemoteService.editParsingParam(
                request.id(),
                principal.userId(),
                request.name(),
                request.htmlParserParams(),
                request.htmlPreprocessingParams(),
                request.llmParams()
        );
    }

    @GetMapping("/names")
    public List<String> getParamNames(@AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.userId();
        return parsingParamRemoteService.getNamesByUserId(userId);
    }

    @GetMapping("/fullParam/{name}")
    public ParsingParamDTO getParam(@PathVariable String name, @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.userId();
        return parsingParamRemoteService.getByUserIdAndName(userId, name);
    }

    @PatchMapping("/rename")
    public void renameParam(@RequestBody ParamsRenameApiRequest request, @AuthenticationPrincipal JwtPrincipal principal) {
        parsingParamRemoteService.renameByUserIdAndName(
                principal.userId(),
                request.oldName(),
                request.newName()
        );
    }

    @DeleteMapping("/{name}")
    public void deleteParam(@PathVariable String name, @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.userId();
        parsingParamRemoteService.deleteByUserIdAndName(userId, name);
    }
}
