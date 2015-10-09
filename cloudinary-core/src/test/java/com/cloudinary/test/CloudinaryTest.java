package com.cloudinary.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryTest {
	private static final String DEFAULT_ROOT_PATH = "http://res.cloudinary.com/test123/";
	private static final String DEFAULT_UPLOAD_PATH = DEFAULT_ROOT_PATH + "image/upload/";
    private static final String VIDEO_UPLOAD_PATH = DEFAULT_ROOT_PATH + "video/upload/";
	private Cloudinary cloudinary;

	@Rule
	public TestName currentTest = new TestName();

	@Before
	public void setUp() {
		System.out.println("Running " + this.getClass().getName() + "." + currentTest.getMethodName());
		this.cloudinary = new Cloudinary("cloudinary://a:b@test123?load_strategies=false");
	}

	@Test
	public void testCloudName() {
		// should use cloud_name from config
		String result = cloudinary.url().generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "test", result);
	}

	@Test
	public void testCloudNameOptions() {
		// should allow overriding cloud_name in options
		String result = cloudinary.url().cloudName("test321").generate("test");
		assertEquals("http://res.cloudinary.com/test321/image/upload/test", result);
	}

	@Test
	public void testSecureDistribution() {
		// should use default secure distribution if secure=TRUE
		String result = cloudinary.url().secure(true).generate("test");
		assertEquals("https://res.cloudinary.com/test123/image/upload/test", result);
	}

	@Test
	public void testSecureDistributionOverwrite() {
		// should allow overwriting secure distribution if secure=TRUE
		String result = cloudinary.url().secure(true).secureDistribution("something.else.com").generate("test");
		assertEquals("https://something.else.com/test123/image/upload/test", result);
	}

	@Test
	public void testSecureDistibution() {
		// should take secure distribution from config if secure=TRUE
		cloudinary.config.secureDistribution = "config.secure.distribution.com";
		String result = cloudinary.url().secure(true).generate("test");
		assertEquals("https://config.secure.distribution.com/test123/image/upload/test", result);
	}

	@Test
	public void testSecureAkamai() {
		// should default to akamai if secure is given with private_cdn and no
		// secure_distribution
		cloudinary.config.secure = true;
		cloudinary.config.privateCdn = true;
		String result = cloudinary.url().generate("test");
		assertEquals("https://test123-res.cloudinary.com/image/upload/test", result);
	}

	@Test
	public void testSecureNonAkamai() {
		// should not add cloud_name if private_cdn and secure non akamai
		// secure_distribution
		cloudinary.config.secure = true;
		cloudinary.config.privateCdn = true;
		cloudinary.config.secureDistribution = "something.cloudfront.net";
		String result = cloudinary.url().generate("test");
		assertEquals("https://something.cloudfront.net/image/upload/test", result);
	}

	@Test
	public void testHttpPrivateCdn() {
		// should not add cloud_name if private_cdn and not secure
		cloudinary.config.privateCdn = true;
		String result = cloudinary.url().generate("test");
		assertEquals("http://test123-res.cloudinary.com/image/upload/test", result);
	}

	@Test
	public void testFormat() {
		// should use format from options
		String result = cloudinary.url().format("jpg").generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "test.jpg", result);
	}

	@Test
	public void testCrop() {
		Transformation transformation = new Transformation().width(100).height(101);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "h_101,w_100/test", result);
		assertEquals("101", transformation.getHtmlHeight().toString());
		assertEquals("100", transformation.getHtmlWidth().toString());
		transformation = new Transformation().width(100).height(101).crop("crop");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "c_crop,h_101,w_100/test", result);
	}

	@Test
	public void testVariousOptions() {
		// should use x, y, radius, prefix, gravity and quality from options
		Transformation transformation = new Transformation().x(1).y(2).radius(3).gravity("center").quality(0.4).prefix("a");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "g_center,p_a,q_0.4,r_3,x_1,y_2/test", result);
	}

	@Test
	public void testTransformationSimple() {
		// should support named transformation
		Transformation transformation = new Transformation().named("blip");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "t_blip/test", result);
	}

	@Test
	public void testTransformationArray() {
		// should support array of named transformations
		Transformation transformation = new Transformation().named("blip", "blop");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "t_blip.blop/test", result);
	}

	@Test
	public void testBaseTransformations() {
		// should support base transformation
		Transformation transformation = new Transformation().x(100).y(100).crop("fill").chain().crop("crop").width(100);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals("100", transformation.getHtmlWidth().toString());
		assertEquals(DEFAULT_UPLOAD_PATH + "c_fill,x_100,y_100/c_crop,w_100/test", result);
	}

	@Test
	public void testBaseTransformationArray() {
		// should support array of base transformations
		Transformation transformation = new Transformation().x(100).y(100).width(200).crop("fill").chain().radius(10).chain().crop("crop").width(100);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals("100", transformation.getHtmlWidth().toString());
		assertEquals(DEFAULT_UPLOAD_PATH + "c_fill,w_200,x_100,y_100/r_10/c_crop,w_100/test", result);
	}

	@Test
	public void testNoEmptyTransformation() {
		// should not include empty transformations
		Transformation transformation = new Transformation().chain().x(100).y(100).crop("fill").chain();
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "c_fill,x_100,y_100/test", result);
	}

	@Test
	public void testType() {
		// should use type from options
		String result = cloudinary.url().type("facebook").generate("test");
		assertEquals("http://res.cloudinary.com/test123/image/facebook/test", result);
	}

	@Test
	public void testResourceType() {
		// should use resource_type from options
		String result = cloudinary.url().resourcType("raw").generate("test");
		assertEquals("http://res.cloudinary.com/test123/raw/upload/test", result);
	}

	@Test
	public void testIgnoreHttp() {
		// should ignore http links only if type is not given or is asset
		String result = cloudinary.url().generate("http://test");
		assertEquals("http://test", result);
		result = cloudinary.url().type("asset").generate("http://test");
		assertEquals("http://test", result);
		result = cloudinary.url().type("fetch").generate("http://test");
		assertEquals("http://res.cloudinary.com/test123/image/fetch/http://test", result);
	}

	@Test
	public void testFetch() {
		// should escape fetch urls
		String result = cloudinary.url().type("fetch").generate("http://blah.com/hello?a=b");
		assertEquals("http://res.cloudinary.com/test123/image/fetch/http://blah.com/hello%3Fa%3Db", result);
	}

	@Test
	public void testCname() {
		// should support external cname
		String result = cloudinary.url().cname("hello.com").generate("test");
		assertEquals("http://hello.com/test123/image/upload/test", result);
	}

	@Test
	public void testCnameSubdomain() {
		// should support external cname with cdn_subdomain on
		String result = cloudinary.url().cname("hello.com").cdnSubdomain(true).generate("test");
		assertEquals("http://a2.hello.com/test123/image/upload/test", result);
	}

	@Test
	public void testHttpEscape() {
		// should escape http urls
		String result = cloudinary.url().type("youtube").generate("http://www.youtube.com/watch?v=d9NF2edxy-M");
		assertEquals("http://res.cloudinary.com/test123/image/youtube/http://www.youtube.com/watch%3Fv%3Dd9NF2edxy-M", result);
	}

	@Test
	public void testBackground() {
		// should support background
		Transformation transformation = new Transformation().background("red");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "b_red/test", result);
		transformation = new Transformation().background("#112233");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "b_rgb:112233/test", result);
	}

	@Test
	public void testDefaultImage() {
		// should support default_image
		Transformation transformation = new Transformation().defaultImage("default");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "d_default/test", result);
	}

	@Test
	public void testAngle() {
		// should support angle
		Transformation transformation = new Transformation().angle(12);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "a_12/test", result);
		transformation = new Transformation().angle("exif", "12");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "a_exif.12/test", result);
	}

	@Test
	public void testOverlay() {
		// should support overlay
		Transformation transformation = new Transformation().overlay("text:hello");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "l_text:hello/test", result);
		// should not pass width/height to html if overlay
		transformation = new Transformation().overlay("text:hello").width(100).height(100);
		result = cloudinary.url().transformation(transformation).generate("test");
		assertNull(transformation.getHtmlHeight());
		assertNull(transformation.getHtmlWidth());
		assertEquals(DEFAULT_UPLOAD_PATH + "h_100,l_text:hello,w_100/test", result);
	}

	@Test
	public void testUnderlay() {
		Transformation transformation = new Transformation().underlay("text:hello");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "u_text:hello/test", result);
		// should not pass width/height to html if underlay
		transformation = new Transformation().underlay("text:hello").width(100).height(100);
		result = cloudinary.url().transformation(transformation).generate("test");
		assertNull(transformation.getHtmlHeight());
		assertNull(transformation.getHtmlWidth());
		assertEquals(DEFAULT_UPLOAD_PATH + "h_100,u_text:hello,w_100/test", result);
	}

	@Test
	public void testFetchFormat() {
		// should support format for fetch urls
		String result = cloudinary.url().format("jpg").type("fetch").generate("http://cloudinary.com/images/old_logo.png");
		assertEquals("http://res.cloudinary.com/test123/image/fetch/f_jpg/http://cloudinary.com/images/old_logo.png", result);
	}

	@Test
	public void testEffect() {
		// should support effect
		Transformation transformation = new Transformation().effect("sepia");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "e_sepia/test", result);
	}

	@Test
	public void testEffectWithParam() {
		// should support effect with param
		Transformation transformation = new Transformation().effect("sepia", 10);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "e_sepia:10/test", result);
	}

	@Test
	public void testDensity() {
		// should support density
		Transformation transformation = new Transformation().density(150);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "dn_150/test", result);
	}

	@Test
	public void testPage() {
		// should support page
		Transformation transformation = new Transformation().page(5);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "pg_5/test", result);
	}

	@Test
	public void testBorder() {
		// should support border
		Transformation transformation = new Transformation().border(5, "black");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "bo_5px_solid_black/test", result);
		transformation = new Transformation().border(5, "#ffaabbdd");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "bo_5px_solid_rgb:ffaabbdd/test", result);
		transformation = new Transformation().border("1px_solid_blue");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "bo_1px_solid_blue/test", result);
	}

	@Test
	public void testFlags() {
		// should support flags
		Transformation transformation = new Transformation().flags("abc");
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "fl_abc/test", result);
		transformation = new Transformation().flags("abc", "def");
		result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "fl_abc.def/test", result);
	}

	@Test
	public void testOpacity() {
		// should support opacity
		Transformation transformation = new Transformation().opacity(50);
		String result = cloudinary.url().transformation(transformation).generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "o_50/test", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testImageTag() {
		Transformation transformation = new Transformation().width(100).height(101).crop("crop");
		String result = cloudinary.url().transformation(transformation).imageTag("test", ObjectUtils.asMap("alt", "my image"));
		assertEquals("<img src='http://res.cloudinary.com/test123/image/upload/c_crop,h_101,w_100/test' alt='my image' height='101' width='100'/>", result);
		transformation = new Transformation().width(0.9).height(0.9).crop("crop").responsiveWidth(true);
		result = cloudinary.url().transformation(transformation).imageTag("test", ObjectUtils.asMap("alt", "my image"));
		assertEquals(
				"<img alt='my image' class='cld-responsive' data-src='http://res.cloudinary.com/test123/image/upload/c_crop,h_0.9,w_0.9/c_limit,w_auto/test'/>",
				result);
		result = cloudinary.url().transformation(transformation).imageTag("test", ObjectUtils.asMap("alt", "my image", "class", "extra"));
		assertEquals(
				"<img alt='my image' class='extra cld-responsive' data-src='http://res.cloudinary.com/test123/image/upload/c_crop,h_0.9,w_0.9/c_limit,w_auto/test'/>",
				result);
		transformation = new Transformation().width("auto").crop("crop");
		result = cloudinary.url().transformation(transformation).imageTag("test", ObjectUtils.asMap("alt", "my image", "responsive_placeholder", "blank"));
		assertEquals(
				"<img src='data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7' alt='my image' class='cld-responsive' data-src='http://res.cloudinary.com/test123/image/upload/c_crop,w_auto/test'/>",
				result);
		result = cloudinary.url().transformation(transformation).imageTag("test", ObjectUtils.asMap("alt", "my image", "responsive_placeholder", "other.gif"));
		assertEquals(
				"<img src='other.gif' alt='my image' class='cld-responsive' data-src='http://res.cloudinary.com/test123/image/upload/c_crop,w_auto/test'/>",
				result);
	}

	@Test
	public void testFolders() {
		// should add version if public_id contains /
		String result = cloudinary.url().generate("folder/test");
		assertEquals(DEFAULT_UPLOAD_PATH + "v1/folder/test", result);
		result = cloudinary.url().version(123).generate("folder/test");
		assertEquals(DEFAULT_UPLOAD_PATH + "v123/folder/test", result);
	}

	@Test
	public void testFoldersWithVersion() {
		// should not add version if public_id contains version already
		String result = cloudinary.url().generate("v1234/test");
		assertEquals(DEFAULT_UPLOAD_PATH + "v1234/test", result);
	}

	@Test
	public void testShorten() {
		// should allow to shorted image/upload urls
		String result = cloudinary.url().shorten(true).generate("test");
		assertEquals("http://res.cloudinary.com/test123/iu/test", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPrivateDownload() throws Exception {
		String url = cloudinary.privateDownload("imgÿ=&é", "jpg", ObjectUtils.emptyMap());
		URI uri = new URI(url);
		Map<String, String> parameters = getUrlParameters(uri);
		assertEquals("imgÿ=&é", parameters.get("public_id"));
		assertEquals("jpg", parameters.get("format"));
		assertEquals("a", parameters.get("api_key"));
		assertEquals("/v1_1/test123/image/download", uri.getPath());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testZipDownload() throws Exception {
		String url = cloudinary.zipDownload("ttag", ObjectUtils.emptyMap());
		URI uri = new URI(url);
		Map<String, String> parameters = getUrlParameters(uri);
		assertEquals("ttag", parameters.get("tag"));
		assertEquals("a", parameters.get("api_key"));
		assertEquals("/v1_1/test123/image/download_tag.zip", uri.getPath());
	}

	@Test
	public void testSpriteCss() {
		String result = cloudinary.url().generateSpriteCss("test");
		assertEquals("http://res.cloudinary.com/test123/image/sprite/test.css", result);
		result = cloudinary.url().generateSpriteCss("test.css");
		assertEquals("http://res.cloudinary.com/test123/image/sprite/test.css", result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEscapePublicId() {
		// should escape public_ids
		Map<String, String> tests = ObjectUtils.asMap("a b", "a%20b", "a+b", "a%2Bb", "a%20b", "a%20b", "a-b", "a-b", "a??b", "a%3F%3Fb");
		for (Map.Entry<String, String> entry : tests.entrySet()) {
			String result = cloudinary.url().generate(entry.getKey());
			assertEquals(DEFAULT_UPLOAD_PATH + "" + entry.getValue(), result);
		}
	}

	@Test
	public void testSignedUrl() {
		// should correctly sign a url
		String expected = DEFAULT_UPLOAD_PATH + "s--Ai4Znfl3--/c_crop,h_20,w_10/v1234/image.jpg";
		String actual = cloudinary.url().version(1234).transformation(new Transformation().crop("crop").width(10).height(20)).signed(true)
				.generate("image.jpg");
		assertEquals(expected, actual);

		expected = DEFAULT_UPLOAD_PATH + "s----SjmNDA--/v1234/image.jpg";
		actual = cloudinary.url().version(1234).signed(true).generate("image.jpg");
		assertEquals(expected, actual);

		expected = DEFAULT_UPLOAD_PATH + "s--Ai4Znfl3--/c_crop,h_20,w_10/image.jpg";
		actual = cloudinary.url().transformation(new Transformation().crop("crop").width(10).height(20)).signed(true).generate("image.jpg");
		assertEquals(expected, actual);
	}

	@Test
	public void testResponsiveWidth() {
		// should support responsive width
		Transformation trans = new Transformation().width(100).height(100).crop("crop").responsiveWidth(true);
		String result = cloudinary.url().transformation(trans).generate("test");
		assertTrue(trans.isResponsive());
		assertEquals(DEFAULT_UPLOAD_PATH + "c_crop,h_100,w_100/c_limit,w_auto/test", result);
		Transformation.setResponsiveWidthTransformation(ObjectUtils.asMap("width", "auto", "crop", "pad"));
		trans = new Transformation().width(100).height(100).crop("crop").responsiveWidth(true);
		result = cloudinary.url().transformation(trans).generate("test");
		assertTrue(trans.isResponsive());
		assertEquals(DEFAULT_UPLOAD_PATH + "c_crop,h_100,w_100/c_pad,w_auto/test", result);
		Transformation.setResponsiveWidthTransformation(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisallowUrlSuffixInSharedDistribution() {
		cloudinary.url().suffix("hello").generate("test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisallowUrlSuffixInNonUploadTypes() {
		cloudinary.url().suffix("hello").privateCdn(true).type("facebook").generate("test");

	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisallowUrlSuffixWithSlash() {
		cloudinary.url().suffix("hello/world").privateCdn(true).generate("test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisallowUrlSuffixWithDot() {
		cloudinary.url().suffix("hello.world").privateCdn(true).generate("test");
	}

	@Test
	public void testSupportUrlSuffixForPrivateCdn() {
		String actual = cloudinary.url().suffix("hello").privateCdn(true).generate("test");
		assertEquals("http://test123-res.cloudinary.com/images/test/hello", actual);

		actual = cloudinary.url().suffix("hello").privateCdn(true).transformation(new Transformation().angle(0)).generate("test");
		assertEquals("http://test123-res.cloudinary.com/images/a_0/test/hello", actual);

	}

	@Test
	public void testPutFormatAfterUrlSuffix() {
		String actual = cloudinary.url().suffix("hello").privateCdn(true).format("jpg").generate("test");
		assertEquals("http://test123-res.cloudinary.com/images/test/hello.jpg", actual);
	}

	@Test
	public void testNotSignTheUrlSuffix() {

		Pattern pattern = Pattern.compile("s--[0-9A-Za-z_-]{8}--");
		String url = cloudinary.url().format("jpg").signed(true).generate("test");
		Matcher matcher = pattern.matcher(url);
		matcher.find();
		String expectedSignature = url.substring(matcher.start(), matcher.end());

		String actual = cloudinary.url().format("jpg").privateCdn(true).signed(true).suffix("hello").generate("test");
		assertEquals("http://test123-res.cloudinary.com/images/" + expectedSignature + "/test/hello.jpg", actual);

		url = cloudinary.url().format("jpg").signed(true).transformation(new Transformation().angle(0)).generate("test");
		matcher = pattern.matcher(url);
		matcher.find();
		expectedSignature = url.substring(matcher.start(), matcher.end());

		actual = cloudinary.url().format("jpg").privateCdn(true).signed(true).suffix("hello").transformation(new Transformation().angle(0)).generate("test");

		assertEquals("http://test123-res.cloudinary.com/images/" + expectedSignature + "/a_0/test/hello.jpg", actual);
	}

	@Test
	public void testSupportUrlSuffixForRawUploads() {
		String actual = cloudinary.url().suffix("hello").privateCdn(true).resourceType("raw").generate("test");
		assertEquals("http://test123-res.cloudinary.com/files/test/hello", actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisllowUseRootPathInSharedDistribution() {
		cloudinary.url().useRootPath(true).generate("test");
	}

	@Test
	public void testSupportUseRootPathForPrivateCdn() {
		String actual = cloudinary.url().privateCdn(true).useRootPath(true).generate("test");
		assertEquals("http://test123-res.cloudinary.com/test", actual);

		actual = cloudinary.url().privateCdn(true).transformation(new Transformation().angle(0)).useRootPath(true).generate("test");
		assertEquals("http://test123-res.cloudinary.com/a_0/test", actual);
	}

	@Test
	public void testSupportUseRootPathTogetherWithUrlSuffixForPrivateCdn() {

		String actual = cloudinary.url().privateCdn(true).suffix("hello").useRootPath(true).generate("test");
		assertEquals("http://test123-res.cloudinary.com/test/hello", actual);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisllowUseRootPathIfNotImageUploadForFacebook() {
		cloudinary.url().useRootPath(true).privateCdn(true).type("facebook").generate("test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDisllowUseRootPathIfNotImageUploadForRaw() {
		cloudinary.url().useRootPath(true).privateCdn(true).resourceType("raw").generate("test");
	}
	
	@Test
	public void testVideoCodec() {
		// should support a string value
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().videoCodec("auto"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vc_auto/video_id", actual);
		// should support a hash value
		actual = cloudinary.url().resourceType("video")
				.transformation(
						new Transformation().videoCodec(ObjectUtils.asMap("codec", "h264", "profile", "basic", "level","3.1"))
					).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vc_h264:basic:3.1/video_id", actual);
	}

	@Test
	public void testAudioCodec(){
		// should support a string value
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().audioCodec("acc")).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "ac_acc/video_id", actual);
	}

	@Test
	public void testBitRate() {
		// should support a numeric value
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().bitRate(2048))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "br_2048/video_id", actual);
		// should support a string value
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().bitRate("44k"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "br_44k/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().bitRate("1m"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "br_1m/video_id", actual);

	}

	@Test
	public void testAudioFrequency() {
		// should support an integer value
		String actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().audioFrequency(44100)).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "af_44100/video_id", actual);
		// should support a string value
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().audioFrequency("44100"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "af_44100/video_id", actual);
	}

	@Test
	public void testVideoSampling() {
		String actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().videoSamplingFrames(20)).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vs_20/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().videoSamplingSeconds(20))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vs_20s/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().videoSamplingSeconds(20.0))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vs_20.0s/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().videoSampling("2.3s"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "vs_2.3s/video_id", actual);
	}

	@Test
	public void testStartOffset() {
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().startOffset(2.63))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "so_2.63/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().startOffset("2.63p"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "so_2.63p/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().startOffset("2.63%"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "so_2.63p/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().startOffsetPercent(2.63))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "so_2.63p/video_id", actual);
	}
	
	@Test
	public void testDuration() {
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().duration(2.63))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "du_2.63/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().duration("2.63p"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "du_2.63p/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().duration("2.63%"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "du_2.63p/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().durationPercent(2.63))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "du_2.63p/video_id", actual);
	}

	@Test
	public void testOffset() {

		String actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset("2.66..3.21")).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_3.21,so_2.66/video_id", actual);
		actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset(new float[] { 2.67f, 3.22f })).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_3.22,so_2.67/video_id", actual);
		actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset(new double[] { 2.67, 3.22 })).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_3.22,so_2.67/video_id", actual);
		actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset(new String[] { "35%", "70%" })).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_70p,so_35p/video_id", actual);
		actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset(new String[] { "36p", "71p" })).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_71p,so_36p/video_id", actual);
		actual = cloudinary.url().resourceType("video")
				.transformation(new Transformation().offset(new String[] { "35.5p", "70.5p" })).generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "eo_70.5p,so_35.5p/video_id", actual);

	}
	
	@Test
	public void testZoom() {
		String actual = cloudinary.url().resourceType("video").transformation(new Transformation().zoom("1.5"))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "z_1.5/video_id", actual);
		actual = cloudinary.url().resourceType("video").transformation(new Transformation().zoom(1.5))
				.generate("video_id");
		assertEquals(VIDEO_UPLOAD_PATH + "z_1.5/video_id", actual);
	}
	
	@Test
	public void testUtils() {
		assertEquals(ObjectUtils.asBoolean(true, null), true);
		assertEquals(ObjectUtils.asBoolean(false, null), false);
	}
	
	@Test
	public void testVideoTag() {
		String expectedUrl = VIDEO_UPLOAD_PATH + "movie";
		String expectedTag = "<video poster='%s.jpg'>" + "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" 
				+ "<source src='%s.ogv' type='video/ogg'>" 
				+ "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl);
		assertEquals(expectedTag, cloudinary.url().videoTag("movie", ObjectUtils.emptyMap()));
		assertEquals(expectedTag, cloudinary.url().publicId("movie").videoTag());
	}

	@Test
	public void testVideoTagWithAttributes() {
		Map attributes = ObjectUtils.asMap(
				"autoplay", true, 
				"controls", null, 
				"loop", null, 
				"muted", "true", 
				"preload", null, 
				"style", "border: 1px");
		String expectedUrl = VIDEO_UPLOAD_PATH + "movie";
		String expectedTag = "<video autoplay='true' controls loop muted='true' poster='%s.jpg' preload style='border: 1px'>"
				+ "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>"
				+ "<source src='%s.ogv' type='video/ogg'>" + "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl);
		assertEquals(expectedTag, cloudinary.url().videoTag("movie", attributes));
	}

	@Test
	public void testVideoTagWithTransformation() {
		Transformation transformation = new Transformation().videoCodec(ObjectUtils.asMap("codec", "h264"))
				.audioCodec("acc").startOffset(3);
		String expectedUrl = VIDEO_UPLOAD_PATH + "ac_acc,so_3.0,vc_h264/movie";
		String expectedTag = "<video height='100' poster='%s.jpg' src='%s.mp4' width='200'></video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl);
		String actualTag = cloudinary.url().transformation(transformation).sourceTypes(new String[] { "mp4" })
				.videoTag("movie", ObjectUtils.asMap("html_height", "100", "html_width", "200"));
		assertEquals(expectedTag, actualTag);

		expectedTag = "<video height='100' poster='%s.jpg' width='200'>" 
				+ "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" 
				+ "<source src='%s.ogv' type='video/ogg'>" 
				+ "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl);
		actualTag = cloudinary.url().transformation(transformation)
				.videoTag("movie", ObjectUtils.asMap("html_height", "100", "html_width", "200"));
		assertEquals(expectedTag, actualTag);

		transformation.width(250);
		expectedUrl = VIDEO_UPLOAD_PATH + "ac_acc,so_3.0,vc_h264,w_250/movie";
		expectedTag = "<video poster='%s.jpg' width='250'>" 
				+ "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" 
				+ "<source src='%s.ogv' type='video/ogg'>" 
				+ "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl);
		actualTag = cloudinary.url().transformation(transformation)
				.videoTag("movie", ObjectUtils.asMap());
		assertEquals(expectedTag, actualTag);

		transformation.crop("fit");
		expectedUrl = VIDEO_UPLOAD_PATH + "ac_acc,c_fit,so_3.0,vc_h264,w_250/movie";
		expectedTag = "<video poster='%s.jpg'>" 
				+ "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>"
				+ "<source src='%s.ogv' type='video/ogg'>" 
				+ "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl);
		actualTag = cloudinary.url().transformation(transformation)
				.videoTag("movie", ObjectUtils.asMap());
		assertEquals(expectedTag, actualTag);
	}

	@Test
	public void testVideoTagWithFallback() {
		String expectedUrl = VIDEO_UPLOAD_PATH + "movie";
		String fallback = "<span id='spanid'>Cannot display video</span>";
		String expectedTag = "<video poster='%s.jpg' src='%s.mp4'>%s</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, fallback);
		String actualTag = cloudinary.url().fallbackContent(fallback).sourceTypes(new String[] { "mp4" })
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);

		expectedTag = "<video poster='%s.jpg'>" + "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" + "<source src='%s.ogv' type='video/ogg'>%s" + "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl, expectedUrl, fallback);
		actualTag = cloudinary.url().fallbackContent(fallback).videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
	}

	@Test
	public void testVideoTagWithSourceTypes() {
		String expectedUrl = VIDEO_UPLOAD_PATH + "movie";
		String expectedTag = "<video poster='%s.jpg'>" + "<source src='%s.ogv' type='video/ogg'>"
				+ "<source src='%s.mp4' type='video/mp4'>" + "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedUrl);
		String actualTag = cloudinary.url().sourceTypes(new String[] { "ogv", "mp4" })
				.videoTag("movie.mp4", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
	}

	@Test
	public void testVideoTagWithSourceTransformation() {
		String expectedUrl = VIDEO_UPLOAD_PATH + "q_50/w_100/movie";
		String expectedOgvUrl = VIDEO_UPLOAD_PATH + "q_50/w_100/q_70/movie";
		String expectedMp4Url = VIDEO_UPLOAD_PATH + "q_50/w_100/q_30/movie";
		String expectedTag = "<video poster='%s.jpg' width='100'>" 
				+ "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" 
				+ "<source src='%s.ogv' type='video/ogg'>"
				+ "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedMp4Url, expectedOgvUrl);
		String actualTag = cloudinary.url().transformation(new Transformation().quality(50).chain().width(100))
				.sourceTransformationFor("mp4", new Transformation().quality(30))
				.sourceTransformationFor("ogv", new Transformation().quality(70))
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);

		expectedTag = "<video poster='%s.jpg' width='100'>" + "<source src='%s.webm' type='video/webm'>"
				+ "<source src='%s.mp4' type='video/mp4'>" + "</video>";
		expectedTag = String.format(expectedTag, expectedUrl, expectedUrl, expectedMp4Url);
		actualTag = cloudinary.url().transformation(new Transformation().quality(50).chain().width(100))
				.sourceTransformationFor("mp4", new Transformation().quality(30))
				.sourceTransformationFor("ogv", new Transformation().quality(70))
				.sourceTypes(new String[] { "webm", "mp4" }).videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
	}
	
	@Test
	public void testVideoTagWithPoster() {
		String expectedUrl = VIDEO_UPLOAD_PATH + "movie";
		String posterUrl = "http://image/somewhere.jpg";
		String expectedTag = "<video poster='%s' src='%s.mp4'></video>";
		expectedTag = String.format(expectedTag, posterUrl, expectedUrl);
		String actualTag = cloudinary.url().sourceTypes(new String[]{"mp4"}).poster(posterUrl)
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
		
		posterUrl = VIDEO_UPLOAD_PATH + "g_north/movie.jpg";
		expectedTag = "<video poster='%s' src='%s.mp4'></video>";
		expectedTag = String.format(expectedTag, posterUrl, expectedUrl);
		actualTag = cloudinary.url().sourceTypes(new String[]{"mp4"})
				.poster(new Transformation().gravity("north"))
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
		
		posterUrl = DEFAULT_UPLOAD_PATH + "g_north/my_poster.jpg";
		expectedTag = "<video poster='%s' src='%s.mp4'></video>";
		expectedTag = String.format(expectedTag, posterUrl, expectedUrl);
		actualTag = cloudinary.url().sourceTypes(new String[]{"mp4"})
				.poster(cloudinary.url()
							.publicId("my_poster")
							.format("jpg")
							.transformation(new Transformation().gravity("north")))
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
		
		expectedTag = "<video src='%s.mp4'></video>";
		expectedTag = String.format(expectedTag, expectedUrl);
		actualTag = cloudinary.url().sourceTypes(new String[]{"mp4"})
				.poster(null)
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);
		
		actualTag = cloudinary.url().sourceTypes(new String[]{"mp4"})
				.poster(false)
				.videoTag("movie", ObjectUtils.emptyMap());
		assertEquals(expectedTag, actualTag);

	}
	
	@Test
	public void testAspectRatio() {
		String actual = cloudinary.url().transformation(new Transformation().aspectRatio("1.5"))
				.generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "ar_1.5/test", actual);
		actual = cloudinary.url().transformation(new Transformation().aspectRatio(1.5))
				.generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "ar_1.5/test", actual);
		actual = cloudinary.url().transformation(new Transformation().aspectRatio(3,2))
				.generate("test");
		assertEquals(DEFAULT_UPLOAD_PATH + "ar_3:2/test", actual);
	}

	public static Map<String, String> getUrlParameters(URI uri) throws UnsupportedEncodingException {
		Map<String, String> params = new HashMap<String, String>();
		for (String param : uri.getRawQuery().split("&")) {
			String pair[] = param.split("=");
			String key = URLDecoder.decode(pair[0], "UTF-8");
			String value = "";
			if (pair.length > 1) {
				value = URLDecoder.decode(pair[1], "UTF-8");
			}
			params.put(new String(key), new String(value));
		}
		return params;
	}
}
