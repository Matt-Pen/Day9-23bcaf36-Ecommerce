package in.edu.kristujayanti.services;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


public class SampleService {
    Vertx vertx = Vertx.vertx();
    HttpServer server = vertx.createHttpServer();
    String connectionString = "mongodb://admin:admin@172.21.17.53:27017,172.21.17.54:27017,172.21.17.92:27017/";
    MongoClient mongoClient = MongoClients.create(connectionString);
    MongoDatabase database = mongoClient.getDatabase("Ordertrack");
    MongoCollection<Document> userlog = database.getCollection("User");
    MongoCollection<Document> adminlog = database.getCollection("Admin");
    MongoCollection<Document> orders = database.getCollection("Orders");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public void usersign(RoutingContext ctx) {
        JsonObject signin = ctx.getBodyAsJson();
        String user = signin.getString("user");
        String pwd = signin.getString("pass");
        String hashpass=hashit(pwd);

        Document doc=new Document("user",user).append("pass",hashpass);
        InsertOneResult ins=userlog.insertOne(doc);

        if(ins.wasAcknowledged()) {
            ctx.response().end("Signed in successfully.");

        }
    }
    public void adminsign(RoutingContext ctx) {
        JsonObject signin = ctx.getBodyAsJson();
        String user = signin.getString("user");
        String pwd = signin.getString("pass");
        String hashpass=hashit(pwd);

        Document doc=new Document("user",user).append("pass",hashpass);
        InsertOneResult ins=adminlog.insertOne(doc);

        if(ins.wasAcknowledged()) {
            ctx.response().end("Signed in successfully.");

        }
    }

    public void adminlog(RoutingContext ctx){
        JsonObject login=ctx.getBodyAsJson();
        String user = login.getString("user");
        String pwd = login.getString("pass");
        System.out.println(user+" "+pwd);
        String hashlog=hashit(pwd);
        String status="";

        for(Document doc:adminlog.find()){
            String dbuser=doc.getString("user");
            String dbpass=doc.getString("pass");
            System.out.println(dbuser +" "+dbpass);

            if(dbuser.equals(user)){
                System.out.println("Heloo ");
                if(dbpass.equals(hashlog)){
                    System.out.printf("Login was successful");
                    status="Login was successfull";
                }
                else{
                    status="Password is Incorrect";
                }
            }
            else{
                System.out.printf("Login was UNsuccessful");
                status="Invalid Login Credentials";
            }

        }
        ctx.response().end(status);

    }

    public void userlog(RoutingContext ctx){
        JsonObject login=ctx.getBodyAsJson();
        String user = login.getString("user");
        String pwd = login.getString("pass");

        String hashlog=hashit(pwd);
        String status="";

        for(Document doc:userlog.find()){
            String dbuser=doc.getString("user");
            String dbpass=doc.getString("pass");

            if(dbuser.equals(user)){
                if(dbpass.equals(hashlog)){
                    status="Login was successfull";
                }
                else{
                    status="Password is Incorrect";
                }
            }
            else{
                status="Invalid Login Credentials";
            }
        }
        ctx.response().end(status);
    }

    public void placeorder(RoutingContext ctx){
        JsonObject ord=ctx.getBodyAsJson();
        String user = ord.getString("user");
        String prod = ord.getString("product");
        int quant= ord.getInteger("quantity");
        double price=ord.getDouble("price");
        String ordate=ord.getString("date");
        String status=ord.getString("status");

        String prostat="";

        if (!ord.containsKey("user")
                || !ord.containsKey("product")
                || !ord.containsKey("quantity")
                || !ord.containsKey("price")
                || !ord.containsKey("date")
                || !ord.containsKey("status")) {
            prostat="Inserted document not valid";
        }
        else{
            Document ordDoc = orders.find().sort(Sorts.descending("order id")).limit(1).first();
            int ordid=ordDoc.getInteger("order id");
            ordid=ordid+1;
            Document doc= new Document("order id",ordid).append("user",user)
                    .append("product",prod).append("quantity",quant)
                    .append("price",price).append("date",ordate)
                    .append("status",status);
            InsertOneResult ins=orders.insertOne(doc);
            if(ins.wasAcknowledged()){
                prostat="Order Successfully Placed with orderid: "+ ordid;
            }
            else{
                prostat="Order failed to be Placed";
            }
            ctx.response().end(prostat);

        }

    }

    public void updateorder(RoutingContext ctx) {
        JsonObject upd = ctx.getBodyAsJson();
        int id = upd.getInteger("order id");
        String status = upd.getString("status");
        String prostat = "";

        if (id != 0 || status != null) {
            Bson filter2 = Filters.eq("order id", id);
            Bson update2 = Updates.set("status", status);
            UpdateResult result2 = orders.updateOne(filter2, update2);
            if (result2.getModifiedCount() != 0) {
                prostat = "Updated Succesfully";
            } else {
                prostat = "Update failed. Try Again!!";
            }
        }
        ctx.response().end(prostat);
    }


    public void getorder(RoutingContext ctx){
        JsonArray jarr = new JsonArray();
        String name = ctx.request().getParam("user");
        Bson filter=Filters.regex("user",name);
        for (Document doc : orders.find().filter(filter)) {
            jarr.add(new JsonObject(doc.toJson()));

        }
        if (jarr.isEmpty()) {
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content", "application/json")
                    .end("Doc returned as NULL.");
        } else {
            ctx.response().putHeader("Content", "application/json").end(jarr.encodePrettily());

        }
    }
    public void getnamesales(RoutingContext ctx){
        String name = ctx.request().getParam("product");
        double sum=0;
        Bson filter=Filters.regex("product",name);
        ctx.response().setChunked(true);
        for(Document doc: orders.find().filter(filter))
        {
            double pri=doc.getDouble("price");
            int quant=doc.getInteger("quantity");
            pri=pri*quant;
            sum=sum+pri;
        }

        ctx.response().write("The total sales for the product: "+ name+ " is "+sum+"\n");
        JsonArray jarr = new JsonArray();
        Bson filter2=Filters.regex("product",name);
        for (Document doc : orders.find().filter(filter)) {
            jarr.add(new JsonObject(doc.toJson()));

        }
        ctx.response().end(jarr.encodePrettily());
    }

    public void getsaledate(RoutingContext ctx){
        try {
            String date1 = ctx.request().getParam("from date");
            String date2 = ctx.request().getParam("to date");
            JsonArray jarr = new JsonArray();
            double sum = 0;
            ctx.response().setChunked(true);
            LocalDate fdate = LocalDate.parse(date1, formatter);
            LocalDate tdate = LocalDate.parse(date2, formatter);
            Bson projection = Projections.fields(Projections.exclude("_id"));
            for (Document doc : orders.find().projection(projection)) {
//                        System.out.println(doc.toJson());
                String txt3 = doc.getString("date");

                LocalDate date3 = LocalDate.parse(txt3, formatter);

                if (date3.isAfter(fdate) && date3.isBefore(tdate)) {
                    double pri = doc.getDouble("price");
                    int quant = doc.getInteger("quantity");
                    pri = pri * quant;
                    sum = sum + pri;

                    jarr.add(new JsonObject(doc.toJson()));
                }

            }
            ctx.response().write("The total sales for all products sold in between " + date1 + " and " + date2 + " is " + sum + "\n");
            ctx.response().end(jarr.encodePrettily());
        } catch (Exception e){
            System.out.println(e);

        }

    }



        public String hashit (String pass){

            try {
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                byte[] hashed = md.digest(pass.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashed)
                    sb.append(String.format("%02x", b));
                return sb.toString();

            } catch (Exception e) {
                throw new RuntimeException("Hashing Failed");
            }
//        SecureRandom random = new SecureRandom();
//        byte[] salt = new byte[16];
//        random.nextBytes(salt);
//
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        md.update(salt);
//
//        byte[] hashedPassword = md.digest(pass.getBytes());
//        return Base64.getEncoder().encodeToString(hashedPassword);
//
//        return pass;
        }

    }


        //Your Logic Goes Here

