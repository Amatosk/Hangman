package Server;

public class Player {

	String n;
	int iG;
	String t;
	
	public Player(String inputName, int incorrectGuesses, String time) {
		
		n = inputName;
		iG = incorrectGuesses;
		t = time;
	}
	
	public String toString() {
		return n + " " + iG + " " + t;
	}
	
	public String getName() {
		return n;
	}
	
	public int getIncorrectGuesses() {
		return iG;
	}
	
	public String getTime() {
		return t;
	}
}