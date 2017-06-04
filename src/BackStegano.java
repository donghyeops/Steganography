import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * 1. boolean loadMP3(String MP3FilePath) : MP3 파일 불러오기 <br>
 * 2. boolean getRecord() : 녹음 파일 추출 <br>
 * 3. boolean saveRecord(String RecordFilePath) : 추출된 녹음파일 저장
 */

public class BackStegano {
	String soundFileName = null;
	byte[] MP3bytes = null; // MP3 파일
	Vector<Integer> Recordbits = new Vector<Integer>(); // 녹음 파일 (bit 단위로 저장)
	// byte[] Steganobytes = null; // 결과 파일

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

	/** 3. 추출된 녹음파일 저장 */
	public boolean saveRecord(String RecordFilePath) {
		FileOutputStream RecordOutputStream = null; // 녹음 파일 outputStream

		if (Recordbits.size() == 0) {
			System.out.println("녹음 파일 생성 안됨");
			return false;
		}
		Vector<Byte> vecRecordbytes = new Vector<Byte>(); // 입력을 위해 byte로 변환
		//

		byte inputByte = 0;
		int bitCount = 0;
		System.out.println("레코드 비트 사이즈 : " + Recordbits.size());
		int endCount = 0; // 1이 연속으로 80개면 끝난거임 
		for (int i = 48; i < Recordbits.size(); i++) { // 변환
			if (Recordbits.get(i) == 1) { // 비트 확인하고 버퍼에 값 삽입
				inputByte += Math.pow(2, 7 - bitCount % 8);
				endCount++;
			} else
				endCount=0;
			if (endCount == 75)
				break;
			bitCount++; // 현재 버퍼에 삽입된 비트 수

			if (bitCount % 8 == 0 && bitCount != 0) { // 8개 읽으면 바이트 삽입, 버퍼초기화
				vecRecordbytes.add(inputByte);
				inputByte = 0; // 버퍼 초기화
			}
		}

		byte[] Recordbytes = new byte[vecRecordbytes.size()];
		for (int i = 0; i < Recordbytes.length - endCount/8; i++)
			Recordbytes[i] = vecRecordbytes.get(i);

		File RecordFile = new File(RecordFilePath);
		try {
			RecordOutputStream = new FileOutputStream(RecordFile);
		} catch (FileNotFoundException e1) {
			return false;
		}
		try {
			RecordOutputStream.write(Recordbytes);
		} catch (IOException e) { // TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	/** 2. 녹음 파일 추출 */
	public boolean getRecord() {
		if (MP3bytes == null) {
			System.out.println("선택된 MP3 파일 없음");
			return false;
		}
		int startPoint = findStartPoint();

		Vector<AAU> AAUs = new Vector<AAU>();

		initAAUs(AAUs, startPoint);

		int count = 0; // 프레임 카운트
		System.out.println("AAU 수 : " + AAUs.size());

		for (AAU aau : AAUs) {
			count++;
			if (count < 5) // 초기 블록은 제외
				continue;
			/** 헤더 수정 */
			{
				{ /** Priv bit 수정 [1 bit] */
					if ((MP3bytes[aau.sp + 2] & 1) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
				{ /** Copyright 수정 [1 bit] */
					if ((MP3bytes[aau.sp + 3] & 8) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
				{ /** Original 수정 [1 bit] */
					if ((MP3bytes[aau.sp + 3] & 4) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
				{ /** Emphasis 수정 [2 bits] */
					if ((MP3bytes[aau.sp + 3] & 2) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
					if ((MP3bytes[aau.sp + 3] & 1) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
			}
			/** 사이드 인포 수정 */
			{
				{ /** private_bits, scfsi 수정 */
					if (aau.singleMode == false) { // 듀얼 채널
						for (int j = 64; j > 2; j /= 2) {
							// 원래 j>8이 정성임. 근데 실험 결과 2번(scfsi) 더 수정해도 이상이 없음
							if ((MP3bytes[aau.sidePoint + 1] & j) > 0)
								Recordbits.add(1);
							else
								Recordbits.add(0);
						}
						for (int j = 128; j > 32; j /= 2) {
							// 두번 째 scfsi 수정
							if ((MP3bytes[aau.sidePoint + 2] & j) > 0)
								Recordbits.add(1);
							else
								Recordbits.add(0);
						}
					} else { // 싱글 채널
						for (int j = 64; j > 2; j /= 2) {
							if ((MP3bytes[aau.sidePoint + 1] & j) > 0)
								Recordbits.add(1);
							else
								Recordbits.add(0);
						}
					}
				}
			}
			{ /** 마지막 비트 수정 (2 bits) */
				for (int j = 2; j > 0; j /= 2) {
					if ((MP3bytes[aau.sdp - 1] & j) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
			}
		}

		// 위조 TAG 시작 위치 찾기
		String tagPointString = "";
		for (int p2 = MP3bytes.length - 1;; p2--) {
			if (MP3bytes[p2] == 'E')
				break;
			tagPointString += String.valueOf(MP3bytes[p2]);
		}
		int tagPoint = Integer.parseInt(tagPointString, 2);

		try {
			byte temp;
			for (int i = tagPoint - 1; i < MP3bytes.length; i++) {
				temp = MP3bytes[i];

				if (i + 2 < MP3bytes.length && temp == 'T' && MP3bytes[i + 1] == 'A' && MP3bytes[i + 2] == 'G') {
					i += 2;
					continue;
				}
				for (int j = 128; j > 0; j /= 2) {
					if ((temp & j) > 0)
						Recordbits.add(1);
					else
						Recordbits.add(0);
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("추출 끝");
		}

		for (int i = 0; i < 24; i++) {
			if (Recordbits.get(i) == 0)
				return false;
		}
		for (int i = 24; i < 48; i++) {
			if (Recordbits.get(i) == 1)
				return false;
		}
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

		// 위조 TAG 시작 위치 찾기
		int p2;
		for (p2 = MP3bytes.length - 1;; p2--) {
			if (MP3bytes[p2] == 'E')
				break;
		}

		// 마지막 수정 AAU 시작 위치 찾기
		String endPointString = "";
		for (int p3 = p2 - 1;; p3--) {
			if (MP3bytes[p3] == 'E')
				break;
			endPointString += String.valueOf(MP3bytes[p3]);
		}
		int endPoint = Integer.parseInt(endPointString, 2);

		while (p < endPoint + 1) {
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
			// bps = (MP3bytes[p+2] & 16) + (MP3bytes[p+2] & 32) +
			// (MP3bytes[p+2] & 64) +
			// (MP3bytes[p+2] & 128);
			bps = bpsTable[bps] * 1000;
			if (bps == 0) {
				System.out.println("비트율 에러 : " + bps);
				System.out.println("에러 위치 : " + (p + 2));
				return;
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

			// 사이드인포 길이까지 구해둠
			// 계속이어서 하믄됨
			// ===========
			// System.out.println("frequency : " + frequency);
			// System.out.println("bps : " + bps);
			// System.out.println("AAUSize : " + AAUSize);
			// System.out.println("sideSize : " + sideSize);

			aau.ep = aau.sp + AAUSize - 1;
			p = aau.ep + 1;
			aau.sdp = aau.sp + sideSize + 4;
			aau.sidePoint = aau.sp + 4; // 사이드인포 시작점 계산
			if (aau.CRC == true) {
				aau.sidePoint += 2;
				aau.sdp += 2;
			}

			AAUs.addElement(aau);
			// System.out.println("(Hex) sp : " + Integer.toHexString(aau.sp));
			// System.out.println("(Hex) sdp : " +
			// Integer.toHexString(aau.sdp));
			// System.out.println("(Hex) ep : " + Integer.toHexString(aau.ep));
		}
	}
}
