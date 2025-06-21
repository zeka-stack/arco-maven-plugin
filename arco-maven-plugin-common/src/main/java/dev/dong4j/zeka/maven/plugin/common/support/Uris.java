package dev.dong4j.zeka.maven.plugin.common.support;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于RFC 3986的URI编解码实用方法.
 * 有两种编码方法:  “encodeXyz”--它们通过百分比编码非法字符 (包括非美国ASCII字符) 对特定的URI组件 (例如路径、查询) 进行编码,
 * 还包括在给定URI组件类型中非法的字符, 如RFC 3986中定义的.
 * 在编码方面, 这种方法的效果相当于使用URI的多参数构造函数.
 * “encode” 和 “encodeUriVariables” —它们可以通过对URI中任何非法或具有保留意义的所有字符进行百分比编码来对URI变量值进行编码.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 * @since 3.0
 */
@SuppressWarnings("all")
public abstract class Uris {

    /**
     * Encode the given URI scheme with the given encoding.
     *
     * @param scheme   the scheme to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded scheme
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeScheme(String scheme, String encoding) {
        return encode(scheme, encoding, Type.SCHEME);
    }

    /**
     * Encode the given URI scheme with the given encoding.
     *
     * @param scheme  the scheme to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded scheme
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeScheme(String scheme, Charset charset) {
        return encode(scheme, charset, Type.SCHEME);
    }

    /**
     * Encode the given URI authority with the given encoding.
     *
     * @param authority the authority to be encoded
     * @param encoding  the character encoding to encode to
     * @return the encoded authority
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeAuthority(String authority, String encoding) {
        return encode(authority, encoding, Type.AUTHORITY);
    }

    /**
     * Encode the given URI authority with the given encoding.
     *
     * @param authority the authority to be encoded
     * @param charset   the character encoding to encode to
     * @return the encoded authority
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeAuthority(String authority, Charset charset) {
        return encode(authority, charset, Type.AUTHORITY);
    }

    /**
     * Encode the given URI user info with the given encoding.
     *
     * @param userInfo the user info to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded user info
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeUserInfo(String userInfo, String encoding) {
        return encode(userInfo, encoding, Type.USER_INFO);
    }

    /**
     * Encode the given URI user info with the given encoding.
     *
     * @param userInfo the user info to be encoded
     * @param charset  the character encoding to encode to
     * @return the encoded user info
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeUserInfo(String userInfo, Charset charset) {
        return encode(userInfo, charset, Type.USER_INFO);
    }

    /**
     * Encode the given URI host with the given encoding.
     *
     * @param host     the host to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded host
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeHost(String host, String encoding) {
        return encode(host, encoding, Type.HOST_IPV4);
    }

    /**
     * Encode the given URI host with the given encoding.
     *
     * @param host    the host to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded host
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeHost(String host, Charset charset) {
        return encode(host, charset, Type.HOST_IPV4);
    }

    /**
     * Encode the given URI port with the given encoding.
     *
     * @param port     the port to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded port
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodePort(String port, String encoding) {
        return encode(port, encoding, Type.PORT);
    }

    /**
     * Encode the given URI port with the given encoding.
     *
     * @param port    the port to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded port
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodePort(String port, Charset charset) {
        return encode(port, charset, Type.PORT);
    }

    /**
     * Encode the given URI path with the given encoding.
     *
     * @param path     the path to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded path
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodePath(String path, String encoding) {
        return encode(path, encoding, Type.PATH);
    }

    /**
     * Encode the given URI path with the given encoding.
     *
     * @param path    the path to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded path
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodePath(String path, Charset charset) {
        return encode(path, charset, Type.PATH);
    }

    /**
     * Encode the given URI path segment with the given encoding.
     *
     * @param segment  the segment to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded segment
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodePathSegment(String segment, String encoding) {
        return encode(segment, encoding, Type.PATH_SEGMENT);
    }

    /**
     * Encode the given URI path segment with the given encoding.
     *
     * @param segment the segment to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded segment
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodePathSegment(String segment, Charset charset) {
        return encode(segment, charset, Type.PATH_SEGMENT);
    }

    /**
     * Encode the given URI query with the given encoding.
     *
     * @param query    the query to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded query
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeQuery(String query, String encoding) {
        return encode(query, encoding, Type.QUERY);
    }

    /**
     * Encode the given URI query with the given encoding.
     *
     * @param query   the query to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded query
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeQuery(String query, Charset charset) {
        return encode(query, charset, Type.QUERY);
    }

    /**
     * Encode the given URI query parameter with the given encoding.
     *
     * @param queryParam the query parameter to be encoded
     * @param encoding   the character encoding to encode to
     * @return the encoded query parameter
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeQueryParam(String queryParam, String encoding) {
        return encode(queryParam, encoding, Type.QUERY_PARAM);
    }

    /**
     * Encode the given URI query parameter with the given encoding.
     *
     * @param queryParam the query parameter to be encoded
     * @param charset    the character encoding to encode to
     * @return the encoded query parameter
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeQueryParam(String queryParam, Charset charset) {
        return encode(queryParam, charset, Type.QUERY_PARAM);
    }

    /**
     * Encode the given URI fragment with the given encoding.
     *
     * @param fragment the fragment to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded fragment
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encodeFragment(String fragment, String encoding) {
        return encode(fragment, encoding, Type.FRAGMENT);
    }

    /**
     * Encode the given URI fragment with the given encoding.
     *
     * @param fragment the fragment to be encoded
     * @param charset  the character encoding to encode to
     * @return the encoded fragment
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encodeFragment(String fragment, Charset charset) {
        return encode(fragment, charset, Type.FRAGMENT);
    }

    /**
     * Variant of {@link #decode(String, Charset)} with a String charset.
     *
     * @param source   the String to be encoded
     * @param encoding the character encoding to encode to
     * @return the encoded String
     * @since 1.0.0
     */
    @Contract("null, _ -> null")
    public static String encode(String source, String encoding) {
        return encode(source, encoding, Type.URI);
    }

    /**
     * Encode all characters that are either illegal, or have any reserved
     * meaning, anywhere within a URI, as defined in
     * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>.
     * This is useful to ensure that the given String will be preserved as-is
     * and will not have any o impact on the structure or meaning of the URI.
     *
     * @param source  the String to be encoded
     * @param charset the character encoding to encode to
     * @return the encoded String
     * @since 5.0
     */
    @Contract("null, _ -> null")
    public static String encode(String source, Charset charset) {
        return encode(source, charset, Type.URI);
    }

    /**
     * Convenience method to apply {@link #encode(String, Charset)} to all
     * given URI variable values.
     *
     * @param uriVariables the URI variable values to be encoded
     * @return the encoded String
     * @since 5.0
     */
    public static @NotNull Map<String, String> encodeUriVariables(@NotNull Map<String, ?> uriVariables) {
        Map<String, String> result = new LinkedHashMap<String, String>(uriVariables.size());
        for (Map.Entry<String, ?> entry : uriVariables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String stringValue = (value != null ? value.toString() : "");
            result.put(key, encode(stringValue, StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * Convenience method to apply {@link #encode(String, Charset)} to all
     * given URI variable values.
     *
     * @param uriVariables the URI variable values to be encoded
     * @return the encoded String
     * @since 5.0
     */
    public static Object[] encodeUriVariables(Object... uriVariables) {
        List<String> result = new ArrayList<>();
        for (Object value : uriVariables) {
            String stringValue = (value != null ? value.toString() : "");
            result.add(encode(stringValue, StandardCharsets.UTF_8));
        }
        return result.toArray();
    }

    /**
     * Encode string
     *
     * @param scheme   scheme
     * @param encoding encoding
     * @param type     type
     * @return the string
     * @since 1.0.0
     */
    @Contract("null, _, _ -> null")
    private static String encode(String scheme, String encoding, Type type) {
        return encodeUriComponent(scheme, encoding, type);
    }

    /**
     * Encode string
     *
     * @param scheme  scheme
     * @param charset charset
     * @param type    type
     * @return the string
     * @since 1.0.0
     */
    @Contract("null, _, _ -> null")
    private static String encode(String scheme, Charset charset, Type type) {
        return encodeUriComponent(scheme, charset, type);
    }

    /**
     * Encode the given source into an encoded String using the rules specified
     * by the given component and with the given options.
     *
     * @param source   the source String
     * @param encoding the encoding of the source String
     * @param type     the URI component for the source
     * @return the encoded URI
     * @throws IllegalArgumentException when the given value is not a valid URI component
     * @since 1.0.0
     */
    @Contract("null, _, _ -> null")
    static String encodeUriComponent(String source, String encoding, Type type) {
        return encodeUriComponent(source, Charset.forName(encoding), type);
    }

    /**
     * Encode the given source into an encoded String using the rules specified
     * by the given component and with the given options.
     *
     * @param source  the source String
     * @param charset the encoding of the source String
     * @param type    the URI component for the source
     * @return the encoded URI
     * @throws IllegalArgumentException when the given value is not a valid URI component
     * @since 1.0.0
     */
    @Contract("null, _, _ -> null")
    static String encodeUriComponent(String source, Charset charset, Type type) {
        if (!(source != null && source.length() > 0)) {
            return source;
        }
        if (charset == null) {
            throw new IllegalArgumentException("Charset must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        byte[] bytes;
        try {
            bytes = source.getBytes(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        boolean changed = false;
        for (byte b : bytes) {
            if (b < 0) {
                b += 256;
            }
            if (type.isAllowed(b)) {
                bos.write(b);
            } else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
                changed = true;
            }
        }
        try {
            return (changed ? new String(bos.toByteArray(), charset.name()) : source);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Uri decode string
     *
     * @param source  source
     * @param charset charset
     * @return the string
     * @since 1.0.0
     */
    public static String uriDecode(@NotNull String source, Charset charset) {
        int length = source.length();
        if (length == 0) {
            return source;
        }
        if (charset == null) {
            throw new IllegalArgumentException("Charset must not be null");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        boolean changed = false;
        for (int i = 0; i < length; i++) {
            int ch = source.charAt(i);
            if (ch == '%') {
                if (i + 2 < length) {
                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }
                    bos.write((char) ((u << 4) + l));
                    i += 2;
                    changed = true;
                } else {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
            } else {
                bos.write(ch);
            }
        }
        try {
            return (changed ? new String(bos.toByteArray(), charset.name()) : source);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decode the given encoded URI component.
     * <p>See {@link Uris#uriDecode(String, Charset)} for the decoding rules.
     *
     * @param source   the encoded String
     * @param encoding the character encoding to use
     * @return the decoded value
     * @throws IllegalArgumentException when the given source contains invalid encoded sequences
     * @see Uris#uriDecode(String, Charset)
     * @see URLDecoder#decode(String, String)
     * @since 1.0.0
     */
    public static String decode(String source, String encoding) {
        return uriDecode(source, Charset.forName(encoding));
    }

    /**
     * Decode the given encoded URI component.
     * <p>See {@link Uris#uriDecode(String, Charset)} for the decoding rules.
     *
     * @param source  the encoded String
     * @param charset the character encoding to use
     * @return the decoded value
     * @throws IllegalArgumentException when the given source contains invalid encoded sequences
     * @see Uris#uriDecode(String, Charset)
     * @see URLDecoder#decode(String, String)
     * @since 5.0
     */
    public static String decode(String source, Charset charset) {
        return uriDecode(source, charset);
    }

    /**
     * Extract the file extension from the given URI path.
     *
     * @param path the URI path (e.g. "/products/index.html")
     * @return the extracted file extension (e.g. "html")
     * @since 4.3.2
     */
    public static @Nullable String extractFileExtension(@NotNull String path) {
        int end = path.indexOf('?');
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
            end = fragmentIndex;
        }
        if (end == -1) {
            end = path.length();
        }
        int begin = path.lastIndexOf('/', end) + 1;
        int paramIndex = path.indexOf(';', begin);
        end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
        int extIndex = path.lastIndexOf('.', end);
        if (extIndex != -1 && extIndex > begin) {
            return path.substring(extIndex + 1, end);
        }
        return null;
    }

    /**
     * Enumeration used to identify the allowed characters per URI component.
     * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 01:02
     * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
     * @since 1.0.0
     */
    public enum Type {

        /** Scheme */
        SCHEME {
            @Override
            public boolean isAllowed(int c) {
                return this.isAlpha(c) || this.isDigit(c) || '+' == c || '-' == c || '.' == c;
            }
        },
        /** Authority */
        AUTHORITY {
            @Override
            public boolean isAllowed(int c) {
                return this.isUnreserved(c) || this.isSubDelimiter(c) || ':' == c || '@' == c;
            }
        },
        /** User info */
        USER_INFO {
            @Override
            public boolean isAllowed(int c) {
                return this.isUnreserved(c) || this.isSubDelimiter(c) || ':' == c;
            }
        },
        /** Host ipv 4 */
        HOST_IPV4 {
            @Override
            public boolean isAllowed(int c) {
                return this.isUnreserved(c) || this.isSubDelimiter(c);
            }
        },
        /** Host ipv 6 */
        HOST_IPV6 {
            @Override
            public boolean isAllowed(int c) {
                return this.isUnreserved(c) || this.isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
            }
        },
        /** Port */
        PORT {
            @Override
            public boolean isAllowed(int c) {
                return this.isDigit(c);
            }
        },
        /** Path */
        PATH {
            @Override
            public boolean isAllowed(int c) {
                return this.isPchar(c) || '/' == c;
            }
        },
        /** Path segment */
        PATH_SEGMENT {
            @Override
            public boolean isAllowed(int c) {
                return this.isPchar(c);
            }
        },
        /** Query */
        QUERY {
            @Override
            public boolean isAllowed(int c) {
                return this.isPchar(c) || '/' == c || '?' == c;
            }
        },
        /** Query param */
        QUERY_PARAM {
            @Override
            public boolean isAllowed(int c) {
                if ('=' == c || '&' == c) {
                    return false;
                } else {
                    return this.isPchar(c) || '/' == c || '?' == c;
                }
            }
        },
        /** Fragment */
        FRAGMENT {
            @Override
            public boolean isAllowed(int c) {
                return this.isPchar(c) || '/' == c || '?' == c;
            }
        },
        /** Uri */
        URI {
            @Override
            public boolean isAllowed(int c) {
                return this.isUnreserved(c);
            }
        };

        /**
         * Indicates whether the given character is allowed in this URI component.
         *
         * @param c c
         * @return {@code true} if the character is allowed; {@code false} otherwise
         * @since 1.0.0
         */
        public abstract boolean isAllowed(int c);

        /**
         * Indicates whether the given character is in the {@code ALPHA} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isAlpha(int c) {
            return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
        }

        /**
         * Indicates whether the given character is in the {@code DIGIT} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isDigit(int c) {
            return (c >= '0' && c <= '9');
        }

        /**
         * Indicates whether the given character is in the {@code gen-delims} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isGenericDelimiter(int c) {
            return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
        }

        /**
         * Indicates whether the given character is in the {@code sub-delims} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isSubDelimiter(int c) {
            return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
                ',' == c || ';' == c || '=' == c);
        }

        /**
         * Indicates whether the given character is in the {@code reserved} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isReserved(int c) {
            return (this.isGenericDelimiter(c) || this.isSubDelimiter(c));
        }

        /**
         * Indicates whether the given character is in the {@code unreserved} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isUnreserved(int c) {
            return (this.isAlpha(c) || this.isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
        }

        /**
         * Indicates whether the given character is in the {@code pchar} set.
         *
         * @param c c
         * @return the boolean
         * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
         * @since 1.0.0
         */
        protected boolean isPchar(int c) {
            return (this.isUnreserved(c) || this.isSubDelimiter(c) || ':' == c || '@' == c);
        }
    }
}
