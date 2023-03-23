package uk.gov.di.ipv.stub.core.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuer;
import uk.gov.di.ipv.stub.core.config.credentialissuer.CredentialIssuerMapper;
import uk.gov.di.ipv.stub.core.config.uatuser.Identity;
import uk.gov.di.ipv.stub.core.config.uatuser.IdentityMapper;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CoreStubConfig {
    public static final String CORE_STUB_PORT = getConfigValue("CORE_STUB_PORT", "8085");
    public static final String CORE_STUB_CLIENT_ID =
            getConfigValue("CORE_STUB_CLIENT_ID", "ipv-core-stub");
    public static final URI CORE_STUB_REDIRECT_URL =
            URI.create(
                    getConfigValue(
                            "CORE_STUB_REDIRECT_URL",
                            "http://localhost:" + CORE_STUB_PORT + "/callback"));
    public static final int CORE_STUB_MAX_SEARCH_RESULTS =
            Integer.parseInt(getConfigValue("CORE_STUB_MAX_SEARCH_RESULTS", "200"));
    public static UserAuth CORE_STUB_BASIC_AUTH;
    public static final String CORE_STUB_USER_DATA_PATH =
            getConfigValue("CORE_STUB_USER_DATA_PATH", "config/experian-uat-users-large.zip");
    public static final String CORE_STUB_CONFIG_FILE =
            getConfigValue("CORE_STUB_CONFIG_FILE", "/app/config/cris-dev.yaml");
    public static final String CORE_STUB_SIGNING_PRIVATE_KEY_JWK_BASE64 =
            getConfigValue("CORE_STUB_SIGNING_PRIVATE_KEY_JWK_BASE64", null);

    public static final String CORE_STUB_JWT_ISS_CRI_URI =
            getConfigValue(
                    "CORE_STUB_JWT_ISS_CRI_URI",
                    "https://di-ipv-core-stub.london.cloudapps.digital");
    public static final boolean CORE_STUB_CONFIG_AGED_DOB =
            Boolean.parseBoolean(getConfigValue("CORE_STUB_CONFIG_AGED_DOB", "true"));
    public static final String MAX_JAR_TTL_MINS = getConfigValue("MAX_JAR_TTL_MINS", "60");

    public static final boolean CORE_STUB_SHOW_VC =
            Boolean.parseBoolean(getConfigValue("CORE_STUB_SHOW_VC", "true"));

    public static final boolean CORE_STUB_ENABLE_BACKEND_ROUTES =
            Boolean.parseBoolean(getConfigValue("CORE_STUB_ENABLE_BACKEND_ROUTES", "true"));

    public static final boolean ENABLE_BASIC_AUTH =
            Boolean.parseBoolean(getConfigValue("ENABLE_BASIC_AUTH", "false"));

    public static final List<Identity> identities = new ArrayList<>();
    public static final List<CredentialIssuer> credentialIssuers = new ArrayList<>();

    private static final Gson gson = new Gson();

    public static String getConfigValue(String key, String defaultValue) {
        String envValue = Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
        if (StringUtils.isBlank(envValue)) {
            throw new IllegalStateException(
                    "env var '%s' is not set and there is no default value".formatted(key));
        }
        return envValue;
    }

    public static UserAuth getUserAuth() {
        if (CORE_STUB_BASIC_AUTH == null) {
            CORE_STUB_BASIC_AUTH = parseUserAuth();
        }
        return CORE_STUB_BASIC_AUTH;
    }

    public static void initCRIS() throws IOException {
        try (FileInputStream inputStream =
                new FileInputStream(CoreStubConfig.CORE_STUB_CONFIG_FILE)) {
            Map<String, Object> obj = new Yaml().load(inputStream);
            CredentialIssuerMapper mapper = new CredentialIssuerMapper();
            List<Map> cis = (List<Map>) obj.get("credentialIssuerConfigs");
            credentialIssuers.addAll(cis.stream().map(mapper::map).toList());
        }
    }

    public static void initUATUsers() throws IOException {
        Path path = Paths.get(CoreStubConfig.CORE_STUB_USER_DATA_PATH);
        try (InputStream is = Files.newInputStream(path)) {
            readZip(
                    is,
                    (entry, zis) -> {
                        if (entry.getName().endsWith(".json")) {
                            Map map = new Gson().fromJson(new InputStreamReader(zis), Map.class);
                            List people = (List) map.get("people");
                            IdentityMapper identityMapper = new IdentityMapper();
                            int rowNumber = 3; // starting row number in experian uat user sheet
                            for (Object person : people) {
                                Map personMap = (Map) person;
                                identities.add(identityMapper.map(personMap, rowNumber++));
                            }
                        }
                    });
        }
    }

    private static UserAuth parseUserAuth() {
        String user_auth = getConfigValue("CORE_STUB_BASIC_AUTH", null);
        if (user_auth == null) {
            return null;
        }
        Type type = new TypeToken<UserAuth>() {}.getType();
        return gson.fromJson(user_auth, type);
    }

    private static void readZip(InputStream is, BiConsumer<ZipEntry, InputStream> consumer)
            throws IOException {
        try (ZipInputStream zipFile = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zipFile.getNextEntry()) != null) {
                consumer.accept(
                        entry,
                        new FilterInputStream(zipFile) {
                            @Override
                            public void close() throws IOException {
                                zipFile.closeEntry();
                            }
                        });
            }
        }
    }
}
