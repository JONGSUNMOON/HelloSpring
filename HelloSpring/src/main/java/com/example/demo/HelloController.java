package com.example.demo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {
	@Resource(name = "redisTemplate")
	private ZSetOperations<String, String> zSetOperations;

	@Resource(name = "redisTemplate")
	private HashOperations<String, String, String> hashOperations;

	@GetMapping("/place")
	public String place(@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "page", defaultValue = "1", required = false) String page,
			HttpServletRequest request) {
		
		HttpSession httpSession = request.getSession(false);

		if (httpSession != null) {
			if (httpSession.getAttribute("KKO_PLACE_SEARCH_USERID") != null) {
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				Date date = new Date();
				String time = format.format(date);

				hashOperations.put("user:" + httpSession.getAttribute("KKO_PLACE_SEARCH_USERID") + ":history", name, time);
			}
		}

		if (zSetOperations.score("hits", name) == null) {
			zSetOperations.add("hits", name, 1);
		} else {
			zSetOperations.add("hits", name, zSetOperations.score("hits", name) + 1);
		}

		String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query={name}&page={page}";

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "KakaoAK 8bc3c6de1da503e47e5a5d78f838f837");
		HttpEntity entity = new HttpEntity(headers);

		ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class, name, page);

		return response.getBody();
	}

	@GetMapping("/rank/keyword")
	public @ResponseBody String rankKeyword() {
		String rankText = "";

		Set<TypedTuple<String>> hitsSet = zSetOperations.reverseRangeWithScores("hits", 0, 9);

		for (TypedTuple<String> t : hitsSet) {
			rankText += t.getValue() + "\t" + t.getScore().intValue() + "회<br/>";
		}

		return rankText;
	}

	@GetMapping("/user/history")
	public @ResponseBody String userHistory(HttpServletRequest request) {
		HttpSession httpSession = request.getSession(false);

		if (httpSession != null) {
			if (httpSession.getAttribute("KKO_PLACE_SEARCH_USERID") != null) {
				String historyText = "";

				Map<String, String> historyMap = hashOperations.entries("user:" + httpSession.getAttribute("KKO_PLACE_SEARCH_USERID") + ":history");

				Map sortMap = sortByValue(historyMap);

				Iterator<String> iter = sortMap.keySet().iterator();
				while (iter.hasNext()) {
					String key = iter.next();

					String dateTime = (String) sortMap.get(key);

					SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
					Date date = new Date();
					try {
						date = format.parse(dateTime);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					SimpleDateFormat format2 = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
					String time = format2.format(date);

					historyText += key + "\t(" + time + ")<br/>";
				}

				return historyText;
			}
		}

		return "로그인 해주세요.";
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.<K, V>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	@PostMapping("/user/{id}")
	public void createUser(@PathVariable("id") String id, @RequestParam String pw) {
		hashOperations.put("user:" + id, "pw", pw);
	}

	@GetMapping("/user/signin/{id}")
	public boolean signinUser(@PathVariable("id") String id, @RequestParam String pw, HttpServletRequest request) {
		// get password
		Map<String, String> ids = hashOperations.entries("user:" + id);
		String userPw = ids.get("pw");

		// check password
		if (decrypt(pw, "Secret Passphrase").equals(decrypt(userPw, "Secret Passphrase"))) {
			// create session
			HttpSession httpSession = request.getSession(true);

			httpSession.setAttribute("KKO_PLACE_SEARCH_USERID", id);

			if (httpSession.getAttribute("KKO_PLACE_SEARCH_USERID") != null) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static String decrypt(String ciphertext, String passphrase) {
		try {
			final int keySize = 256;
			final int ivSize = 128;

			// 텍스트를 BASE64 형식으로 디코드 한다.
			byte[] ctBytes = Base64.decodeBase64(ciphertext.getBytes("UTF-8"));

			// 솔트를 구한다. (생략된 8비트는 Salted__ 시작되는 문자열이다.)
			byte[] saltBytes = Arrays.copyOfRange(ctBytes, 8, 16);

			// 암호화된 테스트를 구한다.( 솔트값 이후가 암호화된 텍스트 값이다.)
			byte[] ciphertextBytes = Arrays.copyOfRange(ctBytes, 16, ctBytes.length);

			// 비밀번호와 솔트에서 키와 IV값을 가져온다.
			byte[] key = new byte[keySize / 8];
			byte[] iv = new byte[ivSize / 8];
			EvpKDF(passphrase.getBytes("UTF-8"), keySize, ivSize, saltBytes, key, iv);

			// 복호화
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			byte[] recoveredPlaintextBytes = cipher.doFinal(ciphertextBytes);

			return new String(recoveredPlaintextBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, byte[] resultKey,
			byte[] resultIv) throws NoSuchAlgorithmException {
		
		return EvpKDF(password, keySize, ivSize, salt, 1, "MD5", resultKey, resultIv);
	}

	private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, int iterations,
			String hashAlgorithm, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
		
		keySize = keySize / 32;
		ivSize = ivSize / 32;
		int targetKeySize = keySize + ivSize;
		byte[] derivedBytes = new byte[targetKeySize * 4];
		int numberOfDerivedWords = 0;
		byte[] block = null;
		MessageDigest hasher = MessageDigest.getInstance(hashAlgorithm);
		while (numberOfDerivedWords < targetKeySize) {
			if (block != null) {
				hasher.update(block);
			}
			hasher.update(password);
			// Salting
			block = hasher.digest(salt);
			hasher.reset();
			// Iterations : 키 스트레칭(key stretching)
			for (int i = 1; i < iterations; i++) {
				block = hasher.digest(block);
				hasher.reset();
			}
			System.arraycopy(block, 0, derivedBytes, numberOfDerivedWords * 4,
					Math.min(block.length, (targetKeySize - numberOfDerivedWords) * 4));
			numberOfDerivedWords += block.length / 4;
		}
		System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4);
		System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4);
		
		return derivedBytes; // key + iv
	}

	@GetMapping("/user/logout")
	public boolean logoutUser(HttpServletRequest request) {
		HttpSession httpSession = request.getSession(false);

		if (httpSession == null) {
			return false;
		}

		httpSession.removeAttribute("KKO_PLACE_SEARCH_USERID");

		httpSession.invalidate();

		return true;
	}
}
