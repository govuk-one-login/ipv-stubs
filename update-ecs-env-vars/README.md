### Adding a new stub
When you add a new stub it also needs adding to an environment variable
for the lambda in the template.yaml file which looks like this:
```
PREFIXES: |
            [{ "clusterNamePrefix" : "core-passport-stub-CoreStubCluster-" ,
              "ssmParamPrefix" : "/stubs/core/passport/env/" } ,
              { "clusterNamePrefix" : "cri-fraud-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/fraud/env/" } ,
              { "clusterName" : "cri-err-test-1-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/error-testing-1/env/" } ,
              { "clusterNamePrefix" : "orch-stub-OrchStubCluster-" ,
                "ssmParamPrefix" : "/stubs/build/orch/env/" } ,
              { "clusterNamePrefix" : "cri-experian-kbv-stub-CriStubCluster-" ,
                  "ssmParamPrefix" : "/stubs/credential-issuer/experianKbv/env/" } ,
              { "clusterNamePrefix" : "cri-err-test-2-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/error-testing-2/env/" } ,
              { "clusterNamePrefix" : "cri-passport-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/passport/env/" } ,
              { "clusterNamePrefix" : "core-cri-stub-CoreStubCluster-" ,
                "ssmParamPrefix" : "/stubs/core/cri/env/" } ,
              { "clusterNamePrefix" : "cri-dcmaw-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/dcmaw/env/" } ,
              { "clusterNamePrefix" : "cri-act-history-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/activity-history/env/" } ,
              { "clusterNamePrefix" : "cri-address-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/address/env/" } ,
              { "clusterNamePrefix" : "cri-drv-license-stub-CriStubCluster-" ,
                "ssmParamPrefix" : "/stubs/credential-issuer/driving-licence/env/" } ,
              { "clusterNamePrefix" : "cri-f2f-stub-CriStubCluster-",
                "ssmParamPrefix" : "/stubs/credential-issuer/f2f/env/"  } ,
              { "clusterNamePrefix" : "core-front-dev-tobys-CoreFrontCluster-" ,
                "ssmParamPrefix" : "/stubs/tobytesting/thistest/" } ]
```
(this is because there is no other way to match the ssm parameter prefix with the intended cluster)