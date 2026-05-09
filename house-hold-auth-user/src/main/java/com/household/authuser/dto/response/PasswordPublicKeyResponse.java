package com.household.authuser.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码传输加密公钥响应。
 *
 * <p>publicKey 为 X.509 SubjectPublicKeyInfo DER 的 Base64 编码，前端使用 Web Crypto
 * 以 RSA-OAEP/SHA-256 导入后加密密码。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordPublicKeyResponse {
    private String keyId;
    private String algorithm;
    private String publicKey;
}
