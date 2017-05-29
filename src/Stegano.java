import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class Stegano {
	String soundFileName = null;
	byte[] sbyte = null;
	FileInputStream fileInputStream = null;
	FileOutputStream fileOutputStream = null;

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

	public Stegano(String soundFileName) {
		this.soundFileName = soundFileName;

		File file = new File(soundFileName);
		File fileOut = new File("처리된 " + soundFileName);
		sbyte = new byte[(int) file.length()]; // 파일의 바이트 길이

		try {
			fileInputStream = new FileInputStream(file);
			fileOutputStream = new FileOutputStream(fileOut);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fileInputStream.read(sbyte);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("읽기 실패");
			return;
		}
		System.out.println("읽기 성공");
		System.out.println("파일 크기 : " + sbyte.length / 1024 + " kb");
	}

	public void encryption() {
		int startPoint = findStartPoint();

		Vector<AAU> AAUs = new Vector<AAU>();

		initAAUs(AAUs, startPoint);

		int inputNumber = 0;
		int count = 0; // 프레임 카운트
		int byteCount = 0; // 몇 번 째 비트인지 카운트
		System.out.println("AAU 수 : " + AAUs.size());
		/*
		 * 비는 공간에 넣기 int end = AAUs.get(AAUs.size()-1).ep; for (int
		 * i=AAUs.get(0).sp; i<end; i++) { boolean isSame = true; for (int j=0;
		 * j<15; j++) { // 15개가 같으면 수정함 if (i+j == end-1) { isSame = false;
		 * break; } if (sbyte[i] != sbyte[i+j]) { isSame = false; break; } } if
		 * (isSame) { for (int j=0; j<15; j++) { sbyte[i+j] = 0x55; i+=5;
		 * inputNumber+=8; } } }
		 */
		for (AAU aau : AAUs) {
			count++;
			if (count < 5) // 초기 블록은 제외
				continue;
			/** 헤더 수정 */
			{
				{ /** Priv bit 수정 [1 bit] */
					if ((sbyte[aau.sp + 2] & 1) > 0)
						sbyte[aau.sp + 2]--;
					else
						sbyte[aau.sp + 2]++;
					inputNumber++;
				}
				{ /** Copyright 수정 [1 bit] */
					if ((sbyte[aau.sp + 3] & 8) > 0)
						sbyte[aau.sp + 3] -= 8;
					else
						sbyte[aau.sp + 3] += 8;
					inputNumber++;
				}
				{ /** Original 수정 [1 bit] */
					if ((sbyte[aau.sp + 3] & 4) > 0)
						sbyte[aau.sp + 3] -= 4;
					else
						sbyte[aau.sp + 3] += 4;
					inputNumber++;
				}
				{ /** Emphasis 수정 [2 bits] */
					if ((sbyte[aau.sp + 3] & 2) > 0)
						sbyte[aau.sp + 3] -= 2;
					else
						sbyte[aau.sp + 3] += 2;
					inputNumber++;
					if ((sbyte[aau.sp + 3] & 1) > 0)
						sbyte[aau.sp + 3] -= 1;
					else
						sbyte[aau.sp + 3] += 1;
					inputNumber++;
				}
			}
			/** 사이드 인포 수정 */
			{
				{ /** private_bits, scfsi 수정 */
					if (aau.singleMode == false) { // 듀얼 채널
						for (int j = 64; j > 2; j /= 2) {
							// 원래 j>8이 정성임. 근데 실험 결과 2번(scfsi) 더 수정해도 이상이 없음
							if ((sbyte[aau.sidePoint + 1] & j) > 0)
								sbyte[aau.sidePoint + 1] -= j;
							else
								sbyte[aau.sidePoint + 1] += j;
							inputNumber++;
						}
						for (int j = 128; j > 32; j /= 2) {
							// 두번 째 scfsi 수정
							if ((sbyte[aau.sidePoint + 2] & j) > 0)
								sbyte[aau.sidePoint + 2] -= j;
							else
								sbyte[aau.sidePoint + 2] += j;
							inputNumber++;
						}
					} else { // 싱글 채널
						for (int j = 64; j > 2; j /= 2) {
							if ((sbyte[aau.sidePoint + 1] & j) > 0)
								sbyte[aau.sidePoint + 1] -= j;
							else
								sbyte[aau.sidePoint + 1] += j;
							inputNumber++;
						}
					}
				}
			}
			{ /** 부수 데이터 수정 *//*
								 * int part2_3_length1 = 0; int part2_3_length2
								 * = 0; int part2_3_length3 = 0; int
								 * part2_3_length4 = 0; int head = aau.sdp; if
								 * (aau.singleMode == false) { // 듀얼 채널 (총 4개있음)
								 * // 1번 for (int j = 8; j > 0; j /= 2) if
								 * ((sbyte[aau.sidePoint + 2] & j) > 0)
								 * part2_3_length1 += j * Math.pow(2,8); for
								 * (int j = 128; j > 0; j /= 2) if
								 * ((sbyte[aau.sidePoint + 3] & j) > 0)
								 * part2_3_length1 += j; // 2번 for (int j = 1; j
								 * > 0; j /= 2) if ((sbyte[aau.sidePoint + 9] &
								 * j) > 0) part2_3_length2 += j *
								 * Math.pow(2,11); for (int j = 128; j > 0; j /=
								 * 2) if ((sbyte[aau.sidePoint + 10] & j) > 0)
								 * part2_3_length2 += j * Math.pow(2,3); for
								 * (int j = 128; j > 16; j /= 2) if
								 * ((sbyte[aau.sidePoint + 11] & j) > 0)
								 * part2_3_length2 += j; // 3번 for (int j = 32;
								 * j > 0; j /= 2) if ((sbyte[aau.sidePoint + 17]
								 * & j) > 0) part2_3_length3 += j *
								 * Math.pow(2,6); for (int j = 128; j > 2; j /=
								 * 2) if ((sbyte[aau.sidePoint + 18] & j) > 0)
								 * part2_3_length3 += j; // 4번 for (int j = 4; j
								 * > 0; j /= 2) if ((sbyte[aau.sidePoint + 24] &
								 * j) > 0) part2_3_length4 += j * Math.pow(2,9);
								 * for (int j = 128; j > 0; j /= 2) if
								 * ((sbyte[aau.sidePoint + 25] & j) > 0)
								 * part2_3_length4 += j * Math.pow(2,1); for
								 * (int j = 128; j > 64; j /= 2) if
								 * ((sbyte[aau.sidePoint + 26] & j) > 0)
								 * part2_3_length4 += j;
								 * 
								 * for (int j=head + part2_3_length1/8 + 1;
								 * j<head; j++) { sbyte[j] = 0; inputNumber +=
								 * 8; } } else { // 싱글 채널 // 1번 for (int j = 32;
								 * j > 0; j /= 2) if ((sbyte[aau.sidePoint + 2]
								 * & j) > 0) part2_3_length1 += j *
								 * Math.pow(2,6); for (int j = 128; j > 2; j /=
								 * 2) if ((sbyte[aau.sidePoint + 3] & j) > 0)
								 * part2_3_length1 += j; // 2번 for (int j = 4; j
								 * > 0; j /= 2) if ((sbyte[aau.sidePoint + 9] &
								 * j) > 0) part2_3_length2 += j * Math.pow(2,9);
								 * for (int j = 128; j > 0; j /= 2) if
								 * ((sbyte[aau.sidePoint + 10] & j) > 0)
								 * part2_3_length2 += j * Math.pow(2,1); for
								 * (int j = 128; j > 64; j /= 2) if
								 * ((sbyte[aau.sidePoint + 11] & j) > 0)
								 * part2_3_length2 += j; }
								 */
			}
			{ /** 마지막 비트 수정 (2 bits) */
				for (int j = 2; j > 0; j /= 2) {
					if ((sbyte[aau.sdp - 1] & j) > 0)
						sbyte[aau.sdp - 1] -= j;
					else
						sbyte[aau.sdp - 1] += j;
					inputNumber++;
				}
			}
			/** main data 수정 *//*
			for (int i = aau.sdp; i < aau.ep; i++) {
				byteCount++;
				if (byteCount % 50 == 0) {
					if ((sbyte[i] & 1) == 1)
						sbyte[i]--;
					else
						sbyte[i]++;
					inputNumber++;
				}
			}*/

			// sbyte[aau.sdp-1] = 0; for (int j=0; j<1; j++) { sbyte[aau.ep-j]
			// = (byte)0xFF; inputNumber += 8; } //inputNumber += 8;

			// sbyte[aau.ep] &= 0xFF;
		}

		
		
		byte[] newByte = new byte[sbyte.length + 12800];
		for (int i=0; i<sbyte.length; i++)
			newByte[i] =  sbyte[i];
		//newByte[sbyte.length-1] = 0;
		//newByte[sbyte.length] = 0;
		for (int i=sbyte.length; i<newByte.length; i+=128) {
			newByte[i] = 0x54;
			newByte[i+1] = 0x41;
			newByte[i+2] = 0x47;
			for (int j=i+3; j<i+128; j++) {
				newByte[j] = 0x77;
			}
			inputNumber += 8*125;
		}
		//newByte[newByte.length-1] = (byte) 0xFF;
		try
		{
			fileOutputStream.write(newByte);
		} catch (IOException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("처리 수 : " + (double) inputNumber / 8 / 1024 + " kb");
		System.out.println("처리 완료");
	}

	/** ID3 v2 Tag를 분석하여 첫 AAU 시작점을 찾음 */
	private int findStartPoint() {
		int ID3_lengh = 0;

		if (sbyte[0] == 0x49 && sbyte[1] == 0x44 && sbyte[2] == 0x33) {
			System.out.println("== ID3 v2 Tag 확인 ==");

			int pos = 27;
			for (int i = 6; i < 10; i++) {
				int bitWise = 1 << 6;

				for (int j = 0; j < 7; j++) {
					if ((bitWise & sbyte[i]) > 0) {
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
		while (p < sbyte.length / 10 * 9) {
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
			if ((sbyte[p + 1] & 1) == 0)
				aau.CRC = true;
			else
				aau.CRC = false;

			// 3번째 바이트
			// 패딩 체크
			if ((sbyte[p + 2] & 2) > 0)
				padding = true;

			// 주파수 계산
			frequency = (sbyte[p + 2] & 4) + (sbyte[p + 2] & 8);
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
				if ((sbyte[p + 2] & (int) Math.pow(2, i)) > 0)
					bps += (int) Math.pow(2, i - 4);
			}
			// bps = (sbyte[p+2] & 16) + (sbyte[p+2] & 32) + (sbyte[p+2] & 64) +
			// (sbyte[p+2] & 128);
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
				if ((sbyte[p + 3] & (int) Math.pow(2, i)) > 0)
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
