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
import java.util.UUID; //I found this UUID class library online and thought it would be an easy way to create unique ID's for client connections

public class JokeClient {

    public static void main(String[] args) throws IOException {
        System.out.println("Bryan Morandi's Joke Client.\n");
        System.out.println("Note to grader: I added some functionality to the joke/proverb labels, the initial label " +
                "will be W through X and will always be in order even after shuffling. I still have included the required " +
                "A through D labels and they will be randomized with the jokes, but I placed them after the username " +
                "prepending the joke. I wanted to add additional complexity to show that the jokes can be randomized " +
                "while still keeping the label order consistent.\n");
        String primaryServer = "localHost";
        String secondaryServer = null;
        String currentServer = primaryServer; //using this variable to be able to toggle between servers
        int primaryPortNum = 4545;
        int secondaryPortNum = 4546;
        int currentPort = primaryPortNum;
        if (args.length > 0) {
            primaryServer = args[0];
            currentServer = primaryServer;
            System.out.println("Server one: " + primaryServer + ", port: " + primaryPortNum);
        }
        if (args.length > 1) {
            secondaryServer = args[1];
            System.out.println("Server two: " + secondaryServer + ", port: " + secondaryPortNum);
        }
        if (args.length < 1) { //case if no args are given, just give the localHost and 4545 port num
            System.out.println("Using server: " + primaryServer + ", Port: " + primaryPortNum);
        }
        System.out.println("Now communicating with: " + currentServer + ", port " + currentPort + "\n"); //initially tells the user what server/port their communicating with
        System.out.print("Please enter your name: "); //this is the name that will be stored in the User class on the Server side
        System.out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String userName = in.readLine();
        String userID = UUID.randomUUID().toString(); //creates a unique userID on first connection, this will be the key to look up this users joke/proverb history
        try {
            String getJokeOrProverb;
            do {
                System.out.print
                        ("Click Enter to get joke or proverb, click 's' to switch server, or type (quit) to end: ");
                System.out.flush(); //gets rid of any existing output stream buffer
                getJokeOrProverb = in.readLine(); //reads users input from the terminal
                if (!getJokeOrProverb.contains("quit"))  //if user hasn't entered 'quit' into the terminal, then find the remote address
                    if (getJokeOrProverb.contains("s")) {
                        if (secondaryServer == null) {
                            System.out.println("No secondary server being used\n");
                        }
                        else if (currentServer.equals(primaryServer) && currentPort == primaryPortNum){
                            currentServer = secondaryServer;
                            currentPort = secondaryPortNum;
                            System.out.println("Now communicating with: " + currentServer + ", port " + currentPort + "\n");
                        } else {
                            currentServer = primaryServer;
                            currentPort = primaryPortNum;
                            System.out.println("Now communicating with: " + currentServer + ", port " + currentPort + "\n");
                        }
                    } else {
                        connectToServer(currentServer, currentPort, userID, userName);
                    }
            } while (!getJokeOrProverb.contains("quit")); //while user has not entered 'quit' in terminal window
            System.out.println("Cancelled by user request");
        } catch (IOException ex) {ex.printStackTrace();}
    }

        static void connectToServer(String serveName, int portNum, String userID, String name ){
            Socket socket;
            BufferedReader fromServe;
            PrintStream toServe;
            String textFromServe;

            try {
                //open connection to the same port as our server
                socket = new Socket(serveName, portNum);

                fromServe = new BufferedReader(new InputStreamReader(socket.getInputStream())); //Reads the input from the server
                toServe = new PrintStream(socket.getOutputStream());

                //sends userID and name to server
                toServe.println(userID);
                toServe.println(name);

                //loop to read the printJokeOrProverb method that the Server worker produces when reading the clients userName and UUID
                for (int i = 1; i <= 3; i++){
                    textFromServe = fromServe.readLine();
                    if (textFromServe != null) System.out.println(textFromServe);
                }
                socket.close();
            } catch (IOException ex) {
                System.out.println("Socket error.");
                ex.printStackTrace();
            }
        }
    }

