package uk.gov.di.ipv.stub.cred.fixtures;

public final class TestFixtures {
    private TestFixtures() {}

    public static final String CLIENT_CONFIG =
            "ewogICJjbGllbnRJZFZhbGlkIjogewogICAgInNpZ25pbmdQdWJsaWNKd2siOiAie1wia3R5XCI6XCJFQ1wiLFwiY3J2XCI6XCJQLTI1NlwiLFwieFwiOlwiRTlaenVPb3FjVlU0cFZCOXJwbVR6ZXpqeU9QUmxPbVBHSkhLaThSU2xJTVwiLFwieVwiOlwiS2xUTVp0aEhaVWtZejVBbGVUUThqZmYwVEppUzNxMk9COUw1Rnc0eEEwNFwifSIsCiAgICAiand0QXV0aGVudGljYXRpb24iOiB7CiAgICAgICJzaWduaW5nUHVibGljSndrIjogIlRPVEFMTFktQS1KV0siLAogICAgICAidmFsaWRSZWRpcmVjdFVybHMiOiAiaHR0cHM6Ly92YWxpZC5leGFtcGxlLmNvbSIsCiAgICAgICJhdXRoZW50aWNhdGlvbk1ldGhvZCI6ICJqd3QiCiAgICB9CiAgfSwKICAiY2xpZW50SWRWYWxpZE11bHRpcGxlVXJpIjogewogICAgInNpZ25pbmdQdWJsaWNKd2siOiAiVE9UQUxMWS1BLUpXSyIsCiAgICAiand0QXV0aGVudGljYXRpb24iOiB7CiAgICAgICJzaWduaW5nUHVibGljSndrIjogIlRPVEFMTFktQS1KV0siLAogICAgICAidmFsaWRSZWRpcmVjdFVybHMiOiAiaHR0cHM6Ly92YWxpZDEuZXhhbXBsZS5jb20saHR0cHM6Ly92YWxpZDIuZXhhbXBsZS5jb20saHR0cHM6Ly92YWxpZDMuZXhhbXBsZS5jb20iLAogICAgICAiYXV0aGVudGljYXRpb25NZXRob2QiOiAiand0IgogICAgfQogIH0sCiAgImNsaWVudElkTm9uUmVnaXN0ZXJlZCI6IHsKICAgICJzaWduaW5nUHVibGljSndrIjogIlRPVEFMTFktQS1KV0siLAogICAgImp3dEF1dGhlbnRpY2F0aW9uIjogewogICAgICAic2lnbmluZ1B1YmxpY0p3ayI6ICJUT1RBTExZLUEtSldLIiwKICAgICAgInZhbGlkUmVkaXJlY3RVcmxzIjogImh0dHBzOi8vaW52YWxpZC5leGFtcGxlLmNvbSIsCiAgICAgICJhdXRoZW50aWNhdGlvbk1ldGhvZCI6ICJqd3QiCiAgICB9CiAgfQp9";
    public static final String NO_AUTHENTICATION_CLIENT_CONFIG =
            "ewogICJub0F1dGhlbnRpY2F0aW9uQ2xpZW50IjogewogICAgInNpZ25pbmdDZXJ0IjogIkEtQ0VSVElGSUNBVEUiLAogICAgImp3dEF1dGhlbnRpY2F0aW9uIjogewogICAgICAiYXV0aGVudGljYXRpb25NZXRob2QiOiAibm9uZSIKICAgIH0KICB9Cn0=";

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

    public static final String CLIENT_CONFIG_WITH_PUBLIC_JWK =
            "ewogICJhVGVzdENsaWVudCI6IHsKICAgICJzaWduaW5nQ2VydCI6ICJUT1RBTExZLUEtQ0VSVElGSUNBVEUiLAogICAgImp3dEF1dGhlbnRpY2F0aW9uIjogewogICAgICAic2lnbmluZ1B1YmxpY0p3ayI6ICJ7XCJrdHlcIjpcIkVDXCIsXCJjcnZcIjpcIlAtMjU2XCIsXCJ4XCI6XCJFOVp6dU9vcWNWVTRwVkI5cnBtVHplemp5T1BSbE9tUEdKSEtpOFJTbElNXCIsXCJ5XCI6XCJLbFRNWnRoSFpVa1l6NUFsZVRROGpmZjBUSmlTM3EyT0I5TDVGdzR4QTA0XCJ9IiwKICAgICAgInZhbGlkUmVkaXJlY3RVcmxzIjogImh0dHBzOi8vdGVzdC1jbGllbnQuZXhhbXBsZS5jb20vY2FsbGJhY2siLAogICAgICAiYXV0aGVudGljYXRpb25NZXRob2QiOiAiand0IgogICAgfQogIH0KfQ==";
}
