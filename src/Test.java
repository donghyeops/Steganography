import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Test {
	public static void main(String[] args) {
		String MP3FilePath = "sample4.mp3";
		File MP3File = new File(MP3FilePath);
		byte[] MP3bytes = new byte[(int) MP3File.length()]; // 파일의 바이트 길이

		FileInputStream MP3InputStream = null; // MP3 inputStream
		try {
			MP3InputStream = new FileInputStream(MP3File);
		} catch (FileNotFoundException e) {
			System.out.println("파일 찾기 실패");
			return;
		}
		try {
			MP3InputStream.read(MP3bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("파일 읽기 실패");
			return;
		}
		System.out.println("읽기 성공");
		System.out.println("파일 크기 : " + MP3bytes.length / 1024 + " kb");

		byte[] newMP3 = new byte[MP3bytes.length + 128000];
		for (int i = 0; i < MP3bytes.length; i++) {
			newMP3[i] = MP3bytes[i];
		}
		newMP3[MP3bytes.length-1] = 0;
		for (int i = 0; i < 1000; i++) {
			//newMP3[MP3bytes.length + i*128] = (byte) 'T';
			//newMP3[MP3bytes.length + i*128+1] = (byte) 'A';
			//newMP3[MP3bytes.length + i*128+2] = (byte) 'G';
			newMP3[MP3bytes.length + i*128] = (byte) 0;
			newMP3[MP3bytes.length + i*128] = (byte) 0;
			newMP3[MP3bytes.length + i*128] = (byte) 0;
			for (int j = 0; j < 125; j++)
				newMP3[MP3bytes.length+ i*128 + 3 + j] = (byte) 0;
			//newMP3[MP3bytes.length + i*128+3+124] = (byte) 0xFF;
		}
		//newMP3[newMP3.length-1] = (byte)0xFF;
				
		FileOutputStream SteganoOutputStream = null; // 스테가노그래피 파일 outputStream

		File SteganoFile = new File("test.mp3");
		try {
			SteganoOutputStream = new FileOutputStream(SteganoFile);
		} catch (FileNotFoundException e1) {
			return;
		}
		try {
			SteganoOutputStream.write(newMP3);
		} catch (IOException e) { // TODO Auto-generated catch block
			return;
		}
	}
}
