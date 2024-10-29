package uk.gov.di.ipv.stub.cred.fixtures;

import uk.gov.di.ipv.stub.cred.config.ClientConfig;
import uk.gov.di.ipv.stub.cred.config.JwtAuthenticationConfig;

import java.util.List;
import java.util.Map;

public final class TestFixtures {
    private TestFixtures() {}

    public static final String EC_PRIVATE_KEY_1 =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    public static final String EC_PUBLIC_KEY_1 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEE9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIMqVMxm2EdlSRjPkCV5NDyN9/RMmJLerY4H0vkXDjEDTg==";
    public static final String EC_PUBLIC_JWK_1 =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";

    public static final String EC_PRIVATE_KEY_2 =
            "MHcCAQEEIAtHgvsZntJucYFY+d29FWLf/vieBEiAKVi24MW5QR8LoAoGCCqGSM49AwEHoUQDQgAE6loQMLFWFzZJROH1zAOGZdqwFD+pqP0s4JDYc4tgC6nUOidqk1KiUPu2vbH5Mv8E8J9GoaNWULBBoscGC2zAyQ==";
    public static final String EC_PUBLIC_KEY_2 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE6loQMLFWFzZJROH1zAOGZdqwFD+pqP0s4JDYc4tgC6nUOidqk1KiUPu2vbH5Mv8E8J9GoaNWULBBoscGC2zAyQ==";
    public static final String EC_PUBLIC_JWK_2 =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"6loQMLFWFzZJROH1zAOGZdqwFD-pqP0s4JDYc4tgC6k\",\"y\":\"1DonapNSolD7tr2x-TL_BPCfRqGjVlCwQaLHBgtswMk\"}";

    public static final String EC_PRIVATE_KEY_3 =
            "MHcCAQEEIJe+RboRypHQaJBhORf6+3TsSZt3IPOFolstp/HxWSf9oAoGCCqGSM49AwEHoUQDQgAEXqrUBZchnNiPxCA4Gwb7EikNGkoOwubgEqC5ZCfOSNJtGdBgP7On//rk4+a9YIIjL5Ep8dOqZOTbUmVjfvo3dA==";
    public static final String EC_PUBLIC_KEY_3 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEXqrUBZchnNiPxCA4Gwb7EikNGkoOwubgEqC5ZCfOSNJtGdBgP7On//rk4+a9YIIjL5Ep8dOqZOTbUmVjfvo3dA==";
    public static final String EC_PUBLIC_JWK_3 =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"XqrUBZchnNiPxCA4Gwb7EikNGkoOwubgEqC5ZCfOSNI\",\"y\":\"bRnQYD-zp__65OPmvWCCIy-RKfHTqmTk21JlY376N3Q\"}";

    public static final String RSA_PRIVATE_KEY_JWK =
            "{\"p\":\"6NcQCvRO3l9rf-1kNkU7hcZS4fSI2ixx_rsWyiYv-PTMb7P4uA5YdPGmMHUzEceLwO4i_0kKpOmBj7cSs4tvSv9YgKvyaqN-rxxngsg1399RSQsmaea0uJ_msAFyqtvhh5IY0WsLqSMEB_rK8d3pprISTpuKf6j3QjCgLEXq_4dr-1rpAPIciLj5s674xQXRI7v3AzT9KFYnaRsu4E7tC1l8gRkdppVRzPI1eF_sH_9aJ2p9UKRvzGnK6tdNVkHOQ3VsOheG29j02DV9Co3gZrqRKs99WGejB_EJwHem6yhO5RHupms-im-q_m7TdPkqxKHTGAny_rPKofxN-_IWEQ\",\"kty\":\"RSA\",\"q\":\"03zn4UZAworX81pvrz8QO-ZJQpnyEfh4LleycKD0-1-MVDQNyHyHZlws30tuR7Cn9FRxsHlH9ONEAXErQGNMSMgsLRR1AC49LtnqlxjBIZOMWDEV1QChMisZRxyI4A_EyFxJlt3f-SHbESo-EcQYqQup-9GdaWqMIW-WpAAL-m02bXIkk7pHHa2eu1Q6zRihBgPzBiUKPjCpLJgcbbN_UjyQXebri_Adwe4c2Nrxl6j703gjbjs7-lB9VcD3oMeinWGJEK3tkBB1WV-OSMJiXkKJqqz2DHh4DrWhIPAVZmMirHHfnHlIRCEZVIlaWGOJZIYhkeo1iYVmSTgORBddyw\",\"d\":\"vX_Gy76jQSRi3qFvYfT9STgybCne8RAt2TarqLg2jzeAaPY3zwWlsf4rgVoanQrXu3a_8yXddy4AJ31LzckoRrrQzLiellVm6luvAGrL4wWG72wThXlXf1vSovJv8-zdUVjY_dYb8WdDgrE6TB2sVLXsLxSXKpXZVUTDI1IkE6h2g-ZEY8FbWQb_zVPIEnaNeOCjvLl_mZR8Xqog1-b-4BWKbw7UpEJvlyAcdn9eBlkK3lBFNPzcglR9ZYgpWaW0RR5aFMJ4F5b5d4molkoixaDqc2H2Oe-th_Y-_umqz5DyMS6D9E3LYIigSvba-Z6aE6DsMKfCQZowkMjup7z9UBecmbd79IYTufh7KrPsh8NjBcMjXHjFMiCUGw99HC9xgbucKAAmwY_Ml7q9-ryRO-T49LyZ6Bj5ag8HY4pRIgJZsw0L_mLywn2tmAZyWynABQaD7g4Uqkl6RAAQUV-AqKMzIHSVKh1HvTrRPKlGMfWGLfCLuQJ_MazggxkjCF3bI_fZSD39Ibfhu-ZtMp1Y1CfbipQiDCR20WLz0Ex7DRq9vtldZ-b6_nhibY4IydzfjNdEh5InPeabPGJ-seggJ9pE3ONhHehZroC6AKIj6FEf_nM5iXRyj-ov09x-wCnj354ZJcm37IcebzFvSA7raPyGwR2pOrqANc6_IjPcAIE\",\"e\":\"AQAB\",\"qi\":\"XHqTK6bMa4B_8cqhXYC0l1mjkm4HkBh8eLQPtpw1tbNP_HARLWqiXkyvoqqAX_gNkWNRsv2tJQ-wwuyZ_oItxE5ptacIxxvBaPsRbdTr1vtRykC9bjadmn7nmG1DMQAXzrdEPoWn88pzF6_4YkBvY3ngCJfe2TjbmrTgaQMgkVYXvqayHSejWlJNa5E-3Og0xH-CDIKWWyAe0KxEE40iI77qBkYMvVwCNhdeNTsuaXy-A5YxblZp4TbXMY_xy3fBzVq2xTmilnAVNxyQ_oqMMv6gbWK7GCsXLmAK5wDZX-vAxAmOmRmEAqlxe5GAkedKAGw_IuLc32_hoDRltNZfFA\",\"dp\":\"wU3zErUbWUCs1cs3PFsj_H7XRqImj8MAbPPUCsXDZBOQOliW7-9w_r20NFzIpkUdQHIz-e8g-CKoHrFlxEvJfOEbD9Aw9NmBjk2tngUrvQ4AxPyNyrPva6vM8GhzU2gzB8OB-TK-vo_Eg_9xR3XtyifiTQKS7ENR69DE2Zy-aaB7RHWIJfHbQKMZI1TrUV7v75PYkgAHANrt4zPfKfg8kgSb-e3pEOi8vcKEI8i3FyV_KmQdX7r02icmgOt4WFlPre-ph10K6DBprapSglWhbIgNhxY1wRRhZHF3oCN2H5saTNEjaWR1yqbEtnE5-s319MNIppdz9oM7gloeQEIOkQ\",\"dq\":\"uKvqIzlgVUA-P_6pZaKwv01gjWq2CWEpOHZVl6nFIjeV5vUpT_cFmKlGeZl5a9pjXqPaPpo47isBeCzk8q2CsE8y3A5v-D9oJ6AcC-KOyo330A7UnJGXMKKXyROupdC_KaIElFucNwSMMVnsp0DPs9U-kmjAhouGX6_8H6r2yq9RBpLUQ7c2YED6SWPMkMk_2mvaa3QulI2TPCB7OoOx2xKNkaGR7zk2EuCkievtaFwjwc23Soso3XQpbZc55EhOxBSmRk1KEzF79xXMvdYXZW2-nq23kL4lP9r0HznlxektHt20wALbyroIT1w86s_H6mKBr9OO-k3lOmxbcLPirw\",\"n\":\"wFrdS8EQCKiD15ruRA7HWyOGhyVtNZaWrX9EFecIe6OAbBDxGKcPBKmRU03nwx5LziEhM_ce5l4kySk72lX0a-ze5dojjfLztRYpgbI9DaEp3_FLGrZJFjOd-piOIgYABk4a5MvPn9YVxJs6XviQNe8IeSzclLGWMWGW8TENpZ1bpFCqkabES7G_uEM0kdhhgaZgUxi-RYQHPqhm6MOdgRqbiy21P0NHTEVKrikYjvXewSBqmgLUPQi850Ojs1wPdYTThj5BObYwz9hJVmbHTHoPh0H4Fdja1opcS5etoHkNYOy37So8CksV6s6ur7zUqa9FTLMrMVva7joDtsWbWJ8l3jay_OHEwRR9DSoLuabZi-kVzFFSvxdCMNvW2D2cRw3GYmG0i8qs11tljQLLEtKa2YrAdDREyEPYJGSXJ2CQxjldi36-iGb-s6A1YSB74qbWdmW1ZKjpaOfkfrTAgqhqG9QDkwhOJNBnUCQ0ieZFauw1FI3NKG5XFR37JGND_YnLlBKX3W3LHeHcXSaJac618qGo81TWnW061S3LdUEg2XbtIrO--4Ge49ImtRMAkrhTR031w7d5gUrmelBq3siPfRadbbv9C5TCG8n3T35VJK4W2lGnIyYAssOpalr7T9NenO51qBf-h2N5UZ-ST5tNF03k9zzJtfND6DqCrHs\"}"; // pragma: allowlist secret

    public static final String RSA_PUBLIC_KEY_JWK =
            "{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"wFrdS8EQCKiD15ruRA7HWyOGhyVtNZaWrX9EFecIe6OAbBDxGKcPBKmRU03nwx5LziEhM_ce5l4kySk72lX0a-ze5dojjfLztRYpgbI9DaEp3_FLGrZJFjOd-piOIgYABk4a5MvPn9YVxJs6XviQNe8IeSzclLGWMWGW8TENpZ1bpFCqkabES7G_uEM0kdhhgaZgUxi-RYQHPqhm6MOdgRqbiy21P0NHTEVKrikYjvXewSBqmgLUPQi850Ojs1wPdYTThj5BObYwz9hJVmbHTHoPh0H4Fdja1opcS5etoHkNYOy37So8CksV6s6ur7zUqa9FTLMrMVva7joDtsWbWJ8l3jay_OHEwRR9DSoLuabZi-kVzFFSvxdCMNvW2D2cRw3GYmG0i8qs11tljQLLEtKa2YrAdDREyEPYJGSXJ2CQxjldi36-iGb-s6A1YSB74qbWdmW1ZKjpaOfkfrTAgqhqG9QDkwhOJNBnUCQ0ieZFauw1FI3NKG5XFR37JGND_YnLlBKX3W3LHeHcXSaJac618qGo81TWnW061S3LdUEg2XbtIrO--4Ge49ImtRMAkrhTR031w7d5gUrmelBq3siPfRadbbv9C5TCG8n3T35VJK4W2lGnIyYAssOpalr7T9NenO51qBf-h2N5UZ-ST5tNF03k9zzJtfND6DqCrHs\"}"; // pragma: allowlist secret

    public static final String DCMAW_VC =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ1cm46dXVpZDo1ZDZkNjgzMy04NTEyLTRlMzctYjVlYS1iZTdkZTc3OTQ4ZGQiLCJhdWQiOiJodHRwczpcL1wvaWRlbnRpdHkuYnVpbGQuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2OTgxNDU2NjksImlzcyI6Imh0dHBzOlwvXC9kY21hdy1jcmkuc3R1YnMuYWNjb3VudC5nb3YudWsiLCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiSWRlbnRpdHlDaGVja0NyZWRlbnRpYWwiXSwiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ0eXBlIjoiR2l2ZW5OYW1lIiwidmFsdWUiOiJLZW5uZXRoIn0seyJ0eXBlIjoiRmFtaWx5TmFtZSIsInZhbHVlIjoiRGVjZXJxdWVpcmEifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjUtMDctMDgifV0sInBhc3Nwb3J0IjpbeyJleHBpcnlEYXRlIjoiMjAzMC0wMS0wMSIsImRvY3VtZW50TnVtYmVyIjoiMzIxNjU0OTg3In1dfSwiZXZpZGVuY2UiOlt7ImFjdGl2aXR5SGlzdG9yeVNjb3JlIjoxLCJjaGVja0RldGFpbHMiOlt7ImNoZWNrTWV0aG9kIjoidnJpIn0seyJiaW9tZXRyaWNWZXJpZmljYXRpb25Qcm9jZXNzTGV2ZWwiOjMsImNoZWNrTWV0aG9kIjoiYnZyIn1dLCJ2YWxpZGl0eVNjb3JlIjoyLCJzdHJlbmd0aFNjb3JlIjozLCJ0eXBlIjoiSWRlbnRpdHlDaGVjayIsInR4biI6ImMxZjZmMzIwLTAxZWYtNDRhNy1hZTA1LTIyNGU1MzgwZDUwMCJ9XX0sImp0aSI6InVybjp1dWlkOmU5MzdlODEyLWRhZmUtNDJlNC1hMDk0LTZjOWQ0MWZlZTUwYyJ9.3d6uaGQHb5zBkSMvgZ7S58_Y_lk_hSRk7ng5WNRf7O437IJKckvSYP_HNX6rCGOhb4QuW2ROCcfiCxyRO73pOg";

    public static final Map<String, ClientConfig> CLIENT_CONFIG =
            Map.of(
                    "clientIdValid",
                    ClientConfig.builder()
                            .signingPublicJwk(
                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                            .audienceForVcJwt("https://example.com/audience")
                            .jwtAuthentication(
                                    JwtAuthenticationConfig.builder()
                                            .signingPublicJwk(
                                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                                            .validRedirectUrls(List.of("https://valid.example.com"))
                                            .authenticationMethod("jwt")
                                            .build())
                            .build(),
                    "clientIdValidMultipleUri",
                    ClientConfig.builder()
                            .signingPublicJwk(
                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                            .audienceForVcJwt("https://example.com/audience")
                            .jwtAuthentication(
                                    JwtAuthenticationConfig.builder()
                                            .signingPublicJwk(
                                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                                            .validRedirectUrls(
                                                    List.of(
                                                            "https://valid1.example.com",
                                                            "https://valid2.example.com",
                                                            "https://valid3.example.com"))
                                            .authenticationMethod("jwt")
                                            .build())
                            .build(),
                    "clientIdNonRegistered",
                    ClientConfig.builder()
                            .signingPublicJwk(
                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                            .audienceForVcJwt("https://example.com/audience")
                            .jwtAuthentication(
                                    JwtAuthenticationConfig.builder()
                                            .signingPublicJwk(
                                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                                            .validRedirectUrls(
                                                    List.of("https://invalid.example.com"))
                                            .authenticationMethod("jwt")
                                            .build())
                            .build(),
                    "noAuthenticationClient",
                    ClientConfig.builder()
                            .signingPublicJwk(
                                    "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}")
                            .audienceForVcJwt("https://example.com/audience")
                            .jwtAuthentication(
                                    JwtAuthenticationConfig.builder()
                                            .authenticationMethod("none")
                                            .build())
                            .build());
}
