package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Arrays;

public class ClientThread extends Thread{
	
	Socket client;
	String wordToFind;
	PrintWriter out;
	ObjectOutputStream objOut;
	BufferedReader in;
	String difficultyLevel;
	char[] currentWordState;
	
	public ClientThread(Socket s) {
		//When this thread is first created, it saves the Socket passed over.
		//This happens before the Thread is even told to start, readying it for the run method.
		client = s;
	}
	
	public void run() {
		
		try {
			//Print Writer used to output Strings through the Socket
			out = new PrintWriter(client.getOutputStream(),true);
			
			//Object Output Stream, used to output objects through the Client.
			objOut = new ObjectOutputStream(client.getOutputStream());
			
			//Buffered Reader, which builds upon the Input Stream Reader, which receives 
			//Strings from the Socket. 
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			//Infinite while loop, staying active so that it can receive input at any time
			while(true) {
				//Attempts to read input from the Buffered Reader and splits the input into 
				//a String array, splitting one String from another whenever it comes across a space
				String[] messageCheck = in.readLine().split(" ");
				
				//Based on the first String stored in the array, different case statements are called
				switch(messageCheck[0]) {
				case "StartUpGame" : 
					
					//Ternary operator, asking whether the second element of the messageCheck array
					//is equal to 7. If so, difficultyLevel is set to Easy. Otherwise, we use another
					//ternary operator to ask whether the second element of messageCheck is equal to
					//5. If so, difficultyLevel is set to Medium. Otherwise, it is set to Hard.
					difficultyLevel = (messageCheck[1].equals("7")) ? "Easy" : (messageCheck[1].equals("5")) ? "Medium" : "Hard";
					
					//Math.random generates a random number between 0.0 and 1.0. By multiplying it by
					//the size of the ArrayList over in the Server class and converting the result to
					//an int, we get a whole number between 0 and the size of the list. That number is
					//used to get one specific element from that ArrayList and saves that word to the
					//string variable here, called wordToFind. 
					wordToFind = Server.listOfWords.get((int)(Math.random()*(Server.listOfWords.size())));
					
					//The char array is now set to a length equal to the amount of characters in the word
					//decided upon above.
					currentWordState = new char[wordToFind.length()];
					
					//The array is filled with underscores. The Arrays.fill command helps to get around the
					//varied length of the array and is more convenient than a for loop
					Arrays.fill(currentWordState,'_');

					//Debugging
					System.out.println("User " + client.getLocalSocketAddress() + " is playing on " + difficultyLevel + " mode and is looking for the word \"" + wordToFind + "\" ");
					
					//Send a String over to the client, who will convert it to a boolean
					out.println("true");
					
					break;
					
				case "GetWord" :
					//Return the word that the player should be looking for
					out.println(new String(currentWordState));
					
					break;
					
				case "LetterGuess" :
					//A single character is sent over, but is converted to a String so that it can pass
					//through the Socket connection. By using charAt on the String, we can retreive the
					//character, even though there should only be one, and convert it back to a char.
					char theGuess = messageCheck[1].charAt(0);
					
					//A boolean that is set to false in preparation for the following loop
					boolean success = false;
					
					//Go through the word that the player should be searching for and compare the guessed
					//character to each character of the word. 
					for(int i = 0; i < wordToFind.length() ; i++) {
						if(wordToFind.charAt(i)== Character.toLowerCase(theGuess)) {
							//If the letter matches one of the letters in the word, change the value of the
							//char array at the same position as in the word, and set the boolean to true. 
							//The boolean will be used on the client's side to determine which message is 
							//displayed on their screen as a result of their guess.
							currentWordState[i] = Character.toLowerCase(theGuess);
							success = true;
						}
					}
					
					//The char array is converted to a String
					String returnValue = new String(currentWordState);
					//Check if the String is exactly equal to the word
					boolean finished = (wordToFind.equals(returnValue));

					//Send the potentially incomplete word, along with the success boolean and
					//finished boolean, with spaces between each value, back to the client.
					out.println(returnValue + " " + success + " " + finished);
					
					break;
					
				case "Victory" :
					
					//Convert the third and fourth Strings to an int and long respectively
					int guess = Integer.parseInt(messageCheck[2]);
					long time = Long.parseLong(messageCheck[3]);
					
					//Break down the milliseconds from the time variable into minutes and seconds. 
					//If time is 10 minutes of greater, minutes and seconds will be 9:59 as a form 
					//of cap, preventing issues with layout later, thanks to large numbers.
					int minutes 	 = (int)((time/1000)/60) >= 10 ? 9   : (int)((time/1000)/60);
					int sec			 = (int)((time/1000)/60) >= 10 ? 59  : (int)((time/1000)%60);
					int millis		 = (int)((time/1000)/60) >= 10 ? 999 : (int)(time%1000);
					
					//Create a Decimal Format that forces two digits to be displayed. Use it on
					//the sec int variable and save the result to a String
					DecimalFormat df1 = new DecimalFormat("00");
					String seconds = df1.format(sec);
					
					//A second Decimal Format, this time, forcing three digits to be displayed.
					//Used on the millis int variable. 
					DecimalFormat df2 = new DecimalFormat("000");
					String milliseconds = df2.format(millis);
					
					//Create a String with the time, formatted to m:ss.mmm, thanks to the 
					//Decimal Formats above.
					String result = minutes + ":" + seconds + "." + milliseconds;
					
					//Create a 2D array to store String objects in preparation
					String[][] leaderboardResults = new String[3][3];
					
					if(difficultyLevel.equals("Easy")) {
						//Assign the 2D array a value from the easyListAdd method, passing the 
						//second element of the messageCheck String array, along with the int 
						//guess and the String result as parameters.
						leaderboardResults = easyListAdd(messageCheck[1],guess,result);
					}
					else if(difficultyLevel.equals("Medium")) {
						//Same as above, but calls the mediumListAdd method instead
						leaderboardResults = mediumListAdd(messageCheck[1],guess,result);
					}
					else if(difficultyLevel.equals("Hard")) {
						//Same as above, but calls the hardListAdd method instead
						leaderboardResults = hardListAdd(messageCheck[1],guess,result);
					}
					//Use the Object Output Stream to send the 2D array to the client
					objOut.writeObject(leaderboardResults);
					
					break;
				}
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}catch(NullPointerException npe) {
			if(!client.isConnected()) {
				try {
					client.close();
				}catch(Exception e) {
				}
			}
		}
	}
	
	//In a situation where MongoDB is used, the following methods could be changed to add Player
	//objects to the database, instead of sending off parameters to different methods in the Server
	//class to add them to different ArrayLists.
	private String[][] easyListAdd(String name, int guess, String time) {
		return Server.easyAddToList(name,guess,time);
	}
	
	private String[][] mediumListAdd(String name, int guess, String time) {
		return Server.mediumAddToList(name,guess,time);
	}
	
	private String[][] hardListAdd(String name, int guess, String time) {
		return Server.hardAddToList(name,guess,time);
	}
}