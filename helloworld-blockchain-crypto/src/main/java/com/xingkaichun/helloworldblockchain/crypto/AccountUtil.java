package com.xingkaichun.helloworldblockchain.crypto;

import com.xingkaichun.helloworldblockchain.crypto.model.Account;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;

/**
 * 账户工具类
 *
 * @author 邢开春 409060350@qq.com
 */
public class AccountUtil {

    private static final Logger logger = LoggerFactory.getLogger(AccountUtil.class);

    private static final ECDomainParameters CURVE;
    private static final SecureRandom SECURE_RANDOM;
    private static final boolean COMPRESSED = false;
    private static final BigInteger HALF_CURVE_ORDER;


    private static final byte VERSION = 0x00;

    static {
        JavaCryptographyExtensionProviderUtil.addBouncyCastleProvider();
    }

    static {
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        SECURE_RANDOM = new SecureRandom();
        HALF_CURVE_ORDER = CURVE.getN().shiftRight(1);
    }

    /**
     * Returns true if the S component is "low", that means it is below {@link ECKey#HALF_CURVE_ORDER}. See <a
     * href="https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures">BIP62</a>.
     * 参考：bitcoinj-core-0.15.10.jar org.bitcoinj.core.ECKey.isCanonical()
     */
    private static boolean isCanonical(BigInteger s) {
        return s.compareTo(HALF_CURVE_ORDER) <= 0;
    }

    /**
     * 随机生成一个账户
     */
    public static Account randomAccount() {
        try {
            ECKeyPairGenerator generator = new ECKeyPairGenerator();
            ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(CURVE, SECURE_RANDOM);
            generator.init(keygenParams);
            AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
            ECPrivateKeyParameters ecPrivateKeyParameters = (ECPrivateKeyParameters) keypair.getPrivate();
            ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
            BigInteger bigIntegerPrivateKey = ecPrivateKeyParameters.getD();
            byte[] pub = pubParams.getQ().getEncoded(COMPRESSED);
            String privateKey = encodePrivateKey(bigIntegerPrivateKey);
            String publicKey = encodePublicKey(pub);
            String publicKeyHash = publicKeyHashFromPublicKey(publicKey);
            String address = addressFromPublicKey(publicKey);
            Account account = new Account(privateKey,publicKey,publicKeyHash,address);
            return account;
        } catch (Exception e) {
            logger.debug("生成账户失败。",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 私钥生成账户
     */
    public static Account accountFromPrivateKey(String privateKey) {
        try {
            privateKey = formatPrivateKey(privateKey);

            BigInteger bigIntegerPrivateKey = privateKeyFrom(privateKey);
            byte[] ecPublicKey = publicKeyFromPrivateKey(bigIntegerPrivateKey);

            String publicKey = encodePublicKey(ecPublicKey);
            String publicKeyHash = publicKeyHashFromPublicKey(publicKey);
            String address = addressFromPublicKey(publicKey);
            Account account = new Account(privateKey,publicKey,publicKeyHash,address);
            return account;
        } catch (Exception e) {
            logger.debug("从私钥恢复账户失败。",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 公钥生成地址
     */
    public static String addressFromPublicKey(String publicKey) {
        try {
            byte[] bytePublicKey = HexUtil.hexStringToBytes(publicKey);
            return base58AddressFromPublicKey(bytePublicKey);
        } catch (Exception e) {
            logger.debug("公钥生成地址失败。",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 公钥哈希生成地址
     */
    public static String addressFromPublicKeyHash(String publicKeyHash) {
        try {
            byte[] bytesPublicKeyHash = HexUtil.hexStringToBytes(publicKeyHash);
            return base58AddressFromPublicKeyHash(bytesPublicKeyHash);
        } catch (Exception e) {
            logger.debug("公钥哈希生成地址失败。",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 公钥生成公钥哈希
     */
    public static String publicKeyHashFromPublicKey(String publicKey) {
        try {
            byte[] bytesPublicKey = publicKeyFrom(publicKey);
            byte[] publicKeyHash = publicKeyHashFromPublicKey(bytesPublicKey);
            return HexUtil.bytesToHexString(publicKeyHash);
        } catch (Exception e) {
            logger.debug("公钥生成公钥哈希失败。",e);
            throw new RuntimeException(e);
        }
    }
    /**
     * 公钥生成公钥哈希
     * 对公钥进行两次哈希(第一次采用SHA256算法进行哈希，第二次采用RipeMD160算法进行哈希)得到的结果，就是公钥哈希
     */
    private static byte[] publicKeyHashFromPublicKey(byte[] publicKey) {
        byte[] publicKeyHash = RipeMD160Util.digest(SHA256Util.digest(publicKey));
        return publicKeyHash;
    }

    /**
     * 地址生成公钥哈希
     */
    public static String publicKeyHashFromAddress(String address) {
        try {
            byte[] bytesAddress = Base58Util.decode(address);
            byte[] bytePublicKeyHash = new byte[20];
            System.arraycopy(bytesAddress, 1, bytePublicKeyHash, 0, 20);
            return HexUtil.bytesToHexString(bytePublicKeyHash);
        } catch (Exception e) {
            logger.debug("地址生成公钥哈希失败。",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 签名
     */
    public static String signature(String privateKey, String message) {
        try {
            BigInteger bigIntegerPrivateKey = privateKeyFrom(privateKey);
            byte[] bytesMessage = HexUtil.hexStringToBytes(message);
            byte[] bytesSignature = signature(bigIntegerPrivateKey,bytesMessage);
            return HexUtil.bytesToHexString(bytesSignature);
        } catch (Exception e) {
            logger.debug("签名出错。");
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证签名
     */
    public static boolean verifySignature(String publicKey, String message, String signature) {
        try {
            byte[] bytesPublicKey = publicKeyFrom(publicKey);
            byte[] bytesMessage = HexUtil.hexStringToBytes(message);
            byte[] bytesSignature = HexUtil.hexStringToBytes(signature);
            return verifySignature(bytesPublicKey,bytesMessage,bytesSignature);
        }catch(Exception e) {
            logger.debug("验证签名出错。");
            return false;
        }
    }



    /**
     * 由原始私钥推导出原始公钥
     */
    private static byte[] publicKeyFromPrivateKey(BigInteger bigIntegerPrivateKey) {
        byte[] bytePublicKey = CURVE.getG().multiply(bigIntegerPrivateKey).getEncoded(COMPRESSED);
        return bytePublicKey;
    }
    /**
     * 由编码私钥解码出原始私钥
     */
    private static BigInteger privateKeyFrom(String privateKey) {
        BigInteger bigIntegerPrivateKey = new BigInteger(privateKey,16);
        return bigIntegerPrivateKey;
    }
    /**
     * 由编码公钥解码出原始公钥
     */
    private static byte[] publicKeyFrom(String publicKey) {
        byte[] bytePublicKey = HexUtil.hexStringToBytes(publicKey);
        return bytePublicKey;
    }
    /**
     * 将原始私钥进行编码操作，生成编码私钥
     */
    private static String encodePrivateKey(BigInteger bigIntegerPrivateKey) {
        String hexPrivateKey = bigIntegerPrivateKey.toString(16);
        return formatPrivateKey(hexPrivateKey);
    }

    /**
     * 将原始公钥进行编码操作，生成编码公钥
     */
    private static String encodePublicKey(byte[] bytePublicKey) {
        String hexPublicKey = HexUtil.bytesToHexString(bytePublicKey);
        return hexPublicKey;
    }

    /**
     * 签名
     */
    private static byte[] signature(BigInteger bigIntegerPrivateKey, byte[] message) {
        try {
            ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
            ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(bigIntegerPrivateKey, CURVE);
            signer.init(true, ecPrivateKeyParameters);
            BigInteger[] sigs = signer.generateSignature(message);
            BigInteger s = sigs[1];
            if (!isCanonical(s)) {
                s = CURVE.getN().subtract(s);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DERSequenceGenerator seq = new DERSequenceGenerator(bos);
            seq.addObject(new ASN1Integer(sigs[0]));
            seq.addObject(new ASN1Integer(s));
            seq.close();
            return bos.toByteArray();
        } catch (Exception e) {
            logger.debug("签名出错。");
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证签名
     */
    private static boolean verifySignature(byte[] pub, byte[] message, byte[] signature) {
        try {
            ECDSASigner signer = new ECDSASigner();
            ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
            signer.init(false, ecPublicKeyParameters);
            ASN1InputStream decoder = new ASN1InputStream(signature);
            DLSequence seq = (DLSequence) decoder.readObject();
            ASN1Integer r = (ASN1Integer) seq.getObjectAt(0);
            ASN1Integer s = (ASN1Integer) seq.getObjectAt(1);
            if (!isCanonical(s.getValue())) {
                return false;
            }
            decoder.close();
            return signer.verifySignature(message, r.getValue(), s.getValue());
        } catch (Exception e) {
            logger.debug("验证签名，出错。");
            return false;
        }
    }

    /**
     * 公钥生成base58格式地址
     */
    private static String base58AddressFromPublicKey(byte[] bytePublicKey) {
        byte[] publicKeyHash = publicKeyHashFromPublicKey(bytePublicKey);
        return base58AddressFromPublicKeyHash(publicKeyHash);
    }

    /**
     * 公钥哈希生成base58格式地址
     */
    private static String base58AddressFromPublicKeyHash(byte[] publicKeySha256DigestRipeMD160Digest) {
        //地址数组
        byte[] byteAddress = new byte[1 + 20 + 4];

        //将地址的版本号0x00存储进地址数组
        byteAddress[0] = VERSION;
        System.arraycopy(publicKeySha256DigestRipeMD160Digest, 0, byteAddress, 1, 20);

        //计算公钥Hash
        byte[] versionAndPubKSha256RipeMD160 = new byte[21];
        System.arraycopy(byteAddress, 0, versionAndPubKSha256RipeMD160, 0, 21);
        byte[] doubleSHA256 = SHA256Util.doubleDigest(versionAndPubKSha256RipeMD160);

        //取前四位作为地址校验码，将校验码前四位加到地址数组的末四位
        System.arraycopy(doubleSHA256, 0, byteAddress, 21, 4);

        //用Base58编码地址数组
        String base58Address = Base58Util.encode(byteAddress);
        return base58Address;
    }

    /**
     * 格式化私钥
     * 前置填零，返回[长度为64位][十六进制字符串形式的]私钥
     */
    private static String formatPrivateKey(String privateKey) {
        //私钥长度是256bit，64位十六进制的字符串数，如果传入的私钥长度不够，这里进行前置补充零操作。
        final int length = 64;
        if(privateKey.length()<length){
            privateKey = (String.join("", Collections.nCopies(length-privateKey.length(), "0")))+privateKey;
        }
        return privateKey;
    }

    /**
     * 是否是合法的地址
     */
    public static boolean isPayToPublicKeyHashAddress(String address) {
        try {
            byte[] bytesAddress = Base58.decode(address);
            byte[] bytePublicKeyHash = new byte[20];
            System.arraycopy(bytesAddress, 1, bytePublicKeyHash, 0, 20);
            String base58Address = addressFromPublicKeyHash(HexUtil.bytesToHexString(bytePublicKeyHash));
            return base58Address.equals(address);
        }catch (Exception e){
            logger.debug(String.format("地址[%s]不是base58格式的地址。",address));
            return false;
        }
    }
}
