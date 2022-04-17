/*--------------------------------------------------------
1.  Bryan Morandi / Date: 4.16.2022

        2. Java version used (java -version), if not the official version for the class:

        16.0.2 Amazon coretto

        3. Precise command-line compilation examples / instructions:

        > javac JokeServer.java
        > javac JokeClient.java
        > javac JokeClientAdmin.java
        OR
        > javac JokeServer.java JokeClient.java JokeClientAdmin.java


        4. Precise examples / instructions to run this program:

        e.g.:

        In separate shell windows:

        > java JokeServer
        > java JokeServer secondary
        > java JokeClient
        > java JokeClient localhost
        > java JokeClient localhost localhost
        > java JokeClientAdmin
        > java JokeClientAdmin localhost
        > java JokeClientAdmin localhost localhost

        All acceptable commands are displayed on the various consoles.

        This runs across machines, in which case you have to pass the IP address of
        the server to the clients. For exmaple, if the server is running at
        192.168.1.24 then you would type:

        > java JokeClient 192.168.1.24
        > java JokeClientAdmin 192.168.1.24
        > java JokeClientAdmin localhost 192.168.1.24
        > java JokeClient localhost 192.168.1.24

        5. List of files needed for running the program.

        e.g.:

        a. checklist.html
        b. JokeServer.java
        c. JokeClient.java
        d. JokeClientAdmin.java

        5. Notes:

        e.g.:

        This was a fun assignment, and I feel very good after completing it with all the
        functionality that was required. In the beginning of starting this I was intimidated, but after
        thinking through the logic program by program I was able to figure it out. I am happy that I
        decided use some OOP principles by creating a User class and having the UUID be the key for all of that users' information.
        That organization and mapping helped me out in the long run and made the later tasks easier to complete.
        I also added additional complexity to the Joke labels for an additional challenge, the first label
        (J/P)W->X->Y->Z will always be in order even after joke randomization. The label after the username and before the
        joke (J/P)A-B-C-D will be randomized with the joke after the first cycle is complete.
        Additionally, I was able to run clients on separate computers and all the functionality still worked fine.

        ----------------------------------------------------------*/

import java.io.*; //input libraries for reading/writing user input
import java.net.*; //networking library to get the IP address and create socket
import java.util.*;

public class JokeServer {
    //adding a Global serverMode attribute so that it can be changed by the admin to swap modes
    static boolean serverMode = true; //true = joke mode | false = proverb mode
    //this will be where I store the state when clients connect, the UserID will be the Key and the User class will keep track of the jokes and proverbs seen
    //just like the serverMode, I think this needs to be a static variable, so it can be easily accessed by the Worker thread.
    static HashMap<String, User> StateMaintenance = new HashMap<>();
    static boolean isSecondary = false; // storing a boolean static variable, so I can tell if the server is secondary or not

    public static void main(String[] args) throws IOException {
        int jokeClientPortNum = 4545;
        if(args.length > 0 && Objects.equals(args[0], "secondary") ) {
             jokeClientPortNum = 4546;  //if user types 'secondary' as an arg, then a different port is used
            isSecondary = true;
            System.out.println("This is the secondary server."); //telling the user that they are using the secondary server
        }
        int requestLimit = 6; //this is the number of requests that the server can process at a given point in time... rare occurrence.
        Socket socket; //responsible creating new workers to handle client requests

        //Here I'm starting using the AdminLooper to listen for the ClientAdmin requests to change the servermode
        //This asynchronous thread allows us to listen to both the clientAdmin and the jokeClient at the same time
        AdminLooper clientAdmin = new AdminLooper();
        Thread adminThread = new Thread(clientAdmin);
        adminThread.start();

        //responsible for connecting to the client
        ServerSocket serveSocket = new ServerSocket(jokeClientPortNum, requestLimit);

        System.out.println("Bryan Morandi's Joke Server starting up, listening at port " + jokeClientPortNum + ".\n");

        while(true){
            socket = serveSocket.accept(); //accept is listening constantly due to the while loop until a JokeClient request
            new Worker(socket).start(); //creates worker to handle client request
        }
    }
}

class Worker extends Thread { //Worker socket that is used by the JokeServer to handle client requests
    Socket socket;
    //this will be where I store the state when clients connect, the UserID will be the Key and the User class will keep track of the jokes and proverbs seen
    //just like the serverMode, I think this needs to be a static variable, so it can be easily accessed by the Worker thread.

    Worker(Socket sock) // constructor to create new worker socket
    {
        socket = sock;
    }

    public void run() {
        PrintStream output = null; //sends the client the jokes/proverbs that they have not yet seen
        BufferedReader input = null; //collects the input that the client entered on their terminal(name and userID)
        try {

            input = new BufferedReader(new InputStreamReader(socket.getInputStream())); //Reads the input from the server
            output = new PrintStream(socket.getOutputStream());

            String userID = input.readLine();
            String userName = input.readLine();


            if(!JokeServer.StateMaintenance.containsKey(userID)) { // add user to stateMaintenance if userID is not there yet
                JokeServer.StateMaintenance.put(userID, new User(userName));
                User CurrentUser = JokeServer.StateMaintenance.get(userID); //adding this variable to make the code semi cleaner
                CurrentUser.jokeReference = new Jokes().jokeList;
                CurrentUser.proverbReference = new Proverbs().proverbList;
            }

            User CurrentUser = JokeServer.StateMaintenance.get(userID); //adding this variable to make the code cleaner
            //closes the worker connection, not the server connection

            if(JokeServer.serverMode){
                for(int i = 0; i < CurrentUser.jokeReference.size(); i++) {
                    if(!CurrentUser.jokeMap.containsValue(CurrentUser.jokeReference.get(i))){
                        String key = findKeyName(i);
                        CurrentUser.jokeMap.put(key, CurrentUser.jokeReference.get(i));
                        sendJokeOrProverb(key, CurrentUser, output);
                        break;
                    }
                }
                if (isEndOfCycle(CurrentUser)) {
                    output.println("JOKE CYCLE COMPLETED");
                    CurrentUser.jokeMap.clear(); //resetting the current users joke map to start the cycle over
                    //I found online that an easy and efficient way to shuffle my jokes is to use the Collections library's shuffle method.
                    //It has a runtime of O(n)
                    Collections.shuffle(CurrentUser.jokeReference);
                }
            } else {
                for(int i = 0; i < CurrentUser.proverbReference.size(); i++) {
                    if(!CurrentUser.proverbMap.containsValue(CurrentUser.proverbReference.get(i))){
                        String key = findKeyName(i);
                        CurrentUser.proverbMap.put(key, CurrentUser.proverbReference.get(i));
                        sendJokeOrProverb(key, CurrentUser, output);
                        break;
                    }
                }
                if (isEndOfCycle(CurrentUser)) {
                    output.println("PROVERB CYCLE COMPLETED");
                    CurrentUser.proverbMap.clear();
                    Collections.shuffle(CurrentUser.proverbReference);
                }
            }
            socket.close(); //closes the worker connection, not the server connection
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //method to send the jokes/proverbs not seen to the client.
    static void sendJokeOrProverb(String key, User currentUser, PrintStream output) {
        if(JokeServer.isSecondary) { // will add the characters denoting if this is coming from the secondary server
            output.print("<S2> ");
        }
        if (JokeServer.serverMode) {
            output.println(key + " " + currentUser.name + ": " + currentUser.jokeMap.get(key));
        } else {
            output.println(key + " " + currentUser.name + ": " + currentUser.proverbMap.get(key));
        }
    }

    //I'm using this method so that I can keep track of the jokes when randomized, while also always having the order (J/P)W->X->Y->Z
    //this method adds some additional complexity to this program, I altered the output slightly so that it shows the ordered labels first
    //then the username, then the randomized (J/P)A->B->C->D so my program essentially does both functionalities, just formatted differently.
    static String findKeyName(int index){
        String keyName = "";
        if (JokeServer.serverMode) {
            switch (index) {
                case 0 -> keyName = "JW";
                case 1 -> keyName = "JX";
                case 2 -> keyName = "JY";
                case 3 -> keyName = "JZ";
            }
        }
        else {
            keyName = switch (index) {
                case 0 -> "PW";
                case 1 -> "PX";
                case 2 -> "PY";
                case 3 -> "PZ";
                default -> keyName;
            };
        }
        return keyName;
    }

    //method that I'm using to check if the client has seen all of the jokes/proverbs
    //if true, then tell the client that the cycle has ended and then randomize the jokes and reset the users jokeMap to be empty
    static boolean isEndOfCycle(User currentUser) {
        if(JokeServer.serverMode){
            for(String joke : currentUser.jokeReference) {
                if (!currentUser.jokeMap.containsValue(joke)) {
                    return false;
                }
            }
        } else {
            for(String proverb : currentUser.proverbReference) {
                if (!currentUser.proverbMap.containsValue(proverb)) {
                    return false;
                }
            }
        }
        return true;
    }
}

class AdminWorker extends Thread {
    Socket socket;

    AdminWorker(Socket sock) // constructor to create new worker socket
    {
        socket = sock;
    }

    public void run() {
        PrintStream output = null;
        BufferedReader input = null;
        String modeString = null;

        try {
            output = new PrintStream(socket.getOutputStream());

            if(JokeServer.serverMode){
                JokeServer.serverMode = false;
                modeString = "Proverb";
                System.out.println("Proverb Mode");
            } else {
                JokeServer.serverMode = true;
                modeString = "Joke";
                System.out.println("Joke Mode");
            }
            output.println("Joke Server is now in " + modeString + " mode."); //Tells JokeClientAdmin what mode the Server is now in.
        } catch (IOException ioe){
            System.out.println(ioe);
        }
    }
}

//AdminLooper is utilized so that we can wait for an AdminWorker Thread in addition to waiting for a Joke Client to connect
class AdminLooper implements Runnable {

    public static boolean ModeSwitcher = true;

    public void run() {
        int requestLimit = 6;
        int jokeAdminPortNum = 5050; //this will be the port number that our AdminLooper will listen to get JokeClientAdmin requests. Needs to be different port
        if (JokeServer.isSecondary) {
            jokeAdminPortNum = 5051;
        }
        Socket socket; //responsible creating new workers to handle client requests
        try {
            ServerSocket adminSock = new ServerSocket(jokeAdminPortNum, requestLimit);
            while(ModeSwitcher){
                socket = adminSock.accept();
                new AdminWorker(socket).start();
            }
        } catch (IOException ioe) {System.out.println(ioe);}
    }
}

/* I decided to store the jokes/proverbs on the server, in my opinion, I like having full control of what
the connecting clients have access to. So if I needed to make a change to a joke, or I wanted to
have a weekly joke rotation, it would be much easier to change the jokes on the server side than
having to change the jokes if they were to be stored on all the clients that connect.
*/
class Jokes {
    ArrayList<String> jokeList;
    Jokes() {
        jokeList = new ArrayList<>();
        jokeList.add("JA What kitchen tool does a panda cook with? -- A pan, duh..."); //adding JA - JD here, so they can show the randomization.
        jokeList.add("JB Did you hear about the Italian chef who died? -- He pasta-way");
        jokeList.add("JC Where do sheep go to get their haircut? -- The baa baa shop");
        jokeList.add("JD What do you call birds who stick together? -- Vel-crows");
    }
}

class Proverbs {
    ArrayList<String> proverbList;
    Proverbs() {
        proverbList = new ArrayList<>(); //adding PA - PD here, so they can show the randomization.
        proverbList.add("PA Most young kings get their heads cut off"); // this is from the artist Jean-Michel Basquiat
        proverbList.add("PB Don't try to teach your Grandma to suck eggs");
        proverbList.add("PC A person who sups with the devil should have a long spoon");
        proverbList.add("PD Mighty oaks from little acorns grow");
    }
}

//I thought it would be a good idea to create a User Class that the stateMaintenance hashmap uses to reference UserIDs
//this way I am able to store the users name and both sets of jokes and proverbs they have seen in one place
//I use C# and .NET at work and everything is very organized with attributes in their own classes, so that's why I thought this approach would be helpful.
class User {
    String name;
    HashMap<String, String> jokeMap; //stores jokes that have been seen
    HashMap<String, String> proverbMap; //stores proverbs that have been seen
    ArrayList<String> jokeReference; //this is the list that we run through on each client request to compare with the jokeMap values
    ArrayList<String> proverbReference;//this is the list that we run through on each client request to compare with the proverbMap values
    //also storing the joke/proverb reference within the user allows us to maintain the state for each user so there are no issues with multiple clients
    //and this storage allows us to randomize the reference lists after each cycle is complete with no issues.

    public User(String userName){
        name = userName;
        jokeMap = new HashMap<>();
        proverbMap = new HashMap<>();
        jokeReference = null;
        proverbReference = null;
    }
}



