package net.qiujuer.library.planck.file;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simply perform MD5 operation to obtain the unique cache file name
 *
 * @author qiujuer Email: qiujuer@live.cn
 * @version 1.0.0
 * Create at: 2018/8/8
 */
public class Md5FileNameGenerator implements FileNameGenerator {
    private static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    @Override
    public String generatePlanckCacheFileName(String url) {
        return computeMD5(url);
    }
}
