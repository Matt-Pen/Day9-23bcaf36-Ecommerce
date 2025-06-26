package in.edu.kristujayanti.handlers;

import in.edu.kristujayanti.services.SampleService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class SampleHandler extends AbstractVerticle {

    public void start(Promise<Void> startPromise){
        HttpServer server = vertx.createHttpServer();
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        SampleService smp= new SampleService();


        router.post("/signuser").handler(smp::usersign);
        router.post("/loguser").handler(smp::userlog);

        router.post("/signadmin").handler(smp::adminsign);
        router.post("/logadmin").handler(smp::adminlog);

        router.post("/order").handler(smp::placeorder);
        router.patch("/order").handler(smp::updateorder);
        router.get("/order").handler(smp::getorder);

        router.get("/order/product").handler(smp::getnamesales);
        router.get("/order/date").handler(smp::getsaledate);



        Future<HttpServer> fut=server.requestHandler(router).listen(8080);
        if(fut.succeeded()){
            System.out.println("Server running at http://localhost:8080");
        }
        else{
            System.out.println("server failed to run.");
        }

    }
    @Override
    public void stop(Promise<Void> stopPromise) {
        System.out.println("Server stopping...");
        stopPromise.complete();
    }

    //Handler Logic And Initialize the Service Here
}
