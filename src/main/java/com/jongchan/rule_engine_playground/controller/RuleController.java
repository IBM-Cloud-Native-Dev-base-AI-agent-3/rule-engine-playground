package com.jongchan.rule_engine_playground.controller;

import com.jongchan.rule_engine_playground.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@CrossOrigin(origins = "*")
public class RuleController {

    private final RuleService ruleService;

    @Autowired
    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * 현재 저장된 모든 규칙 목록을 DB로부터 조회하여 JSON 문자열로 반환합니다.
     */
    @GetMapping
    public ResponseEntity<String> getRules() {
        try {
            String rulesJson = ruleService.getRulesJsonFromDb();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(rulesJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("규칙 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 새로운 규칙 목록(JSON)을 받아서 DB의 규칙들을 업데이트합니다.
     */
    @PostMapping
    public ResponseEntity<String> updateRules(@RequestBody String jsonRules) {
        try {
            ruleService.saveRulesJsonToDb(jsonRules);
            return ResponseEntity.ok("DB 규칙이 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("DB 규칙 업데이트 실패: " + e.getMessage());
        }
    }



    /**
     * 특정 공고 ID에 대하여 DTO 매개변수로 하나씩 전달받은 신청자 정보로 실시간 자격 평가를 수행합니다.
     */
    @PostMapping("/evaluate/{announcementId}")
    public ResponseEntity<?> evaluateRules(
            @PathVariable("announcementId") Long announcementId,
            @RequestBody com.jongchan.rule_engine_playground.dto.RentalHomeRequestDto evaluationRequest) {
        try {
            // DTO가 제공하는 중첩 구조의 Facts Map 포맷으로 자동 변환합니다.
            Map<String, Object> factsMap = evaluationRequest.toFactsMap();
            
            com.jongchan.rule_engine_playground.dto.RentalHomeResponseDto evaluationResult = ruleService.evaluate(announcementId, factsMap);
            return ResponseEntity.ok(evaluationResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
