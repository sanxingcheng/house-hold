package com.household.authuser.service;

import com.household.authuser.dto.response.PasswordPublicKeyResponse;
import com.household.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * 密码传输加密服务。
 *
 * <p>前端使用服务端公钥通过 RSA-OAEP/SHA-256 加密密码，后端仅在内存中用私钥解密，
 * 解密后的明文立即交给 PasswordEncoder 进行哈希校验或存储。</p>
 */
@Slf4j
@Service
public class PasswordCryptoService {
    private static final String KEY_ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String PUBLIC_ALGORITHM = "RSA-OAEP-256";
    private static final int KEY_SIZE = 3072;

    private final KeyPair keyPair;
    private final String keyId;

    public PasswordCryptoService() {
        this.keyPair = generateKeyPair();
        this.keyId = fingerprint(keyPair.getPublic().getEncoded());
        log.info("Password transport encryption key initialized, algorithm={}, keySize={}",
                PUBLIC_ALGORITHM, KEY_SIZE);
    }

    /**
     * 获取当前用于密码传输加密的公钥。
     *
     * @return 公钥标识、算法名和 Base64 编码的 X.509 公钥
     */
    public PasswordPublicKeyResponse getPublicKey() {
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return new PasswordPublicKeyResponse(keyId, PUBLIC_ALGORITHM, publicKey);
    }

    /**
     * 解密前端提交的密码密文。
     *
     * @param encryptedPassword Base64 编码的 RSA-OAEP 密文
     * @return 解密后的密码明文
     */
    public String decryptPassword(String encryptedPassword) {
        if (!StringUtils.hasText(encryptedPassword)) {
            throw new BadRequestException("密码密文不能为空");
        }
        try {
            byte[] ciphertext = Base64.getDecoder().decode(encryptedPassword);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), oaepSha256Spec());
            String password = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(password)) {
                throw new BadRequestException("密码不能为空");
            }
            return password;
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            log.warn("Password ciphertext decrypt failed: {}", e.getClass().getSimpleName());
            throw new BadRequestException("密码密文无效，请刷新页面后重试");
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 加密算法不可用", e);
        }
    }

    private OAEPParameterSpec oaepSha256Spec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }

    private String fingerprint(byte[] encodedPublicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(encodedPublicKey);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 摘要算法不可用", e);
        }
    }
}
