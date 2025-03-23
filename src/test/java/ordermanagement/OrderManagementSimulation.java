package ordermanagement;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.tsv;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class OrderManagementSimulation extends Simulation {

    HttpProtocolBuilder httpProtocolBuilder = http.baseUrl("http://localhost:9503");

    ScenarioBuilder scenario1 = scenario("Get Orders").exec(http("get orders").get("/order"));
    ScenarioBuilder scenario2 = scenario("Save Orders")
            .feed(tsv(("orders-payload.tsv")).circular())
            .exec(
                    http("save orders")
                            .post("/order")
                            .body(StringBody("#{payload}"))
                            .header("Content-Type", "application/json")
                            .check(status().in(201, 422, 400))
                            .toChainBuilder().pause(Duration.ofMillis(1), Duration.ofMillis(30))
            );

    {
        setUp(
                scenario1.injectOpen(rampUsersPerSec(10).to(500)
                                .during(Duration.ofSeconds(3))),
                scenario2.injectOpen(
                        constantUsersPerSec(2).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(5).during(Duration.ofSeconds(15)).randomized(),
                        rampUsersPerSec(6).to(100).during(Duration.ofMinutes(3))
                )
        ).protocols(httpProtocolBuilder);
    }
}
