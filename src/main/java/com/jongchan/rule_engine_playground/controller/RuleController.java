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
     * 특정 공고 ID에 대하여 DB에 정의된 규칙들로 실시간 평가를 수행합니다.
     */
    @PostMapping("/evaluate/{announcementId}")
    public ResponseEntity<?> evaluateRules(
            @PathVariable("announcementId") Long announcementId,
            @RequestBody Map<String, Object> inputFacts) {
        try {
            // 중첩 구조의 맵 데이터 타입을 재귀적으로 정제하여 MVEL 비교 연산 오류 차단
            sanitizeInputFacts(inputFacts);
            
            Map<String, Object> evaluationResult = ruleService.evaluate(announcementId, inputFacts);
            return ResponseEntity.ok(evaluationResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 요청 정보의 문자열 숫자, 불리언을 실제 타입으로 재귀적으로 정제하는 유틸리티
     */
    @SuppressWarnings("unchecked")
    private void sanitizeInputFacts(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Map) {
                sanitizeInputFacts((Map<String, Object>) val);
            } else if (val instanceof String) {
                String str = ((String) val).trim();
                if ("true".equalsIgnoreCase(str)) {
                    entry.setValue(true);
                } else if ("false".equalsIgnoreCase(str)) {
                    entry.setValue(false);
                } else {
                    try {
                        if (str.contains(".")) {
                            entry.setValue(Double.parseDouble(str));
                        } else {
                            entry.setValue(Integer.parseInt(str));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
