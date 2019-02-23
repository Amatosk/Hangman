package Client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Optional;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class Hangman extends Application{
	
	//A Socket is prepared for use later
	static Socket client;
	
	//A PrintWriter, BufferedReader and ObjectInputStream are also prepared ahead of time
	static PrintWriter out;
	static BufferedReader in;
	static ObjectInputStream objIn;
	
	//Several types of variables are prepared here, as they need to be at a scope that can be
	//accessed by anonymous inner classes as well as methods in this class
	static long epoch; //Start time when the player begins the game.
	static long completeTime;//End time, once the player has completed the word.

	Label statusMessageLabel;//A Label whose text value will be adjusted many times through the game
							 //in order to inform the player of their success or failure as they progress
	
	Scene scene; //A scene, using a Group and some parameters to determine the size of the window and what
				 //should be displayed.
	Group group; //The main group, used to store various kinds of UI nodes, including labels and buttons.
	Group newGroup; //A second group that is needed during the game, as the hanged man is formed from 
					//incorrect guesses. Is added on top of the initial group.
	int difficultyGuesses = 0;//Using the difficulty setting, stores the maximum amount of guesses that the 
							  //player has.
	boolean finished;//Boolean checking whether the word has been finished or not. Changed by a value sent
					 //by the server after each player guess.
	char[] currentGuesses;//Letters that have been guessed so far by the player.
	Label currentGuessesLabel;//A label for currentGuesses to appear on screen
	Label currentWordLabel;//A label for the current state of the word. For instance, it may display "P__ro".
	Label attemptsRemainingLabel;//A label informing the user of how many guesses they have left before the hanged
								 //man is complete
	HBox finalLine;//A HBox storing the currentGuesses and attemptsRemaining labels, along with a Region object
				   //used for spacing. 
	VBox massiveSpacing;//A VBox used to contain other labels and the support used for the hanged man visual.
	Region eh = new Region();//A Region object used in the finalLine HBox to affect spacing
	int remainingGuesses; //Stores how many guesses the player has left before the hanged man is completed.
	double drawnPieces;//Amount of hangman pieces to draw on screen. 
	int count = 0;//Used on the victory screen to determine where the player's input should go in the inputName
				  //array when deciding their name
	Label endStatement3;//Used on the victory screen to display the name that the user is inputting.
	char[] inputName = {'_', '_', '_'};//Default value for the inputName. Three underscores, to help indicate to
									//the player that they have three characters to use for their scoreboard name.
		
	public static void main(String[] args){
		
		try{
			//Socket attempts to connect to the same computer, localhost, using the port number 4500
			client = new Socket("localhost", 4500);
			
			//A PrintWriter is connected to the OutputStream of the client Socket. 
			//The boolean determines whether it should auto-flush or not.
			out = new PrintWriter(client.getOutputStream(),true);
			
			//The BufferedReader creates a Buffer for the InputStreamReader, which in turn
			//reads from the InputStream of the client Socket. 
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			//The ObjectInputStream is able to read objects from the Socket
			objIn = new ObjectInputStream(client.getInputStream());
			

		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			
		}catch(ConnectException ce) {
			System.out.println("Could not connect to the server. Please ensure that the server is active before starting the game.");
			System.exit(1);
			
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		
		//JavaFX requires that this method is called, with args as a parameter, in order to start creating windows
		launch(args);
	}
	
	public void start(Stage stage) {
		
		//A label explaining to the player what they should do. MinWidth and Alignment help to determine
		//the placement and size of the label on the screen.
		Label chooseADifficulty = new Label("Please choose your difficulty preference");
		chooseADifficulty.setMinWidth(300);
		chooseADifficulty.setAlignment(Pos.CENTER);
		
		//A combobox containing Strings, which acts as a drop down menu for the player to use. Easy, Medium
		//and Hard are the options available. getSelectionModel().selectFirst() automatically selects the
		//first option on the drop down menu, preventing Null Pointer Exceptions for if the player tried to
		//continue without selecting a difficulty.
		ComboBox<String> difficultyChoices = new ComboBox<String>();
		difficultyChoices.getItems().add("Easy");
		difficultyChoices.getItems().add("Medium");
		difficultyChoices.getItems().add("Hard");
		difficultyChoices.getSelectionModel().selectFirst();
		
		//A confirmation button that the player can use to progress from this screen.
		Button difficultyConfirm = new Button("Confirm");
		
		//VBox places the nodes within itself one atop the other
		VBox difficultyVBox = new VBox(20,chooseADifficulty, difficultyChoices, difficultyConfirm);
		difficultyVBox.setAlignment(Pos.CENTER);
		
		//The Group object is now set to use the VBox
		group = new Group(difficultyVBox);
		
		//The group is now prepared on the scene, with a window size of 300 by 300, with a background colour
		//of light grey. The scene is placed on the stage. The window cannot be resized and the title is set 
		//to Hangman. It is then displayed to the player.
		scene = new Scene(group, 300,300, Color.LIGHTGREY);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Hangman");
		stage.show();
		
		//The drawnPieces double variable is set to 0.0, the statusMessageLabel variable has its value 
		//removed and the finished boolean is set back to false, acting as a reset for each time the 
		//player is returned to this part of the code, such as when they wish to start a new game.
		drawnPieces = 0.0;
		statusMessageLabel = new Label();
		finished = false;

		//An eventHandler is created for the confirmation button created earlier
		difficultyConfirm.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent e){
				//Clicking the button causes an Alert window to appear on the screen. It is stylised to
				//appear as a confirmation screen, as opposed to a warning or error screen. It uses the
				//selected option of the drop down menu to determine the message that is displayed to
				//the player.
				Alert confirmation = new Alert(AlertType.CONFIRMATION);
				confirmation.setTitle("Start game?");
				confirmation.setHeaderText("Do you wish to start the game on " + difficultyChoices.getValue() + " difficulty?");

				//The Alert window has two buttons, an OK button and a cancel button. This variable is
				//created to store which button was clicked on by the player.
				Optional<ButtonType> result = confirmation.showAndWait();
				
				//If the player clicked on the OK button, this if statement is used.
				if(result.get() == ButtonType.OK) {

					//A switch statement checks the choice selected from the drop down menu
					switch(difficultyChoices.getValue()) {
					//Depending on which difficulty was chosen, the difficultyGuesses int variable is
					//set to the maximum amount of guesses that the player is allowed, and debug messages
					//are displayed on the console. If the player was somehow able to reach this point
					//without selecting one of the options, the program closes, to prevent any issues later.
					case "Easy"   : difficultyGuesses = 7;
									System.out.println("Picked Easy mode");
									break;
					case "Medium" : difficultyGuesses = 5;
									System.out.println("Picked Medium mode");
									break;
					case "Hard"   : difficultyGuesses = 3;
									System.out.println("Picked Hard mode");
									break;
					default 	  : System.out.println("For some reason, we didn't get a difficulty setting.");
									System.exit(1);
									break;
					}
					
					//Using the PrintWriter, a message is sent to the server. StartUpGame acts like a 
					//keyword so that the server can access the right area of the code to use the 
					//second parameter correctly.
					out.println("StartUpGame " + difficultyGuesses);
					
					//A boolean is created and set to false. It is used as a crude way of waiting for a
					//response from the server. The while loop afterwards continues for as long as the
					//boolean remains false. When the correct message is received from the server, it
					//will set the boolean to true, which will end the while loop and continue with the
					//code afterwards. 
					boolean connectionCheck = false;
					while(!connectionCheck) {						
						try {
							connectionCheck = Boolean.parseBoolean(in.readLine());
						}catch(IOException ioe) {
							//If nothing is received, do nothing. The loop will continue as the boolean
							//remains false. May need to be refined in some way to determine whether no
							//message was received because the server's message hasn't arrived yet, or
							//because the connection has been completely lost and act accordingly.
						}
					}
					
					//The Alert window can now be closed
					confirmation.close();
					
					//Call the game method, passing over the stage so that it can be redesigned
					game(stage);
				}
			}
		});		
	}
	
	private void game(Stage stage) {
		
		//Now that the game has started, the current time in milliseconds is saved to the epoch
		//long variable. This variable will be compared to the current time in milliseconds when
		//the game has been successfully completed to determine how long the player took to win.
		epoch = System.currentTimeMillis();
		
		//Instructions are made as a Label to display on the screen to the player.
		Label instructions = new Label("Please type a letter you would like to try");
		instructions.setMinWidth(300);
		instructions.setAlignment(Pos.TOP_CENTER);
		//A char array is prepared, with a length equal to the length of the word that the player
		//should be searching for. The wordToFind method is called to retreive the word from the
		//server and the length method is called on that result to determine the length that the
		//array should be. The wordInProgressLabel is also prepared, clearing any value it may 
		//have held from a previous game.
		char[] wordInProgress = new char[wordToFind().length()];
		Label wordInProgressLabel = new Label();		
		
		//Lines are created to draw the support for the hanged man. 
		Line ground = new Line(200,230,250,230);
		Line upright = new Line(250,230,250,30);
		Line cross = new Line(150,30,250,30);
		Group noose = new Group(ground,upright,cross);

		//CurrentGuesses is a char array that stores all the characters that the player has attempted.
		//As such, the length of it should be, at most, the length of the word plus the amount of wrong
		//guesses that the player is capable of, determined by the difficulty they chose. The label for
		//it is also wiped clear from the value it may have had from a previous game. 
		currentGuesses = new char[wordInProgress.length + difficultyGuesses];
		currentGuessesLabel = new Label();
		
		//To start the game off, the amount of remaining guesses that the player has should match the
		//maximum amount of incorrect guesses that they are allowed. A label is prepared using the
		//value given and is aligned to the left, since it shares the same line as the characters that
		//the player has guessed.
		remainingGuesses = difficultyGuesses;
		attemptsRemainingLabel = new Label("Attempts remaining: " + remainingGuesses);
		attemptsRemainingLabel.setAlignment(Pos.CENTER_LEFT);
		
		//A group is created for the instructions that will appear at the top of the screen, the word in
		//progress that will appear below the hanged man, and the support part to the side of the hanged man.
		//The finalLine is one final HBox that is created for the attempted letters of the player and the
		//amount of guesses that the player has remaining, along with a Region node to space them out.
		group = new Group(instructions, wordInProgressLabel,noose);
		finalLine = new HBox(5,currentGuessesLabel,eh,attemptsRemainingLabel);
		HBox.setHgrow(eh,Priority.ALWAYS);
		finalLine.setAlignment(Pos.BOTTOM_LEFT);
		
		//MassiveSpacing is a VBox used to store the group created a moment ago, along with the statusMessage,
		// which explains whether the player was successful with their guess or not, the currentWordLabel, which
		//displays how much of the word has been correctly guessed so far and the finalLine, combining the player's
		//guesses and the amount of guesses they have left.
		massiveSpacing = new VBox(5,group,statusMessageLabel,currentWordLabel,finalLine);
		massiveSpacing.setAlignment(Pos.CENTER);
		
		//This final group is created out of the above VBox and a Group generated from the method call 'hangTheMan',
		//which passes the drawnPieces double variable, rounded to the nearest whole number and converted to an int.
		Group newGroup = new Group(massiveSpacing,hangTheMan(Math.toIntExact(Math.round(drawnPieces))));

		//The scene is recreated with the group created just above. 
		scene = new Scene(newGroup,300,300,Color.LIGHTGREY);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Hangman");

		//The keyRel method is called, with the stage being passed as a parameter. It adds the Key Released
		//event listener to the stage and is done this way because the event listener needs to be readded to
		//the stage each time one of the labels is updated from the player's actions and the scene is recreated
		//to show that updated label. 
		keyRel(stage);
		stage.show();
	}
	
	//A method that is called each time the different parts of the hanged man need to be worked out and
	//displayed to the player. The int parameter used is the piecesDrawn double rounded and converted to
	//an int, and is used to determine how many parts of the hanged man should be drawn to the screen.
	private Group hangTheMan(int wrong) {
		
		//Rope - Line
		Line rope = new Line(150,30,150,55);
		//Head - Circle
		Circle head = new Circle(150,75,20);
		head.setFill(Color.BLUE);
		//Body - Line
		Line body = new Line(150,95,150,150);
		//Left arm - Line
		Line leftArm = new Line(150,105,120,115);
		//Right arm - Line
		Line rightArm = new Line(150,105,180,115);
		//left leg - Line
		Line leftLeg = new Line(150,150,130,180);
		//Right leg - Line
		Line rightLeg = new Line(150,150,170,180);
		
		//A Group array, containing one group for each part of the hanged man. 
		Group[] hangedMan = {new Group(rope),new Group(head),new Group(body),new Group(leftArm),
							new Group(rightArm),new Group(leftLeg),new Group(rightLeg)};
		
		//This toReturn group is made in preparation for the for loop that happens next and
		//is the group that is returned from this method.
		Group toReturn = new Group();
		
		//This loop happens as often as the int parameter passed to this method. For each time
		//the loop happens, the next element of the Group array is added to the toReturn group.
		for(int i = 0; i < wrong; i++) {
			toReturn.getChildren().add(hangedMan[i]);
		}
		
		return toReturn;
	}
	
	//This method is used in the game method to add a Key Released event handler to the scene.
	//Each time a node on the scene is updated, thanks to the player's actions, the scene needs
	//to be recreated to reflect the update, but each time an update happens, the event handler
	//needs to be added once again. 
	private void keyRel(Stage stage) {
		
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			
			@Override
			public void handle(KeyEvent e) {
				
				//If the input from the player was a letter key, we continue inside this if statement.
				if(e.getCode().isLetterKey()) {
					
					//The input is converted to a char variable and a boolean is created and set to false,
					//in preparation for later in this method.
					char letter = e.getText().charAt(0);
					boolean same = false;
					
					for(int i = 0; i < currentGuesses.length; i++) {
						
						//If the letter that the user input is the same as the letter found
						//in the currentGuesses char array, the same boolean variable is set
						//to true and we break out of the loop.
						if(letter==currentGuesses[i]) {
							same = true;
							break;
							
						//Otherwise, if we reached a point in the array that has a value of
						//0, instead of a letter, we replace that 0 with the letter that the
						//user just input and break out of the loop, because we know everything
						//past this point are also blanks and don't want to replace them too.
						}else if(currentGuesses[i]==0) {
							currentGuesses[i] = letter;
							break;
						}
					}
					
					//If the letter isn't the same as one already guessed, this if statement happens.
					if(!same) {
						//A message is sent to the server, including the codename for the server to use
						//and the letter that the user input.
						out.println("LetterGuess " + letter);
						
						//A String array is created in preparation for the loop that happens 
						//immediately afterwards.
						String[] receive = new String[3];
					
						while(receive[0] == null) {
						
							try {
								//When the server responds, the String is split into the array, separating 
								//one word from the next by looking for a space.
								receive = in.readLine().split(" ");
							}catch(IOException ioe) {
								
							}
						}
						
						//The boolean success is set to the second element of the received String that is converted
						//to a boolean. The same is done with the third element and is saved to the finished boolean
						//that exists in the class scope.
						boolean success = Boolean.parseBoolean(receive[1]);
						finished = Boolean.parseBoolean(receive[2]);
						
						//Success only needs to exist within this method as it is used here to determine how
						//the statusMessageLabel should be updated.
						if(success) {
							//If the player made a successful guess, the status message is set accordingly.
							statusMessageLabel = new Label("There is a '" + letter + "' in the word!");
						}else {
							//However, if the player made an unsuccessful guess, the message is set differently,
							//the amount of remaining guesses the player has decrements and the drawnPieces
							//variable goes up by an amount determined by maths, thanks to the different difficulty
							//settings. Assuming easy difficulty, drawnPieces goes up by 1. Medium difficulty
							//makes it go up by 1.4 and hard difficulty makes it go up by 2.333. 
							statusMessageLabel = new Label("There was no '" + letter + "' in the word.");
							remainingGuesses--;
							drawnPieces += (7.0/difficultyGuesses);
						}
						//The currentWord label is now set to the value of the first element of the 
						//String array.
						currentWordLabel = new Label(receive[0]);
						
					//If the letter is the same as one already guessed, we go here instead
					}else {
						//Inform the user that they already tried their letter
						statusMessageLabel = new Label("The letter '" + letter + "' has already been used");
					}
					
					//The currentGuesses label is updated with the new values of the player's guessed letters.
					//It is converted from a char array to a String, so that it can be used for the label.
					//The attemptsRemaining label is also updated, as the value of the remainingGuesses int
					//may have changed, if the player guessed incorrectly. 
					currentGuessesLabel = new Label(new String(currentGuesses));
					attemptsRemainingLabel = new Label("Attempts remaining: " + remainingGuesses);

					//Since the labels have been updated, the finalLine HBox needs to be updated with the
					//new labels. 
					finalLine = new HBox(5,currentGuessesLabel,eh,attemptsRemainingLabel);
					HBox.setHgrow(eh,Priority.ALWAYS);
					finalLine.setAlignment(Pos.CENTER_LEFT);
					
					//And since the HBox has been updated, so too does the VBox need to be updated.
					massiveSpacing = new VBox(5,group,statusMessageLabel,currentWordLabel,finalLine);
					massiveSpacing.setAlignment(Pos.CENTER);
					
					//And also, the second group. 
					newGroup = new Group(massiveSpacing,hangTheMan(Math.toIntExact(Math.round(drawnPieces))));
					scene = new Scene(newGroup,300,300,Color.LIGHTGREY);
					stage.setScene(scene);
					
					//This method call could be seen as recursive, as it adds the very event listener
					//that we're inside now to the stage once again.
					keyRel(stage);
					
					//If the remaining guesses left for the player is 0, then it's game over for them.
					//The appropriate method is called.
					if(remainingGuesses==0) {
						gameOver(stage);
					}
					
					//Finished will be true if the word has been successfully completed. 
					if(finished) {
						
						//The completeTime long variable is the difference in values from the current time
						//in milliseconds and the value of epoch, which was the current time in milliseconds
						//when the game started.
						completeTime = (System.currentTimeMillis() - epoch);
						
						//In case the player had already played a game before, the values of count and
						//the inputName char array are set to a default value. 
						count = 0;
						inputName[0] = '_';
						inputName[1] = '_';
						inputName[2] = '_';
						
						//The appropriate method is called to progress on from the game.
						gameVictory(stage);
					}
					
				//However, if the player did not click on a letter key and it was instead the escape button,
				//this if statement is used instead
				}else if(e.getCode()==KeyCode.ESCAPE) {
					
					//An Alert window is made, asking the player whether they're sure they want to quit
					Alert alert = new Alert(AlertType.CONFIRMATION);
					alert.setTitle("Quit game");
					alert.setHeaderText(null);
					alert.setContentText("Are you sure you wish to quit the game?\nYour progress will not be saved.");
					Optional<ButtonType> result = alert.showAndWait();
					
					if(result.get() == ButtonType.OK) {
						//If the player clicks on the OK button, an attempt to close the socket is made
						try {
							client.close();
						}catch(IOException ioe) {
						}
						//and the stage is closed, which also ends the client's side of the program.
						stage.close();
					}	
				}	
			}
		});
	}
	
	//A method that is called when the player has ran out of guesses during the game
	private void gameOver(Stage stage) {
		
		//A specific label is created, coloured red and is scaled to be 2.5 times its usual size,
		//to emphasise the state of the game.
		Label gameOver = new Label("Game over");
		gameOver.setTextFill(Color.RED);
		gameOver.setScaleX(2.5);
		gameOver.setScaleY(2.5);
		gameOver.setMinWidth(300);
		gameOver.setAlignment(Pos.BASELINE_CENTER);
		
		//End statement labels are created to explain to the player why the game has ended and
		//what they can do next.
		Label endStatement1 = new Label("You have run out of tries");
		endStatement1.setMinWidth(300);
		endStatement1.setAlignment(Pos.CENTER);
		Label endStatement2 = new Label("Press any key to start a new game or escape to exit.");
		endStatement2.setWrapText(true);
		endStatement2.setPrefWidth(200);
		endStatement2.setMinWidth(200);
		endStatement2.setMaxWidth(200);
		endStatement2.setAlignment(Pos.BASELINE_CENTER);

		//These labels are put into a VBox, along with the Region object to help move the labels down
		//on the screen. The int determines how much space should be between each node in the VBox.
		VBox v = new VBox(20,eh,gameOver,endStatement1,endStatement2);
		v.setAlignment(Pos.TOP_CENTER);
		VBox.setVgrow(eh,Priority.ALWAYS);
		
		//The VBox is put into a group and the scene is then recreated with this group.
		group = new Group(v);
		scene = new Scene(group,300,300,Color.LIGHTGREY);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Hangman");
		stage.show();
		
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			
			@Override
			public void handle(KeyEvent e) {
				
				//If the player presses the escape button, the socket and stage are closed, ending
				//the client program.
				if(e.getCode()==KeyCode.ESCAPE) {
					try {
						client.close();
					}catch(IOException ioe) {
					}
					stage.close();
				//However, if the player has pressed any other button, the start method is called in
				//order to start a new game. It sets up the window once again and resets certain values
				//so that the game can behave normally.
				}else {
					start(stage);
				}
			}
		});
	}
	
	//This method is called when the word has been finished.
	private void gameVictory(Stage stage) {

		//Because of how the player can input characters to decide on a name, the scene
		//will be constantly updated as the name appears on screen. As such, a method
		//returns the group that is constantly changing as the player inputs more characters.
		scene = new Scene(victoryScreenLayout(),300,300,Color.LIGHTGREY);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Hangman");
		stage.show();
 		
		//This event handler manages the input for deciding on the player's name.
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			
			@Override
			public void handle(KeyEvent e) {
				
				//If the player presses the back space button, a check is done to
				//ensure that the count int variable is not less than 1. This is
				//to ensure that the part afterwards, changing the value of the char array,
				//does not go out of bounds. The value of count is then reduced by one,
				//which can result in it being 0, which is the first character of the name
				//or the first element of the char array.
				if(e.getCode()==KeyCode.BACK_SPACE) {
					if(count<1) {
						count = 1;
					}
					inputName[count-1] = '_';
					count--;
				}
				
				//If the player has input a letter, a check is done to ensure that the value
				//of count does not exceed 2. The element of the char array where count is, is
				//then changed to the value that the player has input and the value of count
				//goes up by one. 
				if(e.getCode().isLetterKey()) {
					if(count>2) {
						count = 2;
					}
					inputName[count] = e.getText().charAt(0);
					count++;
				}
				
				//Similarly to before, this method call from within exactly the same method
				//enables this very event handler to be added to the stage once again.
				gameVictory(stage);
				
				if(e.getCode()==KeyCode.ENTER) {

					//If the player has pressed enter however, the input name from the player
					//is saved to a String, the amount of incorrect guesses that the player
					//made is calculated and saved to an int and the calculated time of the game
					//worked out back in the keyRel method are sent, along with the two saved
					//variables to the server. 
					String name = endStatement3.getText();
					int incorGuess = difficultyGuesses - remainingGuesses;
					out.println("Victory " + name + " " + incorGuess + " " + completeTime);
					
					//A 2D String array is prepared to receive one from the server's response.
					String[][] leaderboardResults = new String[3][3];
					while(leaderboardResults[0][0]==null) {
						try {
							leaderboardResults = (String[][]) objIn.readObject();
						}catch(Exception ex) {
						}
					}
					
					//The stage and the 2D array are sent to the resultsScreen method, moving on
					//from this screen
					resultsScreen(stage,leaderboardResults);
				}
			}
		});	
	}
	
	//This method is reached only after the user has sent their name to the server after successfully
	//winning a game.
	private void resultsScreen(Stage stage, String[][] leaderboard) {
		
		//A HBox is prepared, with an int to determine spacing between the nodes within it later.
		HBox h = new HBox(30);
		
		//A VBox array of length 3 is prepared, as well as a Label array, with three values given
		//to it immediately. 
		VBox[] v = new VBox[3];
		Label[] title = {new Label("Name"), new Label("Incorrect Guesses"), new Label("Time")};
		
		//A separate VBox is prepared.
		VBox v2;
		
		//For loop adding all the names from the leaderboard array into one VBox, then
		//adding all the incorrectGuesses to a second VBox, then adding all the times
		//to the third VBox. By doing it this way, names and scores are much more better
		//aligned as columns than if each player and their scores were put into HBoxes.
		for(int i = 0; i < 3; i++) {
			v[i] = new VBox();
			v[i].getChildren().add(title[i]);
			
			for(int j = 0; j < 3; j++) {
				Label l = new Label(leaderboard[j][i]);
				v[i].getChildren().add(l);
			}
			h.getChildren().add(v[i]);
		}
		
		h.setAlignment(Pos.BASELINE_CENTER);
		
		//A label is now created and put into the second VBox, along with the HBox just created.
		//This results in the statement being placed below the entire leaderboard without issues.
		Label endStatement = new Label("Press any key to start a new game or escape to exit.");
		endStatement.setWrapText(true);
		endStatement.setPrefWidth(200);
		endStatement.setMinWidth(200);
		endStatement.setMaxWidth(200);

		v2 = new VBox(2,h,endStatement);
		v2.setAlignment(Pos.TOP_CENTER);
		group = new Group(v2);
		
		scene = new Scene(group,300,300,Color.LIGHTGREY);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Hangman");
		stage.show();
		
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
			
			@Override
			public void handle(KeyEvent e) {
				
				if(e.getCode()==KeyCode.ESCAPE) {
					try {
						client.close();
					}catch(IOException ioe) {
					}
					stage.close();
				}else {
					start(stage);
				}
			}
		});
	}
	
	//This method is called from the gameVictory method, determining the layout of the screen,
	//with all its labels and nodes, each time the player inputs a new character for their name. 
	private Group victoryScreenLayout() {
		
		Label gameWin = new Label("Congratulations!");
		gameWin.setTextFill(Color.GREEN);
		gameWin.setScaleX(2.5);
		gameWin.setScaleY(2.5);
		gameWin.setMinWidth(300);
		gameWin.setAlignment(Pos.BASELINE_CENTER);
		
		Label endStatement1 = new Label("The word was indeed " + currentWordLabel.getText() + "!");
		endStatement1.setMinWidth(300);
		endStatement1.setAlignment(Pos.CENTER);
		Label endStatement2 = new Label("Please input your name and press enter.");
		endStatement2.setMinWidth(300);
		endStatement2.setAlignment(Pos.BASELINE_CENTER);
		endStatement3 = new Label(new String(inputName));
		endStatement3.setMinWidth(300);
		endStatement3.setAlignment(Pos.BASELINE_CENTER);
		
		VBox v = new VBox(20,eh,gameWin,endStatement1,endStatement2,endStatement3);
		v.setAlignment(Pos.TOP_CENTER);
		VBox.setVgrow(eh,Priority.ALWAYS);
		
		return new Group(v);	
	}
	
	//This method is only called from the game method and is used to determine the length of
	//the char array that displays the word in progress to the player. 
	private String wordToFind() {
		
		//It actually just gets the incomplete version of the word, with underscores.
		//By the point this method is called, the server will have created a thread for
		//this client and generated a random word for this client to aim for. 
		//The underscore variant will be just as long as the real word, so it can be used
		//for array lengths and, since it'll be all underscores, even if a hacker went to
		//print out the value of temp, they wouldn't get anywhere.
		out.println("GetWord");
		String temp = null;
		while(true) {
			try {
				temp = in.readLine();
				if(temp!=null) {
					currentWordLabel = new Label(temp);
					return temp;
				}
			}catch(IOException ioe) {
			}
		}
	}
}