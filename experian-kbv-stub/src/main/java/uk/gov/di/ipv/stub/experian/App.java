package uk.gov.di.ipv.stub.experian;

import spark.Spark;

import javax.xml.bind.JAXBException;

public class App {

    public static void main(String[] args) throws JAXBException {
        new App();
    }

    public App() throws JAXBException {
        Spark.port(Integer.parseInt(Config.PORT));

        Handler handler = new Handler();

        Spark.get("/", handler.root);
        Spark.get("/health", handler.root);
        Spark.post("/wasp-token", handler.tokenRequest);
        Spark.post("/saa", "application/soap+xml", handler.startAuthenticationAttempt);
        Spark.post("/rtq", "application/soap+xml", handler.responseToQuestions);
    }
}
