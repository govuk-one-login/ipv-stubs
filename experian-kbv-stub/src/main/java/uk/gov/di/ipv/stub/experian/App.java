package uk.gov.di.ipv.stub.experian;

import jakarta.xml.bind.JAXBException;
import spark.Spark;

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
        Spark.post("/iiq", "application/soap+xml", handler.iiqWebService);
    }
}
