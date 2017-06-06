import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * 1. boolean loadMP3(String MP3FilePath) : MP3 파일 불러오기 <br>
 * 2. boolean loadRecord(String recordFilePath) : 녹음 파일 불러오기 <br>
 * 3. boolean applyStegano() : 스테가노그래피 적용 <br>
 * 4. boolean saveStegano(String SteganoFilePath) : 처리된 스테가노그래피 파일 저장
 */
public class Stegano {
	String soundFileName = null;
	byte[] MP3bytes = null; // MP3 파일
	Vector<Integer> Recordbits = new Vector<Integer>(); // 녹음 파일 (bit 단위로 저장)
	// byte[] Steganobytes = null; // 결과 파일
	Vector<Byte> Steganobytes = new Vector<Byte>(); // 결과 파일 (byte 단위로 저장)

	class AAU {
		int sp; // AAU 시작 바이트 위치
		int ep; // AAU 마지막 바이트 위치
		int sdp; // 소리 데이터 시작 바이트 위치
		int edp; // 소리 데이터 마지막 바이트 위치
		boolean CRC; // CRC 사용?
		boolean padding; // 패딩 사용?
		boolean singleMode; // 싱글모드인지
		int sidePoint; // 사이드인포 시작점
	}

	/** 1. MP3 파일 불러오기 */
	public boolean loadMP3(String MP3FilePath) {
		File MP3File = new File(MP3FilePath);
		MP3bytes = new byte[(int) MP3File.length()]; // 파일의 바이트 길이

		FileInputStream MP3InputStream = null; // MP3 inputStream
		try {
			MP3InputStream = new FileInputStream(MP3File);
		} catch (FileNotFoundException e) {
			System.out.println("파일 찾기 실패");
			return false;
		}
		try {
			MP3InputStream.read(MP3bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("파일 읽기 실패");
			return false;
		}
		System.out.println("읽기 성공");
		System.out.println("파일 크기 : " + MP3bytes.length / 1024 + " kb");
		return true;
	}

	/** 2. 녹음 파일 불러오기 */
	public boolean loadRecord(String recordFilePath) {
		FileInputStream recordInputStream = null; // 녹음파일 inputStream
		byte[] Recordbytes = null;
		File recordFile = new File(recordFilePath);
		Recordbytes = new byte[(int) recordFile.length()]; // 파일의 바이트 길이

		try {
			recordInputStream = new FileInputStream(recordFile);
		} catch (FileNotFoundException e) {
			System.out.println("파일 찾기 실패");
			return false;
		}
		try {
			recordInputStream.read(Recordbytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("파일 읽기 실패");
			return false;
		}
		System.out.println("읽기 성공");
		System.out.println("파일 크기 : " + Recordbytes.length / 1024 + " kb");

		// 위장 마킹 (1값 24개, 0값 24개)
		for (int i = 0; i < 24; i++)
			Recordbits.addElement(1);
		for (int i = 0; i < 24; i++)
			Recordbits.addElement(0);

		for (int i = 0; i < Recordbytes.length; i++) {
			for (int j = 128; j > 0; j /= 2) {
				if ((Recordbytes[i] & j) > 0)
					Recordbits.addElement(1);
				else
					Recordbits.addElement(0);
			}
		}
		
		// end 마킹 (0값 1개  + 1값 80개)
		Recordbits.addElement(0);
		for (int i = 0; i < 80; i++)
			Recordbits.addElement(1);
		return true;
	}

	/** 4. 처리된 스테가노그래피 파일 저장 */
	public boolean saveStegano(String SteganoFilePath) {
		FileOutputStream SteganoOutputStream = null; // 스테가노그래피 파일 outputStream

		if (Steganobytes.size() == 0) {
			System.out.println("스테가노그래피 적용 안됨");
			return false;
		}
		byte[] SteganoArr = new byte[(int) Steganobytes.size()]; // 배열로 변환
		for (int i = 0; i < SteganoArr.length; i++) // 변환
			SteganoArr[i] = Steganobytes.get(i);

		File SteganoFile = new File(SteganoFilePath);
		try {
			SteganoOutputStream = new FileOutputStream(SteganoFile);
		} catch (FileNotFoundException e1) {
			return false;
		}
		try {
			SteganoOutputStream.write(SteganoArr);
		} catch (IOException e) { // TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	/** 3. 스테가노그래피 적용 */
	public boolean applyStegano() {
		if (MP3bytes == null) {
			System.out.println("선택된 MP3 파일 없음");
			return false;
		}
		if (Recordbits.size() == 0) {
			System.out.println("선택된 MP3 파일 없음");
			return false;
		}
		int startPoint = findStartPoint();

		Vector<AAU> AAUs = new Vector<AAU>();

		initAAUs(AAUs, startPoint);

		int inputNumber = 0;
		int pointer = 0; // 녹음 파일의 비트 포인터
		int count = 0; // 프레임 카운트
		System.out.println("AAU 수 : " + AAUs.size());

		try {
			for (AAU aau : AAUs) {
				count++;
				if (count < 5) // 초기 블록은 제외
					continue;
				/** 헤더 수정 */
				{
					{ /** Priv bit 수정 [1 bit] */
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sp + 2] |= 1;
						else
							MP3bytes[aau.sp + 2] &= 255 - 1;
						inputNumber++;
					}
					{ /** Copyright 수정 [1 bit] */
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sp + 3] |= 8;
						else
							MP3bytes[aau.sp + 3] &= 255 - 8;
						inputNumber++;
					}
					{ /** Original 수정 [1 bit] */
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sp + 3] |= 4;
						else
							MP3bytes[aau.sp + 3] &= 255 - 4;
						inputNumber++;
					}
					{ /** Emphasis 수정 [2 bits] */
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sp + 3] |= 2;
						else
							MP3bytes[aau.sp + 3] &= 255 - 2;
						inputNumber++;
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sp + 3] |= 1;
						else
							MP3bytes[aau.sp + 3] &= 255 - 1;
						inputNumber++;
					}
				}
				/** 사이드 인포 수정 */
				{
					{ /** private_bits, scfsi 수정 */
						if (aau.singleMode == false) { // 듀얼 채널
							for (int j = 64; j > 8; j /= 2) {
								// 원래 j>8이 정성임. 근데 실험 결과 2번(scfsi) 더 수정해도 이상이 없음
								if (Recordbits.get(pointer++) == 1)
									MP3bytes[aau.sidePoint + 1] |= j;
								else
									MP3bytes[aau.sidePoint + 1] &= 255 - j;
								inputNumber++;
							}/*
							for (int j = 128; j > 32; j /= 2) {
								// 두번 째 scfsi 수정
								if (Recordbits.get(pointer++) == 1)
									MP3bytes[aau.sidePoint + 2] |= j;
								else
									MP3bytes[aau.sidePoint + 2] &= 255 - j;
								inputNumber++;
							}*/
						} else { // 싱글 채널
							for (int j = 64; j > 2; j /= 2) {
								if (Recordbits.get(pointer++) == 1)
									MP3bytes[aau.sidePoint + 1] |= j;
								else
									MP3bytes[aau.sidePoint + 1] &= 255 - j;
								inputNumber++;
							}
						}
					}
				}
				/*
				{ /** 마지막 비트 수정 (2 bits) *//*
					for (int j = 2; j > 0; j /= 2) {
						if (Recordbits.get(pointer++) == 1)
							MP3bytes[aau.sdp - 1] |= j;
						else
							MP3bytes[aau.sdp - 1] &= 255 - j;
						inputNumber++;
					}
				}*/
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("입력 끝");
		}

		for (int i = 0; i < MP3bytes.length - 1; i++) // 수정된 파일 복사
			Steganobytes.add(MP3bytes[i]);
		
		// 위장용 Tag 정보
		Steganobytes.add((byte) 'T');
		Steganobytes.add((byte) 'A');
		Steganobytes.add((byte) 'G');
		for (int i = 0; i < 125; i++) {
			Steganobytes.add((byte) 0);
		}

		// 여분 데이터 삽입
		int end = Recordbits.size() - pointer;
		byte inputByte = 0; // 입력 버퍼
		int bitCount = 0; // 현재 버퍼에 삽입된 비트 수
		for (int i = 0; i < end; i++) {
			if (bitCount % (125 * 8) == 0) { // 128바이트마다 TAG 삽입
				Steganobytes.add((byte) 'T');
				Steganobytes.add((byte) 'A');
				Steganobytes.add((byte) 'G');
			}
			if (Recordbits.get(pointer++) == 1) // 녹음 데이터의 비트 확인하고 버퍼에 값 삽입
				inputByte += Math.pow(2, 7 - bitCount % 8);
			bitCount++; // 현재 버퍼에 삽입된 비트 수

			if (bitCount % 8 == 0 && bitCount != 0) { // 8개 읽으면 바이트 삽입, 버퍼초기화
				Steganobytes.add(inputByte);
				inputByte = 0; // 버퍼 초기화
				inputNumber += 8;
			}
		}
		if ((inputByte != 0) && (bitCount % (125 * 8) == 0)) { // 데이터가 남아있는데,
																// 128바이트 모두 채웠을
																// 시 TAG 추가
			Steganobytes.add((byte) 'T');
			Steganobytes.add((byte) 'A');
			Steganobytes.add((byte) 'G');
		}
		char[] endPoint = (Integer.toBinaryString(AAUs.get(AAUs.size() - 1).sp)).toCharArray();
		Steganobytes.addElement((byte) 'E'); // 뒤에서부터 수정된 마지막 AAU 시작 위치 반환
		for (int i = endPoint.length - 1; i > -1; i--) {
			if (endPoint[i] == '1')
				Steganobytes.addElement((byte) 1);
			else
				Steganobytes.addElement((byte) 0);
		}

		char[] tagPoint = (Integer.toBinaryString(MP3bytes.length + 128)).toCharArray();
		Steganobytes.addElement((byte) 'E'); // 뒤에서부터 위조 TAG시작 위치 반환
		for (int i = tagPoint.length - 1; i > -1; i--) {
			if (tagPoint[i] == '1')
				Steganobytes.addElement((byte) 1);
			else
				Steganobytes.addElement((byte) 0);
		}

		System.out.println("처리 수 : " + (double) inputNumber / 8 / 1024 + " kb");
		System.out.println("처리 완료");

		return true;
	}

	/** ID3 v2 Tag를 분석하여 첫 AAU 시작점을 찾음 */
	private int findStartPoint() {
		int ID3_lengh = 0;

		if (MP3bytes[0] == 0x49 && MP3bytes[1] == 0x44 && MP3bytes[2] == 0x33) {
			System.out.println("== ID3 v2 Tag 확인 ==");

			int pos = 27;
			for (int i = 6; i < 10; i++) {
				int bitWise = 1 << 6;

				for (int j = 0; j < 7; j++) {
					if ((bitWise & MP3bytes[i]) > 0) {
						ID3_lengh += Math.pow(2, pos);
					}
					bitWise = bitWise >> 1;
					pos--;
				}
			}
		}
		return ID3_lengh + 0xA;
	}

	/** AAU들의 각 길이를 알아냄 */
	private void initAAUs(Vector<AAU> AAUs, int startPoint) {
		int p = startPoint;
		int[] bpsTable = { 0, 32, 40, 45, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0 };
		while (p < MP3bytes.length / 10 * 9) {
			AAU aau = new AAU();
			aau.sp = p;

			boolean padding = false;
			int AAUSize = 0;
			int frequency = 0;
			int bps = 0;
			int channelMode = 0; // 채널 모드
			int sideSize = 0; // sideInfo 사이즈
			// == Header ==
			// CRC 체크
			if ((MP3bytes[p + 1] & 1) == 0)
				aau.CRC = true;
			else
				aau.CRC = false;

			// 3번째 바이트
			// 패딩 체크
			if ((MP3bytes[p + 2] & 2) > 0)
				padding = true;

			// 주파수 계산
			frequency = (MP3bytes[p + 2] & 4) + (MP3bytes[p + 2] & 8);
			// System.out.println("주파수 : " + frequency);
			switch (frequency) {
			case 0:
				frequency = 44100;
				break;
			case 4:
				frequency = 48000;
				break;
			case 8:
				frequency = 32000;
				break;
			default:
				System.out.println("주파수 에러 : " + frequency);
				continue;
			}

			// 비트율 계산
			for (int i = 4; i < 8; i++) {
				if ((MP3bytes[p + 2] & (int) Math.pow(2, i)) > 0)
					bps += (int) Math.pow(2, i - 4);
			}

			bps = bpsTable[bps] * 1000;
			if (bps == 0) {
				System.out.println("비트율 에러 : " + bps);
				continue;
			}

			AAUSize = (int) (144 * bps / frequency); // AAU 길이 계산
			if (padding == true)
				AAUSize++;

			// 4번째 바이트 (마지막)
			// 채널 모드 계산
			for (int i = 6; i < 8; i++) {
				if ((MP3bytes[p + 3] & (int) Math.pow(2, i)) > 0)
					channelMode += (int) Math.pow(2, i - 6);
			}
			// 사이드 인포 길이 계산
			if (channelMode == 3) {
				sideSize = 17;
				aau.singleMode = true;
			} else {
				sideSize = 32;
				aau.singleMode = false;
			}
			
			aau.ep = aau.sp + AAUSize - 1;
			p = aau.ep + 1;
			aau.sdp = aau.sp + sideSize + 4;
			aau.sidePoint = aau.sp + 4; // 사이드인포 시작점 계산
			if (aau.CRC == true) {
				aau.sidePoint += 2;
				aau.sdp += 2;
			}

			AAUs.addElement(aau);
		}
	}
}
