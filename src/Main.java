import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		String soundFileName = "sample4.mp3";
		Stegano stegano = new Stegano(soundFileName);
		stegano.encryption();
	}
}
