package if4031.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    public static String username = "";
    public static String password = "";
    
    public static void main(String[] args) {
        // Cluster and Session declaration
        Cluster cluster;
        Session session;
        
        // Connect to Sister Cluster and twitter_aji keyspace
        cluster = Cluster.builder().addContactPoint("167.205.35.20").build();
        session = cluster.connect("twitter_aji");
        
        // Input variables
        Scanner in = new Scanner(System.in);
        String input = "";
        String[] command;
        
        // Welcome message
        System.out.println("Welcome to Sister Twitter Beta! Please read the instructions below.");
        System.out.println("-------------------------------------------------------------------");
        System.out.println("1. Type /register <nickname> <password> to register.");
        System.out.println("2. Type /follow <nickname> to follow a friend.");
        System.out.println("3. Type /tweet <tweet> to tweet.");
        System.out.println("4. Type /showtweets to show own tweets.");
        System.out.println("5. Type /timeline to show own timeline.");
        System.out.println("-------------------------------------------------------------------");
        do {
            System.out.print("Please enter your command: ");
            input = in.nextLine();
            command = input.split(" ");
            switch(command[0]) {
                case "/register":
                    Register(session, command[1], command[2]);
                    break;
                case "/follow":
                    Follow(session, command[1], Client.username);
                    break;
                case "/tweet":
                    Tweet(session, Client.username, command[1]);
                    break;
                case "/showtweets":
                    ShowTweet(session, Client.username);
                    break;
                case "/timeline":
                    ShowTimeline(session, Client.username);
                    break;
                default: 
                    System.out.println("Invalid command.");
                    break;
            }
        } while (command[0].compareTo("exit") != 0);

        // Clean up the connection by closing it
        cluster.close();
    }
    
    public static void Register(Session session, String username, String password) {
        Client.username = username;
        Client.password = password;
        session.execute("INSERT INTO users (username, password) VALUES ('"+ username + "', '"+ password +"')");
    }
    
    public static void Follow(Session session, String username, String follower) {
        UUID timestamp = UUIDs.timeBased();
        session.execute("INSERT INTO friends (username, friend, since) VALUES ('"+ follower + "', '"+ username +"', dateof("+ timestamp +"))");
        session.execute("INSERT INTO followers (username, follower, since) VALUES ('"+ username + "', '"+ follower +"', dateof("+ timestamp +"))");
    }
    
    public static void Tweet(Session session, String username, String body) {
        UUID tweet_uuid = UUIDs.random();
        UUID time_uuid = UUIDs.timeBased();
        session.execute("INSERT INTO tweets (tweet_id, username, body) VALUES ("+ tweet_uuid +", '"+ username + "', '"+ body +"')");
        session.execute("INSERT INTO userline (username, time, tweet_id) VALUES ('"+ username + "', "+ time_uuid +", "+ tweet_uuid +")");
        session.execute("INSERT INTO timeline (username, time, tweet_id) VALUES ('"+ username + "', "+ time_uuid +", "+ tweet_uuid +")");
        ResultSet results = session.execute("SELECT * FROM followers WHERE username ='"+ username +"'");
        for (Row row : results) {
            session.execute("INSERT INTO timeline (username, time, tweet_id) VALUES ('"+ row.getString("follower") + "', "+ time_uuid +", "+ tweet_uuid +")");
        }
    }
    
    public static void ShowTweet(Session session, String username) {
        ResultSet results = session.execute("SELECT * FROM userline WHERE username='"+ username +"'");
        int i = 1;
        for (Row row : results) {
            ResultSet results2 = session.execute("SELECT * FROM tweets WHERE tweet_id="+ row.getUUID("tweet_id") +"");
            for (Row row2 : results2) {
                System.out.format("%d: %s\n", i, row2.getString("body"));
            }
            i = i + 1;
        }
    }
    
    public static void ShowTimeline(Session session, String username) {
        ResultSet results = session.execute("SELECT * FROM timeline WHERE username='"+ username +"'");
        int i = 1;
        for (Row row : results) {
            ResultSet results2 = session.execute("SELECT * FROM tweets WHERE tweet_id="+ row.getUUID("tweet_id") +"");
            for (Row row2 : results2) {
                System.out.format("%d (@%s): %s\n", i, row2.getString("username"), row2.getString("body"));
            }
            i = i + 1;
        }
    }
}
