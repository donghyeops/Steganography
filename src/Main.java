import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		String MP3FilePath = "sample2.mp3";
		String recordFilePath = "record2.mp3";
		String SteganoFilePath = "output.mp3";
		
		Stegano stegano = new Stegano();
		
		if (!stegano.loadMP3(MP3FilePath)) {
			System.err.println("fail loadMP3");
			return;
		}
		if (!stegano.loadRecord(recordFilePath)) {
			System.err.println("fail loadRecord");
			return;
		}
		if (!stegano.applyStegano()) {
			System.err.println("fail applyStegano");
			return;
		}
		if (!stegano.saveStegano(SteganoFilePath)) {
			System.err.println("fail saveStegano");
			return;
		}
		System.out.println("\n === 성공 !!! ===");
		System.out.println("바구니 파일 : " + MP3FilePath);
		System.out.println("음성 파일 : " + recordFilePath);
		System.out.println("결과 파일 : " + SteganoFilePath);
		
		System.out.println("\n === 추출 시작 ===");
		
		BackStegano backStegano = new BackStegano();
		if (!backStegano.loadMP3(SteganoFilePath)) {
			System.err.println("fail backStegano loadMP3");
			return;
		}
		if (!backStegano.getRecord()) {
			System.err.println("fail backStegano applyStegano");
			return;
		}
		if (!backStegano.saveRecord("추출된 파일.mp3")) {
			System.err.println("fail backStegano applyStegano");
			return;
		}
		
		System.out.println("\n === 추출 성공 !!! ===");
	}
}
