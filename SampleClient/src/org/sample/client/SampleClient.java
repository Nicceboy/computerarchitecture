package org.sample.client;

import java.awt.event.KeyEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sun.org.apache.xpath.internal.operations.Mod;
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


    void run(String[] args) {


        Scanner reader = new Scanner(System.in);

        System.out.println("Initializing api...");
        SampleAPI client = new SampleAPI(this);
        if (args.length > 0 && args[0].equals("DEBUG")) {
            client.deBugginEnabled = true;
        }
        client.prepareClient();
        while (true) {
            System.out.println("\nGive address where we should connect: \n");
            String address = reader.nextLine();
            client.attach(address);
            long start = System.currentTimeMillis();
            System.out.println();
            while (System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(10)) {
                try {
                    if (client.myState() == ClientState.EConnected) {
                        break;
                    }else{
                        client.attach(address);
                        String time = String.format("Trying to connect address %s... Disconnect in %d seconds.\r", address, TimeUnit.MILLISECONDS.toSeconds(TimeUnit.SECONDS.toMillis(10) - (System.currentTimeMillis() - start)));
                        System.out.print(time);
                        Thread.sleep(1000);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (client.myState() == ClientState.EConnected) {
                break;
            }else {

                client.detach();

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
                                String time = String.format("Reloading modules in: %d seconds.\r", TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MINUTES.toMillis(1) - (System.currentTimeMillis() - start)));
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
                            AddOrRemoveTrackables(client);
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
                    reader.next();
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

    private int getIntInput(ArrayList<Integer> range) {
        Scanner reader = new Scanner(System.in);
        while (true) {
            try {
                int input = reader.nextInt();
                if (range.contains(input)) {
                    return input;
                } else {
                    throw new InputMismatchException();
                }

            } catch (InputMismatchException e) {
                System.out.println("Incorrect choice, please try again.");
                reader.next();
            }
        }

    }

    private void addNewTarget(SampleAPI.Module module) {

        Scanner reader = new Scanner(System.in);
        String input;
        ArrayList<String> trackables = new ArrayList<String>();

        String targetName;
        System.out.printf("Please, give a target. Usage of model:\n%s\n", module.getModuleUsage());
        targetName = reader.nextLine();
        // module.addTarget(targetName);
        SampleAPI.Module.ModuleTarget target = module.new ModuleTarget(targetName);
        module.addTarget(AddOrRemoveTrackables(target, Modify.ADD));


    }
    private void removeTarget(){

    }
    private enum Modify {ADD, REMOVE}

    private SampleAPI.Module.ModuleTarget AddOrRemoveTrackables(SampleAPI.Module.ModuleTarget target,  Modify method) {
        Scanner reader = new Scanner(System.in);
        String input;
        ArrayList<String> trackables = new ArrayList<String>();

        String event1 = method == Modify.ADD ? "trackable" : "removable";
        String event2 = method == Modify.ADD ? "you don't want track" : "you are not tracking that";

        System.out.printf("Thank you. Now, give %s things. For example keywords. Give keyword, press Enter. Stop by writing QUIT (Hopefully you don't want track that (: )'", event1, event2);
        while (true) {
            input = reader.nextLine();
            if (input.equals("QUIT")) {
                break;
            }
            trackables.add(input);
            System.out.println("OK. Give new entry:");

        }
        if (method == Modify.ADD){
            target.storeTempAddables(trackables);
        }
        else if(method == Modify.REMOVE){
            target.storeTempRemovables(trackables);
        }
        else {
            throw new InputMismatchException("Unknown enum");
        }

        target.newChanges();
        String resultedWay = method == Modify.ADD ? "added to" : "removed from";
        System.out.printf("Trackables %s target %s.\n", resultedWay, target.getName());
        return target;
    }


    private void AddOrRemoveTrackables(SampleAPI client) {
        //TODO
        ArrayList<Integer> tmp_range = new ArrayList<>();
        int selectedModule;
        Map<Integer, SampleAPI.Module> IntAndModule = new HashMap<>();
        Map<Integer, SampleAPI.Module.ModuleTarget> idAndTarget = new HashMap<>();
        HashMap<Integer, Boolean> YesOrNo = new HashMap<Integer, Boolean>() {{
            put(1, true);
            put(2, false);
        }};

        int temp_input;
        System.out.println("Here you can add or remove trackables. Select Module by giving corresponding ID: ");
        while (true) {

            List<SampleAPI.Module> modules = client.getModules();
            if (modules.isEmpty()) {
                modulesNotFound(client);
                return;
            }
            for (SampleAPI.Module module : modules) {
                tmp_range.add(module.getId());
                IntAndModule.put(module.getId(), module);
            }
            selectedModule = getIntInput(tmp_range);

            System.out.printf("Module '%s' selected. Printing trackable targets: \n", IntAndModule.get(selectedModule).getModuleName());
            List<SampleAPI.Module.ModuleTarget> targets = IntAndModule.get(selectedModule).getModuleTargets();
            if (targets.isEmpty()) {
                System.out.println("Looks like you have added nothing to targets.Add a new one?");
                System.out.println("\n\nPress 1. for Yes\nPress 2. for No\n");
                ArrayList<Integer> test = new ArrayList<>(YesOrNo.keySet());
                if (YesOrNo.get(getIntInput(test))) {
                    addNewTarget(IntAndModule.get(selectedModule));
                } else {
                    return;
                }
            }
            boolean notListedTargetsYet = true;
            int targetNro = 0;


            while (true) {
                if (notListedTargetsYet) {
                  targetNro = 0;
                }
                System.out.println("\n---List of Targets--");
                for (SampleAPI.Module.ModuleTarget target : targets) {
                    if(notListedTargetsYet) {
                        targetNro++;
                        idAndTarget.put(targetNro, target);
                    }
                    System.out.printf("ID: %d\nName: %s\n", targetNro, target.getName());
                    System.out.print("Trackables: ");
                    for (String trackable : target.getTrackables()) {
                        System.out.print(trackable + ", ");
                    }
                    System.out.println("\n");
                    System.out.print("Trackables to add pending: ");
                    for (String trackable : target.getTempAddables()) {
                        System.out.print(trackable + ", ");
                    }
                    System.out.println("\n");
                    System.out.print("Trackables to remove pending: ");
                    for (String trackable : target.getTempRemovables()) {
                        System.out.print(trackable + ", ");
                    }
                    System.out.println("\n");


                }
                notListedTargetsYet = false;

                System.out.println("\nSelect 1. to select old target to add trackables\nSelect 2. to create new target and trackables\nSelect 3. to remove trackables from target.\nSelect 4. to remove whole target\nSelect 5. to Save and return main menu\nSelect 6. To cancel and return main menu.");
                Integer[] choices = {1, 2, 3, 4, 5, 6};
                temp_input = getIntInput(new ArrayList<Integer>(Arrays.asList(choices)));
                switch (temp_input) {
                    case 1: {
                        System.out.println("Selecting old target for adding trackables. Give corresponding ID from above: ");
                        temp_input = getIntInput(new ArrayList<>(idAndTarget.keySet()));
                        AddOrRemoveTrackables(idAndTarget.get(temp_input), Modify.ADD);

                        break;
                    }
                    case 2: {
                        System.out.println("Creating new target: ");
                        addNewTarget(IntAndModule.get(selectedModule));
                        notListedTargetsYet = true;
                        break;
                    }
                    case 3: {
                        System.out.println("Selecting old target for removing trackables. Give corresponding ID from above: ");
                        temp_input = getIntInput(new ArrayList<>(idAndTarget.keySet()));
                        AddOrRemoveTrackables(idAndTarget.get(temp_input), Modify.REMOVE);
                        break;
                    }
                    case 4:{
                        System.out.println("Selecting old target and removing it. Give corresponding ID from above: ");
                        temp_input = getIntInput(new ArrayList<>(idAndTarget.keySet()));
                        IntAndModule.get(selectedModule).removeTarget(idAndTarget.get(temp_input));
                        break;
                    }
                    case 5: {
                        System.out.println("Saving changes...");
                        try {
                            client.commitChanges();
                        } catch (InterruptedException e) {
                            System.out.println("Updates failed: " + e);
                        }
                        return;
                    }
                    case 6: {
                        System.out.println("Returning without savigin...");
                        return;
                    }
                    default: {
                        throw new InputMismatchException();
                    }
                }
            }
        }
    }

    private void runTest(Scanner reader, SampleAPI client) {
        boolean isTargetNew = true;
        int selectedModule = 1;
        String ExtraInfo = "ContryNews";
        String ExtraInfo2 = "SomeStuff";
        String ExtraInfo3 = "C:\\Test, recursive";
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

        HashMap<Integer, Boolean> YesOrNo = new HashMap<Integer, Boolean>() {{
            put(1, true);
            put(2, false);
        }};
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
                            System.out.println("Looks like you have added nothing to targets. Add a new one?");
                            System.out.println("\n\nPress 1. for Yes\nPress 2. for No\n");
                            ArrayList<Integer> test = new ArrayList<>(YesOrNo.keySet());
                            if (YesOrNo.get(getIntInput(test))) {
                                addNewTarget(module);
                                client.commitChanges();
                            } else {
                                return;
                            }
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
            } catch (InterruptedException e) {
                System.out.println("Unable to add new target: " + e);
            }
        }

    }

    private void printCommands() {

        System.out.println("Press 1. to get list of modules.");
        System.out.println("Press 2. to select module and check your targets.");
        System.out.println("Press 3. to add or remove trackable objects from specified target on speficied model.");
        System.out.println("Press 4. to run test sample.");
        System.out.println("Press 5. to manually synchronize with server.");
    }

    public void notify(String data) {
        System.out.println(data);
    }

    public void changeEventHappened(String response, String text, String file) {
        System.out.println("WE GOT A HIT!!");
        System.out.printf("Module name: %s\nModule target: %s\nFound things: %s\n", response, text, file);
    }

}