package org.discord.utils.korean;

import org.discord.utils.ArrayUtils;
import org.discord.utils.MapUtils;
import org.discord.utils.Tuple;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KoreanUtils {
	
	private final static char[] 쌍자음 = "ㄲㄸㅃㅆㅉ".toCharArray();
	private final static char[] 자음 = "ㄱㄲㄳㄴㄵㄶㄷㄸㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅃㅄㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
	private final static char[] 모음 = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
	private final static char[] 초성 = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
	private final static char[] 중성 = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
	private final static char[] 종성 = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();
	private final static char[] 숫자 = "일이삼사오육칠팔구".toCharArray();
	private final static char[] 단위 = "만억조경해자양구간정재극".toCharArray();
	private final static char[] 작은단위 = "십백천".toCharArray();
	private final static Map<Character, Character> kr2enMap = new HashMap<>(Map.ofEntries(
			Map.entry('ㅂ', 'q'),
			Map.entry('ㅃ', 'Q'),
			Map.entry('ㅈ', 'w'),
			Map.entry('ㅉ', 'W'),
			Map.entry('ㄷ', 'e'),
			Map.entry('ㄸ', 'E'),
			Map.entry('ㄱ', 'r'),
			Map.entry('ㄲ', 'R'),
			Map.entry('ㅅ', 't'),
			Map.entry('ㅆ', 'T'),
			Map.entry('ㅛ', 'y'),
			Map.entry('ㅕ', 'u'),
			Map.entry('ㅑ', 'i'),
			Map.entry('ㅐ', 'o'),
			Map.entry('ㅒ', 'O'),
			Map.entry('ㅔ', 'p'),
			Map.entry('ㅖ', 'P'),
			Map.entry('ㅁ', 'a'),
			Map.entry('ㄴ', 's'),
			Map.entry('ㅇ', 'd'),
			Map.entry('ㄹ', 'f'),
			Map.entry('ㅎ', 'g'),
			Map.entry('ㅗ', 'h'),
			Map.entry('ㅓ', 'j'),
			Map.entry('ㅏ', 'k'),
			Map.entry('ㅣ', 'l'),
			Map.entry('ㅋ', 'z'),
			Map.entry('ㅌ', 'x'),
			Map.entry('ㅊ', 'c'),
			Map.entry('ㅍ', 'v'),
			Map.entry('ㅠ', 'b'),
			Map.entry('ㅜ', 'n'),
			Map.entry('ㅡ', 'm')
	));
	public static Collection<String> sortKorean(Collection<String> collection) {
		return collection.stream().sorted((o1, o2) -> {
			char[] c1 = divideKorean(o1).toCharArray();
			char[] c2 = divideKorean(o2).toCharArray();
			for (int i = 0; i < Math.min(c1.length, c2.length); i++) {
				if (c1[i] == c2[i]) continue;
				return Character.compare(c1[i], c2[i]);
			}
			if (c1.length == c2.length) return 0;
			return Integer.compare(c1.length, c2.length);
		}).toList();
	}
	public static char[] divideKorean(char c) {
		if (!isKorean(c)) return new char[] {c};
		if (isSingle(c)) return new char[] {c};
		char[] result = new char[hasSupport(c) ? 3 : 2];
		int d = c - 44032;
		if (hasSupport(c)) {
			result[2] = 종성[d % 28 - 1];
		}
		result[1] = 중성[d / 28 % 21];
		result[0] = 초성[d / 588 % 19];
		return result;
	}
	public static String divideKorean(String s) {
		char[] chars = s.toCharArray();
		String result = "";
		for (char c : chars) {
			result += new String(divideKorean(c));
		}
		return result;
	}
	public static String mergeKorean(List<char[]> chars) {
		String result = "";
		for (char[] c : chars) {
			result += String.valueOf(mergeKorean(c));
		}
		return result;
	}
	public static char mergeKorean(char[] c) {
		boolean hasSupport = c.length == 3;
		if (!ArrayUtils.contains(초성, c[0])) return c[0];
		if (!ArrayUtils.contains(중성, c[1])) return c[1];
		if (hasSupport && !ArrayUtils.contains(종성, c[2])) return c[2];
		int result = new String(초성).indexOf(c[0]) * 588 + new String(중성).indexOf(c[1]) * 28;
		if (hasSupport) result += new String(종성).indexOf(c[2]) + 1;
		result += 44032;
		return (char) result;
	}
	public static String mergeKorean(String s) {
		return boolmergeKorean(s).a();
	}
	public static Tuple<String, Boolean> boolmergeKorean(String s) {
		char[] chars = s.toCharArray();
		String result = "";
		char[] krtemp = new char[] {' ', ' ', ' ', ' '};
		for (Character kr : chars) {
			if (!isKorean(kr)) {
				if (krtemp[3] != ' ') {
					if (softMergeKorean(krtemp[2], krtemp[3]) != '?') {
						result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], softMergeKorean(krtemp[2], krtemp[3])}));
					} else {
						result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
						if (krtemp[3] != 'n') result += String.valueOf(krtemp[3]);
					}
				} else if (krtemp[2] != ' ') {
					result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
				} else if (krtemp[1] != ' ') {
					if (softMergeKorean(krtemp[0], krtemp[1]) != '?') {
						result += String.valueOf(softMergeKorean(krtemp[0], krtemp[1]));
					} else {
						result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
					}
				} else if (krtemp[0] != ' ') {
					result += String.valueOf(krtemp[0]);
				}
				krtemp[0] = ' ';
				krtemp[1] = ' ';
				krtemp[2] = ' ';
				krtemp[3] = ' ';
				result += String.valueOf(kr);
				continue;
			}
			if (krtemp[0] == ' ') {
				if (isVowel(kr)) {
					result += String.valueOf(kr);
				} else if (ArrayUtils.contains(초성, kr)) {
					krtemp[0] = kr;
				} else {
					result += String.valueOf(kr);
				}
				continue;
			}
			if (krtemp[1] == ' ') {
				if (isConsonant(kr)) {
					if (softMergeKorean(krtemp[0], kr) != '?') {
						result += String.valueOf(softMergeKorean(krtemp[0], kr));
						krtemp[0] = ' ';
						continue;
					}
					result += String.valueOf(krtemp[0]);
					krtemp[0] = kr;
					continue;
				}
				krtemp[1] = kr;
				continue;
			}
			if (krtemp[2] == ' ') {
				if (isVowel(kr)) {
					if (softMergeKorean(krtemp[1], kr) != '?') {
						krtemp[1] = softMergeKorean(krtemp[1], kr);
					} else {
						result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
						result += String.valueOf(kr);
						krtemp[0] = ' ';
						krtemp[1] = ' ';
					}
					continue;
				} else if (ArrayUtils.contains(종성, kr)) {
					if (canSoftMerged(kr)) {
						krtemp[2] = kr;
					} else {
						if (ArrayUtils.contains(초성, kr)) {
							krtemp[2] = kr;
							krtemp[3] = 'n';
						} else {
							result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], kr}));
							krtemp[0] = ' ';
							krtemp[1] = ' ';
						}
					}
					continue;
				} else if (canSoftMerged(kr)) {
					krtemp[2] = kr;
					continue;
				} else {
					result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
					krtemp[0] = kr;
					krtemp[1] = ' ';
					continue;
				}
			}
			if (krtemp[3] == 'n') {
				if (isVowel(kr)) {
					result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
					krtemp[0] = krtemp[2];
					krtemp[1] = kr;
					krtemp[2] = ' ';
					krtemp[3] = ' ';
				} else {
					result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
					if (ArrayUtils.contains(초성, kr)) {
						krtemp[0] = kr;
					} else {
						result += String.valueOf(kr);
						krtemp[0] = ' ';
					}
					krtemp[1] = ' ';
					krtemp[2] = ' ';
					krtemp[3] = ' ';
				}
				continue;
			}
			if (krtemp[3] == ' ') {
				if (isVowel(kr)) {
					result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
					krtemp[0] = krtemp[2];
					krtemp[1] = kr;
					krtemp[2] = ' ';
				} else {
					if (softMergeKorean(krtemp[2], kr) != '?') {
						krtemp[3] = kr;
					} else {
						result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
						if (ArrayUtils.contains(초성, kr)) {
							krtemp[0] = kr;
						} else {
							result += String.valueOf(kr);
							krtemp[0] = ' ';
						}
						krtemp[1] = ' ';
						krtemp[2] = ' ';
					}
				}
				continue;
			}
			if (isVowel(kr)) {
				result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
				krtemp[0] = krtemp[3];
				krtemp[1] = kr;
				krtemp[2] = ' ';
				krtemp[3] = ' ';
			} else {
				result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], softMergeKorean(krtemp[2], krtemp[3])}));
				krtemp[0] = ' ';
				krtemp[1] = ' ';
				krtemp[2] = ' ';
				krtemp[3] = ' ';
				if (ArrayUtils.contains(초성, kr)) {
					krtemp[0] = kr;
				} else {
					result += String.valueOf(kr);
				}
			}
		}
		if (krtemp[3] != ' ') {
			if (softMergeKorean(krtemp[2], krtemp[3]) != '?') {
				result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], softMergeKorean(krtemp[2], krtemp[3])}));
			} else {
				result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
				if (krtemp[3] != 'n') result += String.valueOf(krtemp[3]);
			}
		} else if (krtemp[2] != ' ') {
			result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1], krtemp[2]}));
		} else if (krtemp[1] != ' ') {
			if (softMergeKorean(krtemp[0], krtemp[1]) != '?') {
				result += String.valueOf(softMergeKorean(krtemp[0], krtemp[1]));
			} else {
				result += String.valueOf(mergeKorean(new char[] {krtemp[0], krtemp[1]}));
			}
		} else if (krtemp[0] != ' ') {
			result += String.valueOf(krtemp[0]);
		}
		boolean bool = true;
		if (containsKorean(result)) {
			for (char c : result.toCharArray()) {
				if (isSingle(c)) {
					bool = false;
					break;
				}
			}
		} else bool = false;
		return new Tuple<>(result, bool);
	}
	public static char softMergeKorean(char a, char b) {
		if (a == 'ㄱ' && b == 'ㅅ') return 'ㄳ';
		if (a == 'ㄴ' && b == 'ㅈ') return 'ㄵ';
		if (a == 'ㄴ' && b == 'ㅎ') return 'ㄶ';
		if (a == 'ㄹ' && b == 'ㄱ') return 'ㄺ';
		if (a == 'ㄹ' && b == 'ㅁ') return 'ㄻ';
		if (a == 'ㄹ' && b == 'ㅂ') return 'ㄼ';
		if (a == 'ㄹ' && b == 'ㅅ') return 'ㄽ';
		if (a == 'ㄹ' && b == 'ㅌ') return 'ㄾ';
		if (a == 'ㄹ' && b == 'ㅍ') return 'ㄿ';
		if (a == 'ㄹ' && b == 'ㅎ') return 'ㅀ';
		if (a == 'ㅂ' && b == 'ㅅ') return 'ㅄ';
		if (a == 'ㅗ' && b == 'ㅏ') return 'ㅘ';
		if (a == 'ㅗ' && b == 'ㅐ') return 'ㅙ';
		if (a == 'ㅗ' && b == 'ㅣ') return 'ㅚ';
		if (a == 'ㅜ' && b == 'ㅓ') return 'ㅝ';
		if (a == 'ㅜ' && b == 'ㅔ') return 'ㅞ';
		if (a == 'ㅜ' && b == 'ㅣ') return 'ㅟ';
		if (a == 'ㅡ' && b == 'ㅣ') return 'ㅢ';
		return '?';
	}
	public static char[] softDivideKorean(char c) {
		if (c == 'ㄳ') return new char[] {'ㄱ', 'ㅅ'};
		if (c == 'ㄵ') return new char[] {'ㄴ', 'ㅈ'};
		if (c == 'ㄶ') return new char[] {'ㄴ', 'ㅎ'};
		if (c == 'ㄺ') return new char[] {'ㄹ', 'ㄱ'};
		if (c == 'ㄻ') return new char[] {'ㄹ', 'ㅁ'};
		if (c == 'ㄼ') return new char[] {'ㄹ', 'ㅂ'};
		if (c == 'ㄽ') return new char[] {'ㄹ', 'ㅅ'};
		if (c == 'ㄾ') return new char[] {'ㄹ', 'ㅌ'};
		if (c == 'ㄿ') return new char[] {'ㄹ', 'ㅍ'};
		if (c == 'ㅀ') return new char[] {'ㄹ', 'ㅎ'};
		if (c == 'ㅄ') return new char[] {'ㅂ', 'ㅅ'};
		if (c == 'ㅘ') return new char[] {'ㅗ', 'ㅏ'};
		if (c == 'ㅙ') return new char[] {'ㅗ', 'ㅐ'};
		if (c == 'ㅚ') return new char[] {'ㅗ', 'ㅣ'};
		if (c == 'ㅝ') return new char[] {'ㅜ', 'ㅓ'};
		if (c == 'ㅞ') return new char[] {'ㅜ', 'ㅔ'};
		if (c == 'ㅟ') return new char[] {'ㅜ', 'ㅣ'};
		if (c == 'ㅢ') return new char[] {'ㅡ', 'ㅣ'};
		return null;
	}
	public static String perfectDivide(String s) {
		char[] chars = s.toCharArray();
		String result = "";
		for (char c : chars) {
			if (isKorean(c)) {
				if (isSingle(c)) {
					if (softDivideKorean(c) != null) {
						for (char dv : softDivideKorean(c)) {
							result += String.valueOf(dv);
						}
					} else {
						result += String.valueOf(c);
					}
				} else {
					for (char divided : divideKorean(c)) {
						if (softDivideKorean(divided) != null) {
							for (char dv : softDivideKorean(divided)) {
								result += String.valueOf(dv);
							}
						} else {
							result += String.valueOf(divided);
						}
					}
				}
			} else {
				result += c;
			}
		}
		return result;
	}
	public static boolean isFullygksxkdudxk(String s) {
		return boolgksxkdudxk(s).b();
	}
	public static String gksxkdudxk(String s) {
		return boolgksxkdudxk(s).a();
	}
	public static Tuple<String, Boolean> boolgksxkdudxk(String s) {
		char[] chars = s.toCharArray();
		String result = "";
		for (char c : chars) {
			if (isKorean(c)) {
				if (isSingle(c)) {
					if (softDivideKorean(c) != null) {
						for (char dv : softDivideKorean(c)) {
							result += String.valueOf(kr2enMap.get(dv));
						}
					} else {
						result += String.valueOf(kr2enMap.get(c));
					}
				} else {
					for (char divided : divideKorean(c)) {
						if (softDivideKorean(divided) != null) {
							for (char dv : softDivideKorean(divided)) {
								result += String.valueOf(kr2enMap.get(dv));
							}
						} else {
							result += String.valueOf(kr2enMap.get(divided));
						}
					}
				}
			} else {
				Character kr = MapUtils.getByFirstValue(kr2enMap, c);
				if (kr == null) {
					kr = MapUtils.getByFirstValue(kr2enMap, String.valueOf(c).toLowerCase().charAt(0));
				}
				if (kr == null) {
					result += String.valueOf(c);
					continue;
				}
				result += String.valueOf(kr);
			}
		}
		return boolmergeKorean(result);
	}
	public static boolean isDouble(char c) {
		return ArrayUtils.contains(쌍자음, c);
	}
	public static boolean canSoftMerged(char c) {
		return c == 'ㄱ' || c == 'ㄴ' || c == 'ㄹ' || c == 'ㅂ';
	}
	public static boolean isConsonant(char c) {
		return String.valueOf(자음).contains(String.valueOf(c));
	}
	public static boolean isVowel(char c) {
		return String.valueOf(모음).contains(String.valueOf(c));
	}
	public static boolean hasSupport(char c) {
		if ((c - 44032) % 28 == 0) return false;
		return 55203 >= (int) c && (int) c >= 44032;
	}
	public static boolean isSingle(char c) {
		return 12643 >= (int) c && (int) c >= 12593;
	}
	public static boolean isKorean(char c) {
		if (isSingle(c)) return true;
		return 55203 >= (int) c && (int) c >= 44032;
	}
	public static boolean isKorean(String s) {
		for (char c : s.toCharArray()) {
			if (!isKorean(c)) return false;
		}
		return true;
	}
	public static boolean containsKorean(String s) {
		for (char c : s.toCharArray()) {
			if (isKorean(c)) return true;
		}
		return false;
	}
	public static String getPostposition(String word, 문장성분 element) {
		char last = word.charAt(word.length() - 1);
		if (!isKorean(last)) {
			if (element.equals(문장성분.주어)) {
				return word + "(이)가";
			}
			if (element.equals(문장성분.목적어)) {
				return word + "(을)를";
			}
		}
		if (hasSupport(last)) {
			if (element.equals(문장성분.주어)) {
				return word + "이";
			}
			if (element.equals(문장성분.목적어)) {
				return word + "을";
			}
		} else {
			if (element.equals(문장성분.주어)) {
				return word + "가";
			}
			if (element.equals(문장성분.목적어)) {
				return word + "를";
			}
		}
		throw new IllegalArgumentException(element.name() + "에서 조사를 추출할 수 없습니다!");
	}
	public static String getPostposition(String word, String has, String not) {
		char last = word.charAt(word.length() - 1);
		if (!isKorean(last)) return word;
		if (hasSupport(last)) {
			return word + has;
		} else {
			return word + not;
		}
	}
	public static String getPostposition(String word, 문장성분 element, boolean showWord) {
		char last = word.charAt(word.length() - 1);
		if (!isKorean(last)) {
			if (element.equals(문장성분.주어)) {
				return (showWord ? word : "") + "(이)가";
			}
			if (element.equals(문장성분.목적어)) {
				return (showWord ? word : "") + "(을)를";
			}
		}
		if (hasSupport(last)) {
			if (element.equals(문장성분.주어)) {
				return (showWord ? word : "") + "이";
			}
			if (element.equals(문장성분.목적어)) {
				return (showWord ? word : "") + "을";
			}
		} else {
			if (element.equals(문장성분.주어)) {
				return (showWord ? word : "") + "가";
			}
			if (element.equals(문장성분.목적어)) {
				return (showWord ? word : "") + "를";
			}
		}
		throw new IllegalArgumentException(element.name() + "에서 조사를 추출할 수 없습니다!");
	}
	public static String getPostposition(String word, String has, String not, boolean showWord) {
		char last = word.charAt(word.length() - 1);
		if (!isKorean(last)) return word;
		if (hasSupport(last)) {
			return (showWord ? word : "") + has;
		} else {
			return (showWord ? word : "") + not;
		}
	}
	public static String readToKorean(Number number) {
		return readToKorean(new BigDecimal(number.toString()));
	}
	public static String readToKorean(BigDecimal number) {
		if (number.compareTo(BigDecimal.ZERO) == 0) return "영";
		char[] c = number.toString().toCharArray();
		String result = "";
		int j = 0;
		boolean dot = !number.toString().contains(".");
		for (int i = c.length - 1; i >= 0; i--) {
			if (c[i] == '-') {
				result = "마이너스 " + result;
				j++;
				continue;
			}
			if (c[i] == '.') {
				dot = true;
				result = "쩜" + result;
				continue;
			}
			if (c[i] == '0') {
				if (dot) {
					j++;
					continue;
				} else {
					result = "영" + result;
				}
			}
			int num = Integer.parseInt(String.valueOf(c[i])) - 1;
			if (dot) {
				result = (c[i] == '1' && j % 4 != 0 ? "" : String.valueOf(숫자[num])) + (j % 4 == 0 ? "" : 작은단위[j % 4 - 1]) + (j % 4 == 0 ? (j / 4 == 0 ? "" : 단위[j / 4 - 1] + " ") : "") + result;
				j++;
			} else {
				result = (숫자[num]) + result;
			}
		}
		return result;
	}
}
