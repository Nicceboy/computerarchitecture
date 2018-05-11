package org.sample.client;
import java.util.Scanner;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.sample.api.SampleAPI;
import org.sample.api.SampleAPI.ClientState;

class Main {
    public static void main(String[] args) {

        SampleClient sample = new SampleClient();
        sample.run(args);
    }

}

//This Class is to implement basic client on console for our SampleAPI
//SampleAPI is implementation of our protocol in Java language
//Once Client is implemented according to our protocol, it does not need any updates, if our server gains or loses plugins (alias Modules)


//Parameter DEBUG as running argument adds additional printing to SampleAPI

class SampleClient implements SampleAPI.SampleAPIListener {


    void run(String [] args) {


        Scanner reader = new Scanner(System.in);

        System.out.println("Initializing api...");
        SampleAPI client = new SampleAPI(this);
        if (args.length > 0 && args[0].equals("DEBUG")){
            client.deBugginEnabled = true;
        }
        client.prepareClient();
        while (true) {
            System.out.println("Give address, where we should connect: \n");
            String address = reader.nextLine();
            client.attach(address);

            try {
                TimeUnit.SECONDS.sleep(5);
                if (client.myState() == ClientState.EConnected) {
                    break;
                } else {
                    client.detach();
                }

            } catch (InterruptedException e) {
                System.out.println("Something went wrong..");
                System.exit(1);
            }

        }


        while (true) {

            try {
                //Slow down loop a bit
                Thread.sleep(10);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
                //By looking volatile isThreadReady inSampleAPI, we can synchronize printing to console bit better
            if (client.stateOfThread().get()) {
                //  client.setThreadNotReady();
                while (true) {
                    if (client.getModules().isEmpty()) {
                        System.out.println("No modules found currently. Reloading every one minute. ");
                        //TODO There is a bug in IntelliJ console, which causes "\r" not to flush -> Our great timer is not showing up. in case IntelliJ is used.
                        // System.out.print("Stuck");
                        long start = System.currentTimeMillis();
                        while (System.currentTimeMillis() - start < TimeUnit.MINUTES.toMillis(1)) {

                            try {
                               // System.out.println("Timer started.");
                                Thread.sleep(1000);
                                String time  = String.format("Reloading modules in: %d seconds.\r", TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MINUTES.toMillis(1) - (System.currentTimeMillis() - start)));
                                System.out.print(time);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        try {

                            client.initToServerState();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    } else {
                        break;
                    }
                }
                System.out.println("You can use given models to follow some events on specified targets. Sounds interesting? Riight..\n");
                System.out.println("What would you like to do?");
                printCommands();
                try {
                    int input = reader.nextInt();
                    switch (input) {
                        case 1: {
                            printModules(client);
                            pressEnter(client);
                            break;
                        }
                        case 2: {
                            selectModuleAndShowTargets(reader, client);
                            pressEnter(client);
                            break;
                        }
                        case 3: {
                            AddOrRemoveTrackables(reader, client);
                            pressEnter(client);
                            break;
                        }
                        case 4: {
                            runTest(reader, client);
                            pressEnter(client);
                            break;
                        }
                        case 5: {
                            try {
                                System.out.println("Starting syncing..");
                                client.initToServerState();
                            } catch (InterruptedException e) {
                                System.out.println("Something went wrong when reloading...");
                                e.printStackTrace();
                            }

                            //client.setThreadReady();
                            break;
                        }
                        default: {
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

    private void pressEnter(SampleAPI client) {
        while (true) {
            if (client.stateOfThread().get()) {
                System.out.println("Press Enter to continue...");
                try {
                    System.in.read();
                    break;
                } catch (Exception e) {
                    System.out.println("You pressed something else as well..");
                }
            }
        }

    }
    private int getIntInput(Scanner reader, ArrayList<Integer> range){
        while (true){
            try {
                int input = reader.nextInt();
                if (range.contains(input)){
                    return input;
                }else {
                    throw new InputMismatchException();
                }

            }catch (InputMismatchException e){
                System.out.println("Incorrect choice, please try again.");
            }
        }

    }
    private void addNewTarget(){

    }

    private void AddOrRemoveTrackables(Scanner reader, SampleAPI client) {
        //TODO
        System.out.println("Here you can add or remove trackables. Select Module by giving corresponding ID: ");
        while (true) {
            try {
                int input = reader.nextInt();
                List<SampleAPI.Module> modules = client.getModules();
                if (modules.isEmpty()) {
                    modulesNotFound(client);
                    return;
                }
                for (SampleAPI.Module module : modules) {
                    if (module.getId() == input) {
                        System.out.printf("Module '%s' selected. Printing trackable targets: \n", module.getModuleName());
                        List<SampleAPI.Module.ModuleTarget> targets = module.getModuleTargets();
                        if (targets.isEmpty()) {
                            System.out.println("Looks like you have added nothing to targets.");
                            System.out.println("Add a new one?\nPress 1. for Yes\nPress 2. for No\n");
                            return;
                        }
                        System.out.println("\n---List of Targets--");
                        int targetNro = 0;
                        Map<Integer, SampleAPI.Module.ModuleTarget> idAndTarget = new HashMap<>();
                        for (SampleAPI.Module.ModuleTarget target : targets) {
                            idAndTarget.put(targetNro, target);
                            targetNro++;
                            System.out.printf("ID: %d\nName: %s\n",targetNro, target.getName());
                            System.out.print("Trackables: ");
                            for (String trackable : target.getTrackables()) {
                                System.out.print(trackable + ", ");
                            }
                            System.out.println("\n");

                        }
                        System.out.println("\nPress 1. to select old target\nSelect 2. to create new target\nSelect 3. to remove target.\nSelect 4. to return main menu");
                        while (true) {
                            try {
                                input = reader.nextInt();
                                switch (input){
                                    case 1:{
                                        System.out.println("Selecting old target for adding trackables. Give corresponding ID from above: ");
                                        break;
                                    }case 2:{
                                        System.out.println("Creating new target: ");
                                        addNewTarget();
                                        break;
                                    }
                                    case 3:{
                                        System.out.println("Selecting old target for removing trackables. Give corresponding ID from above: ");
                                        break;
                                    }
                                    case 4:{
                                        System.out.println("Returning to main menu..");
                                        return;
                                    }
                                    default:{
                                        throw new InputMismatchException();
                                    }
                                }

                        } catch (InputMismatchException e) {
                            System.out.println("Incorrect choice, please try again.");
                        }
                        }


                     //   return;
                    }

                }
                throw new InputMismatchException();

            } catch (InputMismatchException e) {
                System.out.println("Incorrect choice, please try again.");
            }
        }
       // System.out.println("Sorry, not implemented yet. Run tests.");


    }

    private void runTest(Scanner reader, SampleAPI client) {
        boolean isTargetNew = true;
        int selectedModule = 1;
        String ExtraInfo = "ContryNews";
        String ExtraInfo2 = "SomeStuff";
        String ExtraInfo3 = "C:\\Users\\Niklas Saari\\Desktop\\CompArch\\artifacts\\KeywordServer_jar\\plugins, recursive";
        String[] testAddables = {"Kana", "Koira", "Kissa", "Lehma", "Sika"};
        String[] testRemovables = {"Heippa", "Vaan"};
        ArrayList<String> trackablesToAdd = new ArrayList<String>(Arrays.asList(testAddables));
        ArrayList<String> trackablesToremove = new ArrayList<String>(Arrays.asList(testRemovables));
        for (SampleAPI.Module module : client.getModules()) {

            if (selectedModule == module.getId()) {
                for (SampleAPI.Module.ModuleTarget target : module.getModuleTargets()) {
                    if (target.getName().equals(ExtraInfo3)) {
                        target.storeTempAddables(trackablesToAdd);
                        target.storeTempRemovables(trackablesToremove);
                        target.newChanges();
                        isTargetNew = false;
                        break;
                    }

                }
                if (isTargetNew) {
                    SampleAPI.Module.ModuleTarget newTarget = module.new ModuleTarget(ExtraInfo3);
                    newTarget.storeTempAddables(trackablesToAdd);
                    newTarget.storeTempRemovables(trackablesToremove);
                    newTarget.newChanges();
                    module.addTarget(newTarget);
                    System.out.printf("New target %s added.\n", ExtraInfo);
                }
            }

        }
        try {
            client.commitChanges();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void printModules(SampleAPI client) {
        List<SampleAPI.Module> modules = client.getModules();
        if (modules.isEmpty()) {
            modulesNotFound(client);
            return;
        }
        System.out.println("\n---List of Modules--\n");
        System.out.printf("%-30s %15s\n", "NAME", "ID");
        for (SampleAPI.Module module : modules) {
            System.out.println(String.format("%-45s", " ").replace(' ', '-'));
            System.out.printf("%-30s %15d\n", module.getModuleName(), module.getId());
            System.out.println(String.format("%-45s", " ").replace(' ', '-'));
            System.out.println(String.format("%-45s\n", module.getModuleDesc()));
            System.out.println(String.format("%-45s\n", module.getModuleUsage()));
            System.out.println(String.format("%-45s", " ").replace(' ', '-'));


        }
    }

    private void modulesNotFound(SampleAPI client) {
        System.out.println("Looks like server is not currently supporting modules at all...");
        System.out.println("Reloading modules from server...");
        try {
            client.initToServerState();
        } catch (InterruptedException e) {
            System.out.println("Something went wrong when reloading...");
            e.printStackTrace();
        }
    }

    private void selectModuleAndShowTargets(Scanner reader, SampleAPI client) {
        System.out.println("Select Module by giving corresponding ID: ");
        while (true) {
            try {
                int input = reader.nextInt();
                List<SampleAPI.Module> modules = client.getModules();
                if (modules.isEmpty()) {
                    modulesNotFound(client);
                    return;
                }
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
                            System.out.print("Trackables: ");
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
            }
        }

    }

    private void printCommands() {

        System.out.println("Press 1. to get list of modules.");
        System.out.println("Press 2. to select module and check your targets.");
        System.out.println("Press 3. to add or remove trackable objects from specified target on speficied model.");
        System.out.println("Press 4. to run test sample.");
        System.out.println("Press 5. to manually syncronize with server.");
    }

    public void notify(String data) {
        System.out.println(data);
    }

    public void changeEventHappened(String response, String text, String file) {
        System.out.print("Do something");
    }

}