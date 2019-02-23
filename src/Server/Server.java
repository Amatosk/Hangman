package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Server {
	
	static ServerSocket server;
	
	//Array list containing all the possible words that could be chosen for the game
	static List<String> listOfWords = new ArrayList<String>();
	
	//ArrayLists containing the player scoreboards on different difficulties.
	static List<Player> easyScoreboard = new ArrayList<Player>();
	static List<Player> mediumScoreboard = new ArrayList<Player>();
	static List<Player> hardScoreboard = new ArrayList<Player>();

	
	public static void main(String[] args) {
		
		//Add three values to each of the scoreboard ArrayLists, so that if a player is the
		//first to play and join the scoreboard, dummy values can be used to prevent null pointer
		//issues as the scoreboard is created visually for the player.
		for(int i = 0; i < 3; i++) {
			easyScoreboard.add(new Player("zzz", 7, "9:59.999"));
			mediumScoreboard.add(new Player("zzz", 5, "9:59.999"));
			hardScoreboard.add(new Player("zzz", 3, "9:59.999"));
		}
		
		try {
			//Initiates the ServerSocket with the port number 4500
			server = new ServerSocket(4500);
			
			//A String is made and the absolute file path to the current directory is retreived
			String filePath = new File("").getAbsolutePath();

			//Adding on from the absolute file path, go through some more directories to reach
			//the txt file.
			File txtFile = new File(filePath + "/src/Resources/words.txt");
			
			//Buffered Reader combined with a File Reader to read entire lines from the words.txt 
			//file in the Resources folder.
			BufferedReader buffRead = new BufferedReader(new FileReader(txtFile));
			
			//String that gets the first line from the Buffered Reader
			String line = buffRead.readLine();
			
			//While the String is not null (Which will happen when we reach the end of the file)
			while(line != null){
				
				//Add the current value of the String to the array list...
				listOfWords.add(line);
				
				//and read the next line from the file. 
				line = buffRead.readLine();
				
				//The loop will then check again whether the new value of the String is null or not
			}
			
			//Close the Buffered Reader to prevent a resource leak.
			buffRead.close();
			
			//An infinite loop means that the server can wait indefinitely for something to happen
			while(true) {
				
				//When a client connects, that client is saved to a Socket object
				Socket client = server.accept();
				
				//Print to the console what the IP address of that client is, for debugging
				System.out.println("Connected to client at " + client.getInetAddress());
				
				//Make a thread for that client and start that thread, so that can handle
				//communication with the individual client instead of this class. The loop can
				//then start anew and wait for the next client.
				if(client.isConnected()) {
					ClientThread thread = new ClientThread(client);
					thread.start();
				}
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	//Different threads may try to call this method at the same time, so the synchronized
	//keyword is used to prevent concurrency issues.
	static synchronized String[][] easyAddToList(String name, int guess, String time){
		
		//Create a 2D String array in preparation, of length 3x3
		String[][] relevantThree = new String[3][3];
		
		//Create a Player object out of the parameters given to this method
		Player playerToAdd = new Player(name,guess,time);
		
		//Add the created Player object to the easyScoreboard ArrayList
		easyScoreboard.add(playerToAdd);
		
		//Sort the easyScoreboard by order of incorrect guesses, lower ones appearing higher than others.
		//If the incorrect guesses are the same, sort them by time instead
		Collections.sort(easyScoreboard,new Comparator<Player>(){
			@Override
			public int compare(Player a, Player b) {
				return (a.getIncorrectGuesses() - b.getIncorrectGuesses()) != 0 ? 
						(a.getIncorrectGuesses() - b.getIncorrectGuesses()) : 
							(a.getTime().compareTo(b.getTime()));
			}
		});
		
		//Find index position of the Player we created earlier in this method and save that index
		int playerPos = easyScoreboard.indexOf(playerToAdd);
		
		//If the Player object added is still in the last position of the ArrayList, get the Name, 
		//IncorrectGuesses and Time of the Player and the two above them on the list and add those values
		//to the 2D String array. Ultimately, we should get something similar to:
		//{{"abc", "0", "1:20.459"},
		// {"def", "0", "1:49.750"},
		// {"ghi", "1", "1:31.760"}}
		if(playerPos==easyScoreboard.size()) {
			for(int i = 0; i < 3; i++) {
				relevantThree[relevantThree.length-i][0] = easyScoreboard.get(playerPos-i).getName();
				relevantThree[relevantThree.length-i][1] = Integer.toString(easyScoreboard.get(playerPos-i).getIncorrectGuesses());
				relevantThree[relevantThree.length-i][2] = easyScoreboard.get(playerPos-i).getTime();
			}
		}
		
		//Otherwise, if the Player object added is now in the very first position of the ArrayList, get 
		//the Name, IncorrectGuesses and Time of the Player and the two below them on the list and add 
		//those valies to the 2D String array.
		else if(playerPos==0) {
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = easyScoreboard.get(i).getName();
				relevantThree[i][1] = Integer.toString(easyScoreboard.get(i).getIncorrectGuesses());
				relevantThree[i][2] = easyScoreboard.get(i).getTime();
			}
		}
		
		//Otherwise, get the Name, IncorrectGuesses and Time of the Player and the ones above and below
		//the Player on the ArrayList and add them to the 2D Array.
		else{
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = easyScoreboard.get(playerPos+(i-1)).getName();
				relevantThree[i][1] = Integer.toString(easyScoreboard.get(playerPos+(i-1)).getIncorrectGuesses());
				relevantThree[i][2] = easyScoreboard.get(playerPos+(i-1)).getTime();
			}
		}
		
		//Return the 2D String Array
		return relevantThree;
	}
	
	//Method that does exactly the same as above, except working with the mediumScoreboard instead
	static synchronized String[][] mediumAddToList(String name, int guess, String time){
		
		String[][] relevantThree = new String[3][3];		
		Player playerToAdd = new Player(name,guess,time);
		
		mediumScoreboard.add(playerToAdd);
		
		Collections.sort(mediumScoreboard,new Comparator<Player>(){
			@Override
			public int compare(Player a, Player b) {
				return (a.getIncorrectGuesses() - b.getIncorrectGuesses()) != 0 ? 
						(a.getIncorrectGuesses() - b.getIncorrectGuesses()) : 
							(a.getTime().compareTo(b.getTime()));
			}
		});
		
		int playerPos = mediumScoreboard.indexOf(playerToAdd);
		
		if(playerPos==mediumScoreboard.size()) {
			for(int i = 0; i < 3; i++) {
				relevantThree[relevantThree.length-i][0] = mediumScoreboard.get(playerPos-i).getName();
				relevantThree[relevantThree.length-i][1] = Integer.toString(mediumScoreboard.get(playerPos-i).getIncorrectGuesses());
				relevantThree[relevantThree.length-i][2] = mediumScoreboard.get(playerPos-i).getTime();
			}
		}else if(playerPos==0) {
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = mediumScoreboard.get(i).getName();
				relevantThree[i][1] = Integer.toString(mediumScoreboard.get(i).getIncorrectGuesses());
				relevantThree[i][2] = mediumScoreboard.get(i).getTime();
			}
		}else{
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = mediumScoreboard.get(playerPos+(i-1)).getName();
				relevantThree[i][1] = Integer.toString(mediumScoreboard.get(playerPos+(i-1)).getIncorrectGuesses());
				relevantThree[i][2] = mediumScoreboard.get(playerPos+(i-1)).getTime();
			}
		}
		return relevantThree;
	}
	
	//Method that does exactly the same as above, except working with the hardScoreboard instead
	static synchronized String[][] hardAddToList(String name, int guess, String time){
		
		String[][] relevantThree = new String[3][3];		
		Player playerToAdd = new Player(name,guess,time);
		
		hardScoreboard.add(playerToAdd);
		
		Collections.sort(hardScoreboard,new Comparator<Player>(){
			@Override
			public int compare(Player a, Player b) {
				return (a.getIncorrectGuesses() - b.getIncorrectGuesses()) != 0 ? 
						(a.getIncorrectGuesses() - b.getIncorrectGuesses()) : 
							(a.getTime().compareTo(b.getTime()));
			}
		});
		
		int playerPos = hardScoreboard.indexOf(playerToAdd);
		
		if(playerPos==hardScoreboard.size()) {
			for(int i = 0; i < 3; i++) {
				relevantThree[relevantThree.length-i][0] = hardScoreboard.get(playerPos-i).getName();
				relevantThree[relevantThree.length-i][1] = Integer.toString(hardScoreboard.get(playerPos-i).getIncorrectGuesses());
				relevantThree[relevantThree.length-i][2] = hardScoreboard.get(playerPos-i).getTime();
			}
		}else if(playerPos==0) {
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = hardScoreboard.get(i).getName();
				relevantThree[i][1] = Integer.toString(hardScoreboard.get(i).getIncorrectGuesses());
				relevantThree[i][2] = hardScoreboard.get(i).getTime();
			}
		}else{
			for(int i = 0; i < 3; i++) {
				relevantThree[i][0] = hardScoreboard.get(playerPos+(i-1)).getName();
				relevantThree[i][1] = Integer.toString(hardScoreboard.get(playerPos+(i-1)).getIncorrectGuesses());
				relevantThree[i][2] = hardScoreboard.get(playerPos+(i-1)).getTime();
			}
		}
		return relevantThree;
	}
}