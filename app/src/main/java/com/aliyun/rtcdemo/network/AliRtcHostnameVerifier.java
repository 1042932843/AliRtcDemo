package com.aliyun.rtcdemo.network;


import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;


public class AliRtcHostnameVerifier implements javax.net.ssl.HostnameVerifier {
    public static final AliRtcHostnameVerifier INSTANCE = new AliRtcHostnameVerifier();
    private static final int ALT_DNS_NAME = 2;
    private static final int ALT_IPA_NAME = 7;
    private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

    AliRtcHostnameVerifier() {
    }

    @Override
    public boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certificates = session.getPeerCertificates();
            return this.verify(host, (X509Certificate) certificates[0]);
        } catch (SSLException var4) {
            return false;
        }
    }

    public boolean verify(String host, X509Certificate certificate) {
        return verifyAsIpAddress(host) ? this.verifyIpAddress(host, certificate) : this.verifyHostname(host, certificate);
    }

    private boolean verifyIpAddress(String ipAddress, X509Certificate certificate) {
        List<String> altNames = getSubjectAltNames(certificate, 7);
        int i = 0;

        for (int size = altNames.size(); i < size; ++i) {
            if (ipAddress.equalsIgnoreCase((String) altNames.get(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean verifyHostname(String hostname, X509Certificate certificate) {
        hostname = hostname.toLowerCase(Locale.US);
        List<String> altNames = getSubjectAltNames(certificate, 2);
        Iterator var4 = altNames.iterator();

        String altName;
        do {
            if (!var4.hasNext()) {
                return false;
            }

            altName = (String) var4.next();
        } while (!this.verifyHostname(hostname, altName));

        return true;
    }

    public static List<String> allSubjectAltNames(X509Certificate certificate) {
        List<String> altIpaNames = getSubjectAltNames(certificate, 7);
        List<String> altDnsNames = getSubjectAltNames(certificate, 2);
        List<String> result = new ArrayList(altIpaNames.size() + altDnsNames.size());
        result.addAll(altIpaNames);
        result.addAll(altDnsNames);
        return result;
    }

    private static List<String> getSubjectAltNames(X509Certificate certificate, int type) {
        ArrayList result = new ArrayList();

        try {
            Collection<?> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames == null) {
                return Collections.emptyList();
            } else {
                Iterator var4 = subjectAltNames.iterator();

                while (var4.hasNext()) {
                    Object subjectAltName = var4.next();
                    List<?> entry = (List) subjectAltName;
                    if (entry != null && entry.size() >= 2) {
                        Integer altNameType = (Integer) entry.get(0);
                        if (altNameType != null && altNameType.intValue() == type) {
                            String altName = (String) entry.get(1);
                            if (altName != null) {
                                result.add(altName);
                            }
                        }
                    }
                }

                return result;
            }
        } catch (CertificateParsingException var9) {
            return Collections.emptyList();
        }
    }

    public boolean verifyHostname(String hostname, String pattern) {
        if (hostname != null && hostname.length() != 0 && !hostname.startsWith(".") && !hostname.endsWith("..")) {
            if (pattern != null && pattern.length() != 0 && !pattern.startsWith(".") && !pattern.endsWith("..")) {
                if (!hostname.endsWith(".")) {
                    hostname = hostname + '.';
                }

                if (!pattern.endsWith(".")) {
                    pattern = pattern + '.';
                }

                pattern = pattern.toLowerCase(Locale.US);
                if (!pattern.contains("*")) {
                    return hostname.equals(pattern);
                } else if (pattern.startsWith("*.") && pattern.indexOf(42, 1) == -1) {
                    if (hostname.length() < pattern.length()) {
                        return false;
                    } else if ("*.".equals(pattern)) {
                        return false;
                    } else {
                        String suffix = pattern.substring(1);
                        if (!hostname.endsWith(suffix)) {
                            return false;
                        } else {
                            int suffixStartIndexInHostname = hostname.length() - suffix.length();
                            return suffixStartIndexInHostname <= 0 || hostname.lastIndexOf(46, suffixStartIndexInHostname - 1) == -1;
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }

}
