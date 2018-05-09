package org.sample.client;

import java.rmi.UnknownHostException;
import java.util.Scanner;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sample.api.SampleAPI;
import org.sample.api.SampleAPI.ClientState;

class Main {
    public static void main(String[] args) {

        SampleClient sample = new SampleClient();
        sample.run();

    }

}

class SampleClient implements SampleAPI.SampleAPIListener{


    public void run() {


        Scanner reader = new Scanner(System.in);

        System.out.println("Initializing api...");
        SampleAPI client = new SampleAPI(this);
        client.prepareClient();
        while (true) {
            System.out.println("Give address, where we should connect: \n");
            String address = reader.nextLine();
            client.attach(address);

            try {
                TimeUnit.SECONDS.sleep(5);
                if (client.myState() == ClientState.EConnected) {
                   break;
                }else {
                    client.detach();
                }

            }catch (InterruptedException e){
                System.out.println("Something went wrong..");
                System.exit(1);
            }

        }


        while (true) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            }catch (InterruptedException e){
                System.out.println("Something went wrong..");
                System.exit(1);
            }

            if (client.stateOfThread()) {
                client.setThreadNotReady();
                System.out.println("You can use given models to follow some events on specified targets. Sounds interesting? Riight..\n");
                System.out.println("What would you like to do?");
                printCommands();
                try {
                    int input = reader.nextInt();
                    switch (input) {
                        case 1: {
                            printModules(client);
                            pressEnter();
                            client.setThreadReady();
                            break;
                        }
                        case 2: {
                            selectModuleAndShowTargets(reader, client);
                            pressEnter();
                            client.setThreadReady();
                            break;
                        }
                        case 3: {
                            AddOrRemoveTrackables(reader, client);
                            pressEnter();
                            client.setThreadReady();
                            break;
                        }
                        default:{
                            throw new InputMismatchException();
                                                    }
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Incorrect choice, please try again.");
                    client.setThreadReady();
                }


            }
        }


    }
    private void pressEnter(){

        System.out.println("Press Enter to continue...");
        try
        {
            System.in.read();
        }
        catch(Exception e)
        {
            System.out.println("You pressed something else as well..");
        }

    }
    private void AddOrRemoveTrackables( Scanner reader, SampleAPI client){

        int selectedModule = 1;
        String ExtraInfo1 = "ContryNews";
        String ExtraInfo2 = "SomeStuff";
        String [] testAddables = {"Kana", "Koira", "Kissa", "Lehma", "Sika"};
        String [] testRemovables = {"Heipa", "Vaan"};
        ArrayList<String> trackablesToAdd = new ArrayList<String>(Arrays.asList(testAddables));
        ArrayList<String> trackablesToremove = new ArrayList<String>(Arrays.asList(testRemovables));
        for (SampleAPI.Module module : client.getModules() ){

            if (selectedModule == module.getId()){
                for (SampleAPI.Module.ModuleTarget target : module.getModuleTargets()){
                    if (target.getName() == ExtraInfo1){
                        target.storeTempAddables(trackablesToAdd);
                        target.storeTempRemovables(trackablesToremove);
                    }
                }
            }

        }


    }

    private void printModules(SampleAPI client) {
        List<SampleAPI.Module> modules = client.getModules();
        System.out.println("\n---List of Modules--\n");
        System.out.printf("%-15s %15s\n", "NAME", "ID");
        for (SampleAPI.Module module : modules) {
            System.out.println(String.format("%-30s", " ").replace(' ', '-'));
            System.out.printf("%-15s %15d\n", module.getModuleName(), module.getId());

        }
    }

    private void selectModuleAndShowTargets(Scanner reader, SampleAPI client) {
        System.out.println("Select Module by giving corresponding ID: ");
        while (true) {
            try {
                int input = reader.nextInt();
                List<SampleAPI.Module> modules = client.getModules();
                for (SampleAPI.Module module : modules) {
                    if (module.getId() == input) {
                        System.out.printf("Module '%s' selected. Printing trackable targets: \n", module.getModuleName());
                        List<SampleAPI.Module.ModuleTarget> targets = module.getModuleTargets();
                        if (targets.isEmpty()) {
                            System.out.println("Looks like you have added nothing to targets.");
                            return;
                        }
                        System.out.println("\n---List of Targets--");
                        for (SampleAPI.Module.ModuleTarget target : targets) {
                            System.out.printf("Name: %s\n", target.getName());
                            System.out.print("Targets: %s");
                            for (String trackable : target.getTrackables()) {
                                System.out.print(trackable + ", ");
                            }
                            System.out.println("\n");

                        }
                        return;
                    }

                }
                throw new InputMismatchException();

            } catch (InputMismatchException e) {
                System.out.println("Incorrect choice, please try again.");
                reader.next();
            }
        }

    }

    private void printCommands() {

        System.out.println("Press 1. to get list of modules.");
        System.out.println("Press 2. to select module and check your targets.");
        System.out.println("Press 3. to add or remove trackable objects from specified target on speficied model.");
    }

    public void notify(String data) {
        System.out.println(data);
    }
    public void changeEventHappened(String response, String text, String file){
        System.out.print("Do something");
    }

}