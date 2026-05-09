package com.household.authuser.service;

import com.household.authuser.dto.response.PasswordPublicKeyResponse;
import com.household.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordCryptoService 单元测试")
class PasswordCryptoServiceTest {

    /**
     * 验证服务端发布的 RSA-OAEP/SHA-256 公钥可用于加密，且服务端私钥可正确解密。
     */
    @Test
    @DisplayName("可解密 RSA-OAEP-256 加密后的密码")
    void whenPasswordEncryptedWithPublicKey_thenDecrypts() throws Exception {
        PasswordCryptoService service = new PasswordCryptoService();
        PasswordPublicKeyResponse response = service.getPublicKey();

        String encryptedPassword = encryptWithPublicKey(response.getPublicKey(), "SecurePass123!");

        assertThat(response.getAlgorithm()).isEqualTo("RSA-OAEP-256");
        assertThat(response.getKeyId()).isNotBlank();
        assertThat(service.decryptPassword(encryptedPassword)).isEqualTo("SecurePass123!");
    }

    /**
     * 验证非法密文不会泄漏底层异常细节，而是转换为统一的业务参数异常。
     */
    @Test
    @DisplayName("非法密文抛出 BadRequestException")
    void whenCiphertextInvalid_thenThrowsBadRequest() {
        PasswordCryptoService service = new PasswordCryptoService();

        assertThatThrownBy(() -> service.decryptPassword("not-base64"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("密码密文无效，请刷新页面后重试");
    }

    private String encryptWithPublicKey(String publicKeyBase64, String password) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSha256Spec());
        byte[] ciphertext = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    private OAEPParameterSpec oaepSha256Spec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }
}
