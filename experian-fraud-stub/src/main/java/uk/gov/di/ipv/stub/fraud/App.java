package uk.gov.di.ipv.stub.fraud;

import spark.Spark;

public class App {

    public static void main(String[] args) {
        new App();
    }

    public App() {
        Spark.port(Integer.parseInt(Config.PORT));

        Handler handler = new Handler();

        Spark.get("/", handler.root);
        Spark.get("/fraud-request", handler.root);

        Spark.post("/DefaultRequestListener", handler.fraudCheck);

        Spark.put("/fraud-response", handler.addFraudResponse);
        Spark.delete("/fraud-response", handler.deleteFraudResponse);
        Spark.get("/fraud-response", handler.getFraudResponse);
    }
}
