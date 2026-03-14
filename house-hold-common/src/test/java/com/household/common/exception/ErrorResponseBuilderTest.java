package com.household.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponseBuilder 单元测试")
class ErrorResponseBuilderTest {

    /** 验证 error 方法返回包含 code 和 message 的 Map */
    @Test
    @DisplayName("error 返回包含 code 和 message 的 Map")
    void error_returnsMapWithCodeAndMessage() {
        Map<String, String> result = ErrorResponseBuilder.error("TEST_CODE", "测试消息");

        assertThat(result)
                .containsEntry("code", "TEST_CODE")
                .containsEntry("message", "测试消息")
                .hasSize(2);
    }

    /** 验证不同的 code 和 message 值能正确映射 */
    @Test
    @DisplayName("不同参数值正确映射")
    void error_differentValues_correctlyMapped() {
        Map<String, String> result = ErrorResponseBuilder.error("NOT_FOUND", "资源未找到");

        assertThat(result)
                .containsEntry("code", "NOT_FOUND")
                .containsEntry("message", "资源未找到");
    }
}
