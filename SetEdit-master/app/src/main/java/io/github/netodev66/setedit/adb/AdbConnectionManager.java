package io.github.netodev66.setedit.adb;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/**
 * Keeps the RSA identity this app uses to talk to the device's own ADB daemon.
 *
 * <p>Wireless debugging pairs the app's public key with {@code adbd}, which then remembers it.
 * That is why the key pair is written to private storage: pairing is a one-time step, and every
 * later connection reuses the same key without asking the user again.
 */
public final class AdbConnectionManager extends AbsAdbConnectionManager {
    private static final String KEY_FILE = "adb_private.key";
    private static final String CERT_FILE = "adb_cert.pem";
    private static final String CERT_SUBJECT = "CN=SetEdit";
    private static final long CERT_VALIDITY_MILLIS = 100L * 365L * 24L * 60L * 60L * 1000L;

    @Nullable
    private static volatile AdbConnectionManager instance;

    @NonNull
    private final PrivateKey privateKey;
    @NonNull
    private final Certificate certificate;

    private AdbConnectionManager(@NonNull Context context) throws Exception {
        setApi(Build.VERSION.SDK_INT);
        Context appContext = context.getApplicationContext();
        PrivateKey storedKey = readPrivateKey(appContext);
        Certificate storedCertificate = readCertificate(appContext);
        if (storedKey == null || storedCertificate == null) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"));
            java.security.KeyPair generated = generator.generateKeyPair();
            storedKey = generated.getPrivate();
            storedCertificate = generateCertificate(generated);
            writePrivateKey(appContext, storedKey);
            writeCertificate(appContext, storedCertificate);
        }
        privateKey = storedKey;
        certificate = storedCertificate;
    }

    /** Returns the shared manager, creating the key pair on first use. */
    @NonNull
    public static AdbConnectionManager getInstance(@NonNull Context context) throws Exception {
        AdbConnectionManager local = instance;
        if (local == null) {
            synchronized (AdbConnectionManager.class) {
                local = instance;
                if (local == null) {
                    local = new AdbConnectionManager(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    @NonNull
    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @NonNull
    @Override
    protected Certificate getCertificate() {
        return certificate;
    }

    @NonNull
    @Override
    protected String getDeviceName() {
        return "SetEdit";
    }

    /** Builds the self-signed certificate that carries the public key to {@code adbd}. */
    @NonNull
    private static Certificate generateCertificate(@NonNull java.security.KeyPair keyPair) throws Exception {
        X500Name subject = new X500Name(CERT_SUBJECT);
        long now = System.currentTimeMillis();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                new Date(now - 60_000L),
                new Date(now + CERT_VALIDITY_MILLIS),
                subject,
                keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    @Nullable
    private static PrivateKey readPrivateKey(@NonNull Context context) throws Exception {
        File file = new File(context.getFilesDir(), KEY_FILE);
        if (!file.exists()) return null;
        byte[] encoded = new byte[(int) file.length()];
        try (InputStream is = new FileInputStream(file)) {
            int read = 0;
            while (read < encoded.length) {
                int count = is.read(encoded, read, encoded.length - read);
                if (count < 0) break;
                read += count;
            }
        }
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    @Nullable
    private static Certificate readCertificate(@NonNull Context context) throws Exception {
        File file = new File(context.getFilesDir(), CERT_FILE);
        if (!file.exists()) return null;
        try (InputStream is = new FileInputStream(file)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    private static void writePrivateKey(@NonNull Context context, @NonNull PrivateKey key) throws IOException {
        try (OutputStream os = new FileOutputStream(new File(context.getFilesDir(), KEY_FILE))) {
            os.write(key.getEncoded());
        }
    }

    private static void writeCertificate(@NonNull Context context, @NonNull Certificate certificate)
            throws Exception {
        try (OutputStream os = new FileOutputStream(new File(context.getFilesDir(), CERT_FILE))) {
            os.write(certificate.getEncoded());
        }
    }
}
