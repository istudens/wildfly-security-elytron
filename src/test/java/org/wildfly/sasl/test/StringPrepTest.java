package org.wildfly.sasl.test;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.sasl.util.ByteStringBuilder;
import org.wildfly.sasl.util.StringPrep;

/**
 * Tests of org.wildfly.sasl.util.StringPrep by RFC 3454
 * 
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class StringPrepTest {
	
	@Test
	public void testEncoding(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("", b, 0);
		Assert.assertArrayEquals(new byte[]{},b.toArray());
		StringPrep.encode("abc", b, 0);
		Assert.assertArrayEquals(new byte[]{'a','b','c'},b.toArray());
	}
	
	@Test
	public void testEncodingOf1byteChar(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0x61}, b.toArray());
	}
	
	@Test
	public void testEncodingOf2bytesChar(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\u0438", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0xD0,(byte)0xB8}, b.toArray());
	}
	
	@Test
	public void testEncodingOf3bytesChar(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\u4F60", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0xE4,(byte)0xBD,(byte)0xA0}, b.toArray());
	}
	
	@Test
	public void testEncodingOf4bytesChar(){
		
		assertTrue(Character.isHighSurrogate("\uD83C\uDCA1".charAt(0)));
		assertTrue(Character.isLowSurrogate("\uD83C\uDCA1".charAt(1)));
		
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\uD83C\uDCA1", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0xF0,(byte)0x9F,(byte)0x82,(byte)0xA1}, b.toArray());
		
	}
	
	@Test
	public void testEncodingStringWithSurrogates(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a\uD83C\uDCA1b", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0x61,(byte)0xF0,(byte)0x9F,(byte)0x82,(byte)0xA1,(byte)0x62}, b.toArray());
	}
	
	@Test
	public void testEncodingOfHighSurrogateWithoutLow() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\uD83C", b, 0); // only high surrogate
			throw new Exception("Not throwed Invalid surrogate pair");
		}
		catch(IllegalArgumentException e){}
	}
	
	@Test
	public void testEncodingOfLowSurrogateWithoutHigh() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\uDC00", b, 0); // only low surrogate
			throw new Exception("Not throwed Invalid surrogate pair");
		}
		catch(IllegalArgumentException e){}
	}
	
	@Test
	public void testEncodingOfTwoHighSurrogates() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\uD83C\uD83C", b, 0); // two high surrogates
			throw new Exception("Not throwed Invalid surrogate pair");
		}
		catch(IllegalArgumentException e){}
	}
	
	@Test
	public void testEncodingOfWrongOrderedSurrogates() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\uDCA1\uD83C", b, 0); // wrong order of surrogates
			throw new Exception("Not throwed Invalid surrogate pair");
		}
		catch(IllegalArgumentException e){}
	}
	
	@Test
	public void testRightString() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\u05BE", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0xD6,(byte)0xBE}, b.toArray());
	}
	
	@Test
	public void testRightStringWithNeutralChars() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\u05BE - \uFBA8", b, 0);
		Assert.assertArrayEquals(new byte[]{(byte)0xD6,(byte)0xBE,(byte)0x20,(byte)0x2D,(byte)0x20,(byte)0xEF,(byte)0xAE,(byte)0xA8}, b.toArray()); // D6 BE 20 2D 20 EF AE A8
	}
	
	/** RFC 3454: If a string contains any RandALCat character, the string MUST NOT contain any LCat character. */
	@Test
	public void testLeftInRightString() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\u05BE\uFBA8a\u05BE\uFBA8", b, 0); // <right><right><left><right><right>
			throw new Exception("Not throwed directionality exception");
		}
		catch(IllegalArgumentException e){}
	}
	
	/** RFC 3454: requirement 3 prohibits strings such as <U+0627><U+0031> */
	@Test
	public void testRightWithoutTrailing() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\u0627\u0031", b, 0); // <right><neutral>
			throw new Exception("Not throwed directionality exception");
		}
		catch(IllegalArgumentException e){}
	}
	
	/** RFC 3454: requirement 3 ... allows strings such as <U+0627><U+0031><U+0628> */
	@Test
	public void testRightWithTrailing() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("\u0627\u0031\u0628", b, 0); // <right><neutral><right>
	}
	
	@Test
	public void testRightWithoutLeading() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\u0031\u0627", b, 0); // <neutral><right>
			throw new Exception("Not throwed directionality exception");
		}
		catch(IllegalArgumentException e){}
	}
	
	@Test
	public void testRightWithoutLeadingAndTrailing() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		try{
			StringPrep.encode("\u0031\u0627\u0032", b, 0); // <neutral><right><neutral>
			throw new Exception("Not throwed directionality exception");
		}
		catch(IllegalArgumentException e){}
	}
	
	/** RFC 3454 3.1 Commonly mapped to nothing / Table B.1 */
	@Test
	public void testMappingToNothing(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a\u00AD\u1806\u200B\u2060\uFEFF\u034F\u180B\u180C\u180D\u200C\u200D\uFE00\uFE01\uFE02\uFE03\uFE04\uFE05\uFE06\uFE07\uFE08\uFE09\uFE0A\uFE0B\uFE0C\uFE0D\uFE0E\uFE0Fa", b, StringPrep.MAP_TO_NOTHING);
		Assert.assertArrayEquals(new byte[]{'a','a'},b.toArray());
	}
	
	/** RFC 3454 5.1 Space characters / Table C.1.2 */
	@Test
	public void testMappingNonAsciiSpaceToSpace(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u200B\u202F\u205F\u3000a", b, StringPrep.MAP_TO_SPACE);
		Assert.assertArrayEquals(new byte[]{'a',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ','a'},b.toArray());
	}
	
	/** RFC 5802 5.1. SCRAM Attributes - characters ',' or '=' in usernames */
	@Test
	public void testMappingScramLoginChars(){
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a,b=c", b, StringPrep.MAP_SCRAM_LOGIN_CHARS);
		Assert.assertArrayEquals(new byte[]{'a','=','2','C','b','=','3','D','c'},b.toArray());
	}
	
	/** Mapping for case-folding used with NFKC / Table B.2 */
	@Test
	@Ignore("By RFC should change all to lowercase, but java.text.Normalizer change all to uppercase, see ELY-47")
	public void testNormalizationWithNFKC(){
		ByteStringBuilder b = new ByteStringBuilder();
		
		String before = "a\u0041\u0042\u0043\u0044\u0045\u0046\u0047\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057\u0058\u0059\u005A\u00B5\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C6\u00C7\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF\u00D0\u00D1\u00D2\u00D3\u00D4\u00D5\u00D6\u00D8\u00D9\u00DA\u00DB\u00DC\u00DD\u00DE\u00DF\u0100\u0102\u0104\u0106\u0108\u010A\u010C\u010E\u0110\u0112\u0114\u0116\u0118\u011A\u011C\u011E\u0120\u0122\u0124\u0126\u0128\u012A\u012C\u012E\u0130\u0132\u0134\u0136\u0139\u013B\u013D\u013F\u0141\u0143\u0145\u0147\u0149\u014A\u014C\u014E\u0150\u0152\u0154\u0156\u0158\u015A\u015C\u015E\u0160\u0162\u0164\u0166\u0168\u016A\u016C\u016E\u0170\u0172\u0174\u0176\u0178\u0179\u017B\u017D\u017F\u0181\u0182\u0184\u0186\u0187\u0189\u018A\u018B\u018E\u018F\u0190\u0191\u0193\u0194\u0196\u0197\u0198\u019C\u019D\u019F\u01A0\u01A2\u01A4\u01A6\u01A7\u01A9\u01AC\u01AE\u01AF\u01B1\u01B2\u01B3\u01B5\u01B7\u01B8\u01BC\u01C4\u01C5\u01C7\u01C8\u01CA\u01CB\u01CD\u01CF\u01D1\u01D3\u01D5\u01D7\u01D9\u01DB\u01DE\u01E0\u01E2\u01E4\u01E6\u01E8\u01EA\u01EC\u01EE\u01F0\u01F1\u01F2\u01F4\u01F6\u01F7\u01F8\u01FA\u01FC\u01FE\u0200\u0202\u0204\u0206\u0208\u020A\u020C\u020E\u0210\u0212\u0214\u0216\u0218\u021A\u021C\u021E\u0220\u0222\u0224\u0226\u0228\u022A\u022C\u022E\u0230\u0232\u0345\u037A\u0386\u0388\u0389\u038A\u038C\u038E\u038F\u0390\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398\u0399\u039A\u039B\u039C\u039D\u039E\u039F\u03A0\u03A1\u03A3\u03A4\u03A5\u03A6\u03A7\u03A8\u03A9\u03AA\u03AB\u03B0\u03C2\u03D0\u03D1\u03D2\u03D3\u03D4\u03D5\u03D6\u03D8\u03DA\u03DC\u03DE\u03E0\u03E2\u03E4\u03E6\u03E8\u03EA\u03EC\u03EE\u03F0\u03F1\u03F2\u03F4\u03F5\u0400\u0401\u0402\u0403\u0404\u0405\u0406\u0407\u0408\u0409\u040A\u040B\u040C\u040D\u040E\u040F\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418\u0419\u041A\u041B\u041C\u041D\u041E\u041F\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042A\u042B\u042C\u042D\u042E\u042F\u0460\u0462\u0464\u0466\u0468\u046A\u046C\u046E\u0470\u0472\u0474\u0476\u0478\u047A\u047C\u047E\u0480\u048A\u048C\u048E\u0490\u0492\u0494\u0496\u0498\u049A\u049C\u049E\u04A0\u04A2\u04A4\u04A6\u04A8\u04AA\u04AC\u04AE\u04B0\u04B2\u04B4\u04B6\u04B8\u04BA\u04BC\u04BE\u04C1\u04C3\u04C5\u04C7\u04C9\u04CB\u04CD\u04D0\u04D2\u04D4\u04D6\u04D8\u04DA\u04DC\u04DE\u04E0\u04E2\u04E4\u04E6\u04E8\u04EA\u04EC\u04EE\u04F0\u04F2\u04F4\u04F8\u0500\u0502\u0504\u0506\u0508\u050A\u050C\u050E\u0531\u0532\u0533\u0534\u0535\u0536\u0537\u0538\u0539\u053A\u053B\u053C\u053D\u053E\u053F\u0540\u0541\u0542\u0543\u0544\u0545\u0546\u0547\u0548\u0549\u054A\u054B\u054C\u054D\u054E\u054F\u0550\u0551\u0552\u0553\u0554\u0555\u0556\u0587\u1E00\u1E02\u1E04\u1E06\u1E08\u1E0A\u1E0C\u1E0E\u1E10\u1E12\u1E14\u1E16\u1E18\u1E1A\u1E1C\u1E1E\u1E20\u1E22\u1E24\u1E26\u1E28\u1E2A\u1E2C\u1E2E\u1E30\u1E32\u1E34\u1E36\u1E38\u1E3A\u1E3C\u1E3E\u1E40\u1E42\u1E44\u1E46\u1E48\u1E4A\u1E4C\u1E4E\u1E50\u1E52\u1E54\u1E56\u1E58\u1E5A\u1E5C\u1E5E\u1E60\u1E62\u1E64\u1E66\u1E68\u1E6A\u1E6C\u1E6E\u1E70\u1E72\u1E74\u1E76\u1E78\u1E7A\u1E7C\u1E7E\u1E80\u1E82\u1E84\u1E86\u1E88\u1E8A\u1E8C\u1E8E\u1E90\u1E92\u1E94\u1E96\u1E97\u1E98\u1E99\u1E9A\u1E9B\u1EA0\u1EA2\u1EA4\u1EA6\u1EA8\u1EAA\u1EAC\u1EAE\u1EB0\u1EB2\u1EB4\u1EB6\u1EB8\u1EBA\u1EBC\u1EBE\u1EC0\u1EC2\u1EC4\u1EC6\u1EC8\u1ECA\u1ECC\u1ECE\u1ED0\u1ED2\u1ED4\u1ED6\u1ED8\u1EDA\u1EDC\u1EDE\u1EE0\u1EE2\u1EE4\u1EE6\u1EE8\u1EEA\u1EEC\u1EEE\u1EF0\u1EF2\u1EF4\u1EF6\u1EF8\u1F08\u1F09\u1F0A\u1F0B\u1F0C\u1F0D\u1F0E\u1F0F\u1F18\u1F19\u1F1A\u1F1B\u1F1C\u1F1D\u1F28\u1F29\u1F2A\u1F2B\u1F2C\u1F2D\u1F2E\u1F2F\u1F38\u1F39\u1F3A\u1F3B\u1F3C\u1F3D\u1F3E\u1F3F\u1F48\u1F49\u1F4A\u1F4B\u1F4C\u1F4D\u1F50\u1F52\u1F54\u1F56\u1F59\u1F5B\u1F5D\u1F5F\u1F68\u1F69\u1F6A\u1F6B\u1F6C\u1F6D\u1F6E\u1F6F\u1F80\u1F81\u1F82\u1F83\u1F84\u1F85\u1F86\u1F87\u1F88\u1F89\u1F8A\u1F8B\u1F8C\u1F8D\u1F8E\u1F8F\u1F90\u1F91\u1F92\u1F93\u1F94\u1F95\u1F96\u1F97\u1F98\u1F99\u1F9A\u1F9B\u1F9C\u1F9D\u1F9E\u1F9F\u1FA0\u1FA1\u1FA2\u1FA3\u1FA4\u1FA5\u1FA6\u1FA7\u1FA8\u1FA9\u1FAA\u1FAB\u1FAC\u1FAD\u1FAE\u1FAF\u1FB2\u1FB3\u1FB4\u1FB6\u1FB7\u1FB8\u1FB9\u1FBA\u1FBB\u1FBC\u1FBE\u1FC2\u1FC3\u1FC4\u1FC6\u1FC7\u1FC8\u1FC9\u1FCA\u1FCB\u1FCC\u1FD2\u1FD3\u1FD6\u1FD7\u1FD8\u1FD9\u1FDA\u1FDB\u1FE2\u1FE3\u1FE4\u1FE6\u1FE7\u1FE8\u1FE9\u1FEA\u1FEB\u1FEC\u1FF2\u1FF3\u1FF4\u1FF6\u1FF7\u1FF8\u1FF9\u1FFA\u1FFB\u1FFC\u20A8\u2102\u2103\u2107\u2109\u210B\u210C\u210D\u2110\u2111\u2112\u2115\u2116\u2119\u211A\u211B\u211C\u211D\u2120\u2121\u2122\u2124\u2126\u2128\u212A\u212B\u212C\u212D\u2130\u2131\u2133\u213E\u213F\u2145\u2160\u2161\u2162\u2163\u2164\u2165\u2166\u2167\u2168\u2169\u216A\u216B\u216C\u216D\u216E\u216F\u24B6\u24B7\u24B8\u24B9\u24BA\u24BB\u24BC\u24BD\u24BE\u24BF\u24C0\u24C1\u24C2\u24C3\u24C4\u24C5\u24C6\u24C7\u24C8\u24C9\u24CA\u24CB\u24CC\u24CD\u24CE\u24CF\u3371\u3373\u3375\u3380\u3381\u3382\u3383\u3384\u3385\u3386\u3387\u338A\u338B\u338C\u3390\u3391\u3392\u3393\u3394\u33A9\u33AA\u33AB\u33AC\u33B4\u33B5\u33B6\u33B7\u33B8\u33B9\u33BA\u33BB\u33BC\u33BD\u33BE\u33BF\u33C0\u33C1\u33C3\u33C6\u33C7\u33C8\u33C9\u33CB\u33CD\u33CE\u33D7\u33D9\u33DA\u33DC\u33DD\uFB00\uFB01\uFB02\uFB03\uFB04\uFB05\uFB06\uFB13\uFB14\uFB15\uFB16\uFB17\uFF21\uFF22\uFF23\uFF24\uFF25\uFF26\uFF27\uFF28\uFF29\uFF2A\uFF2B\uFF2C\uFF2D\uFF2E\uFF2F\uFF30\uFF31\uFF32\uFF33\uFF34\uFF35\uFF36\uFF37\uFF38\uFF39\uFF3A\u10400\u10401\u10402\u10403\u10404\u10405\u10406\u10407\u10408\u10409\u1040A\u1040B\u1040C\u1040D\u1040E\u1040F\u10410\u10411\u10412\u10413\u10414\u10415\u10416\u10417\u10418\u10419\u1041A\u1041B\u1041C\u1041D\u1041E\u1041F\u10420\u10421\u10422\u10423\u10424\u10425\u1D400\u1D401\u1D402\u1D403\u1D404\u1D405\u1D406\u1D407\u1D408\u1D409\u1D40A\u1D40B\u1D40C\u1D40D\u1D40E\u1D40F\u1D410\u1D411\u1D412\u1D413\u1D414\u1D415\u1D416\u1D417\u1D418\u1D419\u1D434\u1D435\u1D436\u1D437\u1D438\u1D439\u1D43A\u1D43B\u1D43C\u1D43D\u1D43E\u1D43F\u1D440\u1D441\u1D442\u1D443\u1D444\u1D445\u1D446\u1D447\u1D448\u1D449\u1D44A\u1D44B\u1D44C\u1D44D\u1D468\u1D469\u1D46A\u1D46B\u1D46C\u1D46D\u1D46E\u1D46F\u1D470\u1D471\u1D472\u1D473\u1D474\u1D475\u1D476\u1D477\u1D478\u1D479\u1D47A\u1D47B\u1D47C\u1D47D\u1D47E\u1D47F\u1D480\u1D481\u1D49C\u1D49E\u1D49F\u1D4A2\u1D4A5\u1D4A6\u1D4A9\u1D4AA\u1D4AB\u1D4AC\u1D4AE\u1D4AF\u1D4B0\u1D4B1\u1D4B2\u1D4B3\u1D4B4\u1D4B5\u1D4D0\u1D4D1\u1D4D2\u1D4D3\u1D4D4\u1D4D5\u1D4D6\u1D4D7\u1D4D8\u1D4D9\u1D4DA\u1D4DB\u1D4DC\u1D4DD\u1D4DE\u1D4DF\u1D4E0\u1D4E1\u1D4E2\u1D4E3\u1D4E4\u1D4E5\u1D4E6\u1D4E7\u1D4E8\u1D4E9\u1D504\u1D505\u1D507\u1D508\u1D509\u1D50A\u1D50D\u1D50E\u1D50F\u1D510\u1D511\u1D512\u1D513\u1D514\u1D516\u1D517\u1D518\u1D519\u1D51A\u1D51B\u1D51C\u1D538\u1D539\u1D53B\u1D53C\u1D53D\u1D53E\u1D540\u1D541\u1D542\u1D543\u1D544\u1D546\u1D54A\u1D54B\u1D54C\u1D54D\u1D54E\u1D54F\u1D550\u1D56C\u1D56D\u1D56E\u1D56F\u1D570\u1D571\u1D572\u1D573\u1D574\u1D575\u1D576\u1D577\u1D578\u1D579\u1D57A\u1D57B\u1D57C\u1D57D\u1D57E\u1D57F\u1D580\u1D581\u1D582\u1D583\u1D584\u1D585\u1D5A0\u1D5A1\u1D5A2\u1D5A3\u1D5A4\u1D5A5\u1D5A6\u1D5A7\u1D5A8\u1D5A9\u1D5AA\u1D5AB\u1D5AC\u1D5AD\u1D5AE\u1D5AF\u1D5B0\u1D5B1\u1D5B2\u1D5B3\u1D5B4\u1D5B5\u1D5B6\u1D5B7\u1D5B8\u1D5B9\u1D5D4\u1D5D5\u1D5D6\u1D5D7\u1D5D8\u1D5D9\u1D5DA\u1D5DB\u1D5DC\u1D5DD\u1D5DE\u1D5DF\u1D5E0\u1D5E1\u1D5E2\u1D5E3\u1D5E4\u1D5E5\u1D5E6\u1D5E7\u1D5E8\u1D5E9\u1D5EA\u1D5EB\u1D5EC\u1D5ED\u1D608\u1D609\u1D60A\u1D60B\u1D60C\u1D60D\u1D60E\u1D60F\u1D610\u1D611\u1D612\u1D613\u1D614\u1D615\u1D616\u1D617\u1D618\u1D619\u1D61A\u1D61B\u1D61C\u1D61D\u1D61E\u1D61F\u1D620\u1D621\u1D63C\u1D63D\u1D63E\u1D63F\u1D640\u1D641\u1D642\u1D643\u1D644\u1D645\u1D646\u1D647\u1D648\u1D649\u1D64A\u1D64B\u1D64C\u1D64D\u1D64E\u1D64F\u1D650\u1D651\u1D652\u1D653\u1D654\u1D655\u1D670\u1D671\u1D672\u1D673\u1D674\u1D675\u1D676\u1D677\u1D678\u1D679\u1D67A\u1D67B\u1D67C\u1D67D\u1D67E\u1D67F\u1D680\u1D681\u1D682\u1D683\u1D684\u1D685\u1D686\u1D687\u1D688\u1D689\u1D6A8\u1D6A9\u1D6AA\u1D6AB\u1D6AC\u1D6AD\u1D6AE\u1D6AF\u1D6B0\u1D6B1\u1D6B2\u1D6B3\u1D6B4\u1D6B5\u1D6B6\u1D6B7\u1D6B8\u1D6B9\u1D6BA\u1D6BB\u1D6BC\u1D6BD\u1D6BE\u1D6BF\u1D6C0\u1D6D3\u1D6E2\u1D6E3\u1D6E4\u1D6E5\u1D6E6\u1D6E7\u1D6E8\u1D6E9\u1D6EA\u1D6EB\u1D6EC\u1D6ED\u1D6EE\u1D6EF\u1D6F0\u1D6F1\u1D6F2\u1D6F3\u1D6F4\u1D6F5\u1D6F6\u1D6F7\u1D6F8\u1D6F9\u1D6FA\u1D70D\u1D71C\u1D71D\u1D71E\u1D71F\u1D720\u1D721\u1D722\u1D723\u1D724\u1D725\u1D726\u1D727\u1D728\u1D729\u1D72A\u1D72B\u1D72C\u1D72D\u1D72E\u1D72F\u1D730\u1D731\u1D732\u1D733\u1D734\u1D747\u1D756\u1D757\u1D758\u1D759\u1D75A\u1D75B\u1D75C\u1D75D\u1D75E\u1D75F\u1D760\u1D761\u1D762\u1D763\u1D764\u1D765\u1D766\u1D767\u1D768\u1D769\u1D76A\u1D76B\u1D76C\u1D76D\u1D76E\u1D781\u1D790\u1D791\u1D792\u1D793\u1D794\u1D795\u1D796\u1D797\u1D798\u1D799\u1D79A\u1D79B\u1D79C\u1D79D\u1D79E\u1D79F\u1D7A0\u1D7A1\u1D7A2\u1D7A3\u1D7A4\u1D7A5\u1D7A6\u1D7A7\u1D7A8\u1D7BBb";
		String after =  "a\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u03BC\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E6\u00E7\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F8\u00F9\u00FA\u00FB\u00FC\u00FD\u00FE\u00730073\u0101\u0103\u0105\u0107\u0109\u010B\u010D\u010F\u0111\u0113\u0115\u0117\u0119\u011B\u011D\u011F\u0121\u0123\u0125\u0127\u0129\u012B\u012D\u012F\u00690307\u0133\u0135\u0137\u013A\u013C\u013E\u0140\u0142\u0144\u0146\u0148\u02BC006E\u014B\u014D\u014F\u0151\u0153\u0155\u0157\u0159\u015B\u015D\u015F\u0161\u0163\u0165\u0167\u0169\u016B\u016D\u016F\u0171\u0173\u0175\u0177\u00FF\u017A\u017C\u017E\u0073\u0253\u0183\u0185\u0254\u0188\u0256\u0257\u018C\u01DD\u0259\u025B\u0192\u0260\u0263\u0269\u0268\u0199\u026F\u0272\u0275\u01A1\u01A3\u01A5\u0280\u01A8\u0283\u01AD\u0288\u01B0\u028A\u028B\u01B4\u01B6\u0292\u01B9\u01BD\u01C6\u01C6\u01C9\u01C9\u01CC\u01CC\u01CE\u01D0\u01D2\u01D4\u01D6\u01D8\u01DA\u01DC\u01DF\u01E1\u01E3\u01E5\u01E7\u01E9\u01EB\u01ED\u01EF\u006A030C\u01F3\u01F3\u01F5\u0195\u01BF\u01F9\u01FB\u01FD\u01FF\u0201\u0203\u0205\u0207\u0209\u020B\u020D\u020F\u0211\u0213\u0215\u0217\u0219\u021B\u021D\u021F\u019E\u0223\u0225\u0227\u0229\u022B\u022D\u022F\u0231\u0233\u03B9\u002003B9\u03AC\u03AD\u03AE\u03AF\u03CC\u03CD\u03CE\u03B903080301\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03CA\u03CB\u03C503080301\u03C3\u03B2\u03B8\u03C5\u03CD\u03CB\u03C6\u03C0\u03D9\u03DB\u03DD\u03DF\u03E1\u03E3\u03E5\u03E7\u03E9\u03EB\u03ED\u03EF\u03BA\u03C1\u03C3\u03B8\u03B5\u0450\u0451\u0452\u0453\u0454\u0455\u0456\u0457\u0458\u0459\u045A\u045B\u045C\u045D\u045E\u045F\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438\u0439\u043A\u043B\u043C\u043D\u043E\u043F\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044A\u044B\u044C\u044D\u044E\u044F\u0461\u0463\u0465\u0467\u0469\u046B\u046D\u046F\u0471\u0473\u0475\u0477\u0479\u047B\u047D\u047F\u0481\u048B\u048D\u048F\u0491\u0493\u0495\u0497\u0499\u049B\u049D\u049F\u04A1\u04A3\u04A5\u04A7\u04A9\u04AB\u04AD\u04AF\u04B1\u04B3\u04B5\u04B7\u04B9\u04BB\u04BD\u04BF\u04C2\u04C4\u04C6\u04C8\u04CA\u04CC\u04CE\u04D1\u04D3\u04D5\u04D7\u04D9\u04DB\u04DD\u04DF\u04E1\u04E3\u04E5\u04E7\u04E9\u04EB\u04ED\u04EF\u04F1\u04F3\u04F5\u04F9\u0501\u0503\u0505\u0507\u0509\u050B\u050D\u050F\u0561\u0562\u0563\u0564\u0565\u0566\u0567\u0568\u0569\u056A\u056B\u056C\u056D\u056E\u056F\u0570\u0571\u0572\u0573\u0574\u0575\u0576\u0577\u0578\u0579\u057A\u057B\u057C\u057D\u057E\u057F\u0580\u0581\u0582\u0583\u0584\u0585\u0586\u05650582\u1E01\u1E03\u1E05\u1E07\u1E09\u1E0B\u1E0D\u1E0F\u1E11\u1E13\u1E15\u1E17\u1E19\u1E1B\u1E1D\u1E1F\u1E21\u1E23\u1E25\u1E27\u1E29\u1E2B\u1E2D\u1E2F\u1E31\u1E33\u1E35\u1E37\u1E39\u1E3B\u1E3D\u1E3F\u1E41\u1E43\u1E45\u1E47\u1E49\u1E4B\u1E4D\u1E4F\u1E51\u1E53\u1E55\u1E57\u1E59\u1E5B\u1E5D\u1E5F\u1E61\u1E63\u1E65\u1E67\u1E69\u1E6B\u1E6D\u1E6F\u1E71\u1E73\u1E75\u1E77\u1E79\u1E7B\u1E7D\u1E7F\u1E81\u1E83\u1E85\u1E87\u1E89\u1E8B\u1E8D\u1E8F\u1E91\u1E93\u1E95\u00680331\u00740308\u0077030A\u0079030A\u006102BE\u1E61\u1EA1\u1EA3\u1EA5\u1EA7\u1EA9\u1EAB\u1EAD\u1EAF\u1EB1\u1EB3\u1EB5\u1EB7\u1EB9\u1EBB\u1EBD\u1EBF\u1EC1\u1EC3\u1EC5\u1EC7\u1EC9\u1ECB\u1ECD\u1ECF\u1ED1\u1ED3\u1ED5\u1ED7\u1ED9\u1EDB\u1EDD\u1EDF\u1EE1\u1EE3\u1EE5\u1EE7\u1EE9\u1EEB\u1EED\u1EEF\u1EF1\u1EF3\u1EF5\u1EF7\u1EF9\u1F00\u1F01\u1F02\u1F03\u1F04\u1F05\u1F06\u1F07\u1F10\u1F11\u1F12\u1F13\u1F14\u1F15\u1F20\u1F21\u1F22\u1F23\u1F24\u1F25\u1F26\u1F27\u1F30\u1F31\u1F32\u1F33\u1F34\u1F35\u1F36\u1F37\u1F40\u1F41\u1F42\u1F43\u1F44\u1F45\u03C50313\u03C503130300\u03C503130301\u03C503130342\u1F51\u1F53\u1F55\u1F57\u1F60\u1F61\u1F62\u1F63\u1F64\u1F65\u1F66\u1F67\u1F0003B9\u1F0103B9\u1F0203B9\u1F0303B9\u1F0403B9\u1F0503B9\u1F0603B9\u1F0703B9\u1F0003B9\u1F0103B9\u1F0203B9\u1F0303B9\u1F0403B9\u1F0503B9\u1F0603B9\u1F0703B9\u1F2003B9\u1F2103B9\u1F2203B9\u1F2303B9\u1F2403B9\u1F2503B9\u1F2603B9\u1F2703B9\u1F2003B9\u1F2103B9\u1F2203B9\u1F2303B9\u1F2403B9\u1F2503B9\u1F2603B9\u1F2703B9\u1F6003B9\u1F6103B9\u1F6203B9\u1F6303B9\u1F6403B9\u1F6503B9\u1F6603B9\u1F6703B9\u1F6003B9\u1F6103B9\u1F6203B9\u1F6303B9\u1F6403B9\u1F6503B9\u1F6603B9\u1F6703B9\u1F7003B9\u03B103B9\u03AC03B9\u03B10342\u03B1034203B9\u1FB0\u1FB1\u1F70\u1F71\u03B103B9\u03B9\u1F7403B9\u03B703B9\u03AE03B9\u03B70342\u03B7034203B9\u1F72\u1F73\u1F74\u1F75\u03B703B9\u03B903080300\u03B903080301\u03B90342\u03B903080342\u1FD0\u1FD1\u1F76\u1F77\u03C503080300\u03C503080301\u03C10313\u03C50342\u03C503080342\u1FE0\u1FE1\u1F7A\u1F7B\u1FE5\u1F7C03B9\u03C903B9\u03CE03B9\u03C90342\u03C9034203B9\u1F78\u1F79\u1F7C\u1F7D\u03C903B9\u00720073\u0063\u00B00063\u025B\u00B00066\u0068\u0068\u0068\u0069\u0069\u006C\u006E\u006E006F\u0070\u0071\u0072\u0072\u0072\u0073006D\u00740065006C\u0074006D\u007A\u03C9\u007A\u006B\u00E5\u0062\u0063\u0065\u0066\u006D\u03B3\u03C0\u0064\u2170\u2171\u2172\u2173\u2174\u2175\u2176\u2177\u2178\u2179\u217A\u217B\u217C\u217D\u217E\u217F\u24D0\u24D1\u24D2\u24D3\u24D4\u24D5\u24D6\u24D7\u24D8\u24D9\u24DA\u24DB\u24DC\u24DD\u24DE\u24DF\u24E0\u24E1\u24E2\u24E3\u24E4\u24E5\u24E6\u24E7\u24E8\u24E9\u006800700061\u00610075\u006F0076\u00700061\u006E0061\u03BC0061\u006D0061\u006B0061\u006B0062\u006D0062\u00670062\u00700066\u006E0066\u03BC0066\u0068007A\u006B0068007A\u006D0068007A\u00670068007A\u00740068007A\u00700061\u006B00700061\u006D00700061\u006700700061\u00700076\u006E0076\u03BC0076\u006D0076\u006B0076\u006D0076\u00700077\u006E0077\u03BC0077\u006D0077\u006B0077\u006D0077\u006B03C9\u006D03C9\u00620071\u00632215006B0067\u0063006F002E\u00640062\u00670079\u00680070\u006B006B\u006B006D\u00700068\u00700070006D\u00700072\u00730076\u00770062\u00660066\u00660069\u0066006C\u006600660069\u00660066006C\u00730074\u00730074\u05740576\u05740565\u0574056B\u057E0576\u0574056D\uFF41\uFF42\uFF43\uFF44\uFF45\uFF46\uFF47\uFF48\uFF49\uFF4A\uFF4B\uFF4C\uFF4D\uFF4E\uFF4F\uFF50\uFF51\uFF52\uFF53\uFF54\uFF55\uFF56\uFF57\uFF58\uFF59\uFF5A\u10428\u10429\u1042A\u1042B\u1042C\u1042D\u1042E\u1042F\u10430\u10431\u10432\u10433\u10434\u10435\u10436\u10437\u10438\u10439\u1043A\u1043B\u1043C\u1043D\u1043E\u1043F\u10440\u10441\u10442\u10443\u10444\u10445\u10446\u10447\u10448\u10449\u1044A\u1044B\u1044C\u1044D\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0063\u0064\u0067\u006A\u006B\u006E\u006F\u0070\u0071\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0064\u0065\u0066\u0067\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u0061\u0062\u0064\u0065\u0066\u0067\u0069\u006A\u006B\u006C\u006D\u006F\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03B8\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03C3\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03B8\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03C3\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03B8\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03C3\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03B8\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03C3\u03B1\u03B2\u03B3\u03B4\u03B5\u03B6\u03B7\u03B8\u03B9\u03BA\u03BB\u03BC\u03BD\u03BE\u03BF\u03C0\u03C1\u03B8\u03C3\u03C4\u03C5\u03C6\u03C7\u03C8\u03C9\u03C3b";
		
		StringPrep.encode(before, b, StringPrep.NORMALIZE_KC);
		assertEquals(after, new String(b.toArray()));
	}
	
	@Test
	public void testForbitNonAsciiSpaces() throws Exception {
		String forbidded = "\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u200B\u202F\u205F\u3000";
		for(char c : forbidded.toCharArray()){
			try{
				ByteStringBuilder b = new ByteStringBuilder();
				StringPrep.encode(Character.toString(c), b, StringPrep.FORBID_NON_ASCII_SPACES);
				throw new Exception("Not throwed IllegalArgumentException on "+String.format("%04x",(int)c));
			}catch(IllegalArgumentException e){}
		}
	}
	
	@Test
	public void testForbitAsciiControl() throws Exception {
		testForbitChars(StringPrep.FORBID_ASCII_CONTROL, new char[]{0x0000,0x0001,0x0002,0x0003,0x0004,0x0005,0x0006,0x0007,0x0008,0x0009,0x000A,0x000B,0x000C,0x000D,0x000E,0x000F,0x0010,0x0011,0x0012,0x0013,0x0014,0x0015,0x0016,0x0017,0x0018,0x0019,0x001A,0x001B,0x001C,0x001D,0x001E,0x001F,0x007F});
	}
	
	@Test
	public void testForbitNonAsciiControl() throws Exception {
		testForbitChars(StringPrep.FORBID_NON_ASCII_CONTROL, (int)0x0080,(int)0x009F);
		testForbitChars(StringPrep.FORBID_NON_ASCII_CONTROL, new char[]{0x06DD,0x070F,0x180E,0x200C,0x200D,0x2028,0x2029,0x2060,0x2061,0x2062,0x2063,0xFEFF});
		testForbitChars(StringPrep.FORBID_NON_ASCII_CONTROL, (int)0x206A,(int)0x206F);
		testForbitChars(StringPrep.FORBID_NON_ASCII_CONTROL, (int)0xFFF9,(int)0xFFFC);
	}
	
	@Test
	public void testForbitPrivateUse() throws Exception {
		testForbitChars(StringPrep.FORBID_PRIVATE_USE, (int)0xE000,(int)0xF8FF);
		testForbitChars(StringPrep.FORBID_PRIVATE_USE, (int)0xF0000,(int)0xFFFFD);
		testForbitChars(StringPrep.FORBID_PRIVATE_USE, (int)0x100000,(int)0x10FFFD);
	}
	
	/** 5.4 Non-character code points */
	@Test
	public void testForbitNonCharacter() throws Exception {
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xFDD0,(int)0xFDEF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xFFFE,(int)0xFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x1FFFE,(int)0x1FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x2FFFE,(int)0x2FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x3FFFE,(int)0x3FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x4FFFE,(int)0x4FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x5FFFE,(int)0x5FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x6FFFE,(int)0x6FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x7FFFE,(int)0x7FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x8FFFE,(int)0x8FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x9FFFE,(int)0x9FFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xAFFFE,(int)0xAFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xBFFFE,(int)0xBFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xCFFFE,(int)0xCFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xDFFFE,(int)0xDFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xEFFFE,(int)0xEFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0xFFFFE,(int)0xFFFFF);
		testForbitChars(StringPrep.FORBID_NON_CHARACTER, (int)0x10FFFE,(int)0x10FFFF);
	}
	
	/** 5.5 Surrogate codes */
	@Test
	public void testForbitSurrogate() throws Exception {
		testForbitChars(StringPrep.FORBID_SURROGATE, (int)0xD800,(int)0xDFFF);
	}
	
	/** 5.6 Inappropriate for plain text */
	@Test
	public void testForbitInappropriateForPlainText() throws Exception {
		testForbitChars(StringPrep.FORBID_INAPPROPRIATE_FOR_PLAIN_TEXT, (int)0xFFF9,(int)0xFFFD);
	}
	
	/** 5.7 Inappropriate for canonical representation */
	@Test
	public void testForbitInappropriateForCanonicalRepresentation() throws Exception {
		testForbitChars(StringPrep.FORBID_INAPPROPRIATE_FOR_CANON_REP, (int)0x2FF0,(int)0x2FFB);
	}
	
	/** 5.8 Change display properties or are deprecated */
	@Test
	public void testForbitChangeDisplayAndDeprecated() throws Exception {
		testForbitChars(StringPrep.FORBID_CHANGE_DISPLAY_AND_DEPRECATED, (int)0x0340,(int)0x0341);
		testForbitChars(StringPrep.FORBID_CHANGE_DISPLAY_AND_DEPRECATED, (int)0x200E,(int)0x200F);
		testForbitChars(StringPrep.FORBID_CHANGE_DISPLAY_AND_DEPRECATED, (int)0x202A,(int)0x202E);
		testForbitChars(StringPrep.FORBID_CHANGE_DISPLAY_AND_DEPRECATED, (int)0x206A,(int)0x206F);
	}
	
	/** 5.9 Tagging characters */
	@Test
	public void testForbitTagging() throws Exception {
		testForbitChars(StringPrep.FORBID_TAGGING, (int)0xE0001);
		testForbitChars(StringPrep.FORBID_TAGGING, (int)0xE0020,(int)0xE007F);
	}
	
	/** A.1 Unassigned code points in Unicode 3.2 */
	@Test
	@Ignore("This implementation use newer Unicode (but require RFC of StringPrep Unicode 3.2?), see ELY-48")
	public void testForbitUnassigned() throws Exception {
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0221);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0234, (int)0x024F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x02AE, (int)0x02AF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x02EF, (int)0x02FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0350, (int)0x035F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0370, (int)0x0373);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0376, (int)0x0379);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x037B, (int)0x037D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x037F, (int)0x0383);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x038B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x038D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x03A2);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x03CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x03F7, (int)0x03FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0487);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x04CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x04F6, (int)0x04F7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x04FA, (int)0x04FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0510, (int)0x0530);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0557, (int)0x0558);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0560);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0588);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x058B, (int)0x0590);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x05A2);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x05BA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x05C5, (int)0x05CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x05EB, (int)0x05EF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x05F5, (int)0x060B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x060D, (int)0x061A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x061C, (int)0x061E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0620);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x063B, (int)0x063F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0656, (int)0x065F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x06EE, (int)0x06EF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x06FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x070E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x072D, (int)0x072F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x074B, (int)0x077F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x07B2, (int)0x0900);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0904);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x093A, (int)0x093B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x094E, (int)0x094F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0955, (int)0x0957);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0971, (int)0x0980);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0984);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x098D, (int)0x098E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0991, (int)0x0992);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09A9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09B1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09B3, (int)0x09B5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09BA, (int)0x09BB);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09BD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09C5, (int)0x09C6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09C9, (int)0x09CA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09CE, (int)0x09D6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09D8, (int)0x09DB);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09DE);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09E4, (int)0x09E5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x09FB, (int)0x0A01);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A03, (int)0x0A04);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A0B, (int)0x0A0E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A11, (int)0x0A12);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A29);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A31);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A34);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A37);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A3A, (int)0x0A3B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A3D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A43, (int)0x0A46);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A49, (int)0x0A4A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A4E, (int)0x0A58);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A5D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A5F, (int)0x0A65);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A75, (int)0x0A80);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A84);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A8C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A8E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0A92);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AA9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AB1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AB4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0ABA, (int)0x0ABB);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AC6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0ACA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0ACE, (int)0x0ACF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AD1, (int)0x0ADF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AE1, (int)0x0AE5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0AF0, (int)0x0B00);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B04);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B0D, (int)0x0B0E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B11, (int)0x0B12);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B29);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B31);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B34, (int)0x0B35);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B3A, (int)0x0B3B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B44, (int)0x0B46);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B49, (int)0x0B4A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B4E, (int)0x0B55);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B58, (int)0x0B5B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B5E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B62, (int)0x0B65);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B71, (int)0x0B81);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B84);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B8B, (int)0x0B8D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B91);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B96, (int)0x0B98);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B9B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0B9D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BA0, (int)0x0BA2);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BA5, (int)0x0BA7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BAB, (int)0x0BAD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BB6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BBA, (int)0x0BBD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BC3, (int)0x0BC5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BC9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BCE, (int)0x0BD6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BD8, (int)0x0BE6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0BF3, (int)0x0C00);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C04);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C0D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C11);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C29);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C34);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C3A, (int)0x0C3D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C45);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C49);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C4E, (int)0x0C54);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C57, (int)0x0C5F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C62, (int)0x0C65);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C70, (int)0x0C81);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C84);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C8D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0C91);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CA9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CB4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CBA, (int)0x0CBD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CC5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CC9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CCE, (int)0x0CD4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CD7, (int)0x0CDD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CDF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CE2, (int)0x0CE5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0CF0, (int)0x0D01);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D04);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D0D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D11);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D29);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D3A, (int)0x0D3D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D44, (int)0x0D45);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D49);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D4E, (int)0x0D56);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D58, (int)0x0D5F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D62, (int)0x0D65);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D70, (int)0x0D81);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D84);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0D97, (int)0x0D99);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DB2);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DBC);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DBE, (int)0x0DBF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DC7, (int)0x0DC9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DCB, (int)0x0DCE);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DD5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DD7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DE0, (int)0x0DF1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0DF5, (int)0x0E00);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E3B, (int)0x0E3E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E5C, (int)0x0E80);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E83);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E85, (int)0x0E86);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E89);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E8B, (int)0x0E8C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E8E, (int)0x0E93);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0E98);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EA0);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EA4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EA6);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EA8, (int)0x0EA9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EAC);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EBA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EBE, (int)0x0EBF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EC5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EC7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0ECE, (int)0x0ECF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EDA, (int)0x0EDB);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0EDE, (int)0x0EFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0F48);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0F6B, (int)0x0F70);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0F8C, (int)0x0F8F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0F98);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0FBD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0FCD, (int)0x0FCE);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x0FD0, (int)0x0FFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1022);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1028);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x102B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1033, (int)0x1035);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x103A, (int)0x103F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x105A, (int)0x109F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10C6, (int)0x10CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10F9, (int)0x10FA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10FC, (int)0x10FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x115A, (int)0x115E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x11A3, (int)0x11A7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x11FA, (int)0x11FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1207);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1247);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1249);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x124E, (int)0x124F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1257);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1259);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x125E, (int)0x125F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1287);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1289);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x128E, (int)0x128F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12AF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12B1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12B6, (int)0x12B7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12BF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12C1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12C6, (int)0x12C7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12D7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x12EF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x130F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1311);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1316, (int)0x1317);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x131F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1347);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x135B, (int)0x1360);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x137D, (int)0x139F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x13F5, (int)0x1400);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1677, (int)0x167F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x169D, (int)0x169F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x16F1, (int)0x16FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x170D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1715, (int)0x171F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1737, (int)0x173F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1754, (int)0x175F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x176D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1771);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1774, (int)0x177F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x17DD, (int)0x17DF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x17EA, (int)0x17FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x180F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x181A, (int)0x181F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1878, (int)0x187F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x18AA, (int)0x1DFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1E9C, (int)0x1E9F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1EFA, (int)0x1EFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F16, (int)0x1F17);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F1E, (int)0x1F1F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F46, (int)0x1F47);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F4E, (int)0x1F4F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F58);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F5A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F5C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F5E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1F7E, (int)0x1F7F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FB5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FC5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FD4, (int)0x1FD5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FDC);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FF0, (int)0x1FF1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FF5);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1FFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2053, (int)0x2056);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2058, (int)0x205E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2064, (int)0x2069);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2072, (int)0x2073);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x208F, (int)0x209F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x20B2, (int)0x20CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x20EB, (int)0x20FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x213B, (int)0x213C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x214C, (int)0x2152);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2184, (int)0x218F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x23CF, (int)0x23FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2427, (int)0x243F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x244B, (int)0x245F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x24FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2614, (int)0x2615);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2618);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x267E, (int)0x267F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x268A, (int)0x2700);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2705);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x270A, (int)0x270B);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2728);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x274C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x274E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2753, (int)0x2755);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2757);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x275F, (int)0x2760);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2795, (int)0x2797);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x27B0);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x27BF, (int)0x27CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x27EC, (int)0x27EF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2B00, (int)0x2E7F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2E9A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2EF4, (int)0x2EFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2FD6, (int)0x2FEF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2FFC, (int)0x2FFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x3040);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x3097, (int)0x3098);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x3100, (int)0x3104);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x312D, (int)0x3130);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x318F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x31B8, (int)0x31EF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x321D, (int)0x321F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x3244, (int)0x3250);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x327C, (int)0x327E);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x32CC, (int)0x32CF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x32FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x3377, (int)0x337A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x33DE, (int)0x33DF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x33FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x4DB6, (int)0x4DFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x9FA6, (int)0x9FFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xA48D, (int)0xA48F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xA4C7, (int)0xABFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xD7A4, (int)0xD7FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFA2E, (int)0xFA2F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFA6B, (int)0xFAFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB07, (int)0xFB12);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB18, (int)0xFB1C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB37);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB3D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB3F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB42);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFB45);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFBB2, (int)0xFBD2);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFD40, (int)0xFD4F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFD90, (int)0xFD91);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFDC8, (int)0xFDCF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFDFD, (int)0xFDFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE10, (int)0xFE1F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE24, (int)0xFE2F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE47, (int)0xFE48);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE53);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE67);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE6C, (int)0xFE6F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFE75);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFEFD, (int)0xFEFE);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFF00);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFBF, (int)0xFFC1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFC8, (int)0xFFC9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFD0, (int)0xFFD1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFD8, (int)0xFFD9);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFDD, (int)0xFFDF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFE7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xFFEF, (int)0xFFF8);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10000, (int)0x102FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1031F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10324, (int)0x1032F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1034B, (int)0x103FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x10426, (int)0x10427);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1044E, (int)0x1CFFF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D0F6, (int)0x1D0FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D127, (int)0x1D129);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D1DE, (int)0x1D3FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D455);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D49D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4A0, (int)0x1D4A1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4A3, (int)0x1D4A4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4A7, (int)0x1D4A8);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4AD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4BA);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4BC);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4C1);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D4C4);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D506);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D50B, (int)0x1D50C);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D515);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D51D);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D53A);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D53F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D545);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D547, (int)0x1D549);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D551);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D6A4, (int)0x1D6A7);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D7CA, (int)0x1D7CD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x1D800, (int)0x1FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2A6D7, (int)0x2F7FF);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x2FA1E, (int)0x2FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x30000, (int)0x3FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x40000, (int)0x4FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x50000, (int)0x5FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x60000, (int)0x6FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x70000, (int)0x7FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x80000, (int)0x8FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0x90000, (int)0x9FFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xA0000, (int)0xAFFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xB0000, (int)0xBFFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xC0000, (int)0xCFFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xD0000, (int)0xDFFFD);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xE0000);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xE0002, (int)0xE001F);
		testForbitChars(StringPrep.FORBID_UNASSIGNED, (int)0xE0080, (int)0xEFFFD);
	}
	
	@Test
	public void testAllowAssigned() throws Exception {
		ByteStringBuilder b = new ByteStringBuilder();
		StringPrep.encode("a", b, StringPrep.FORBID_UNASSIGNED);
		StringPrep.encode("\u0438", b, StringPrep.FORBID_UNASSIGNED);
		StringPrep.encode("\u4F60", b, StringPrep.FORBID_UNASSIGNED);
		StringPrep.encode("\uD83C\uDCA1", b, StringPrep.FORBID_UNASSIGNED);
	}
	
	
	// ---------------------- helpers ----------------------
	
	private String codePointToString(int codePoint){
		ByteStringBuilder b = new ByteStringBuilder();
		b.appendUtf8Raw(codePoint);
		return new String(b.toArray());
	}
	
	private void testForbitChars(long profile, int codePoint) throws Exception {
		try{
			ByteStringBuilder b = new ByteStringBuilder();
			StringPrep.encode(codePointToString(codePoint), b, profile);
			throw new Exception("Not throwed IllegalArgumentException for "+Integer.toHexString(codePoint)+"!");
		}
		catch(IllegalArgumentException e){}
	}
	
	private void testForbitChars(long profile, char[] chars) throws Exception{
		for(char i : chars){
			testForbitChars(profile, (int)i);
		}
	}
	
	private void testForbitChars(long profile, int[] chars) throws Exception{
		for(int c : chars){
			testForbitChars(profile, c);
		}
	}
	
	private void testForbitChars(long profile, int from, int to) throws Exception{
		for(int i = (int)from; i <= to; i++){
			testForbitChars(profile, i);
		}
	}
	
	@Test
	public void testOwnCodePointToStringConversion() throws Exception {
		assertEquals("\uD800",codePointToString(0xD800));
		assertEquals("\uDBB6\uDC00",codePointToString(0xFD800));
	}
	
}
