package org.exoplatform.selenium;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Transforms the Selenium IDE recorded html to a corresponding JUnit class.
 */
public class SeleniumTestCaseGenerator {

	private static final String TEST_PATTERN = "Test_";

	private static int testsNumber = 0;
	private static int testsSuitesNumber = 0;

	private String basedir;
	private String outputdir;

	public static void main(String[] args) throws Exception {
		SeleniumTestCaseGenerator seleneseToJavaBuilder = null;
		if (args.length == 1) {
			seleneseToJavaBuilder = new SeleniumTestCaseGenerator(args[0], args[0]);
		} else if (args.length == 2) {
			seleneseToJavaBuilder = new SeleniumTestCaseGenerator(args[0], args[1]);
		} else {
			throw new IllegalArgumentException("SeleneseToJavaBuilder suitePath [Target] [TestSpeed]");
		}
		seleneseToJavaBuilder.run();
		System.out
		      .println("Done : " + testsNumber + " tests generated, " + testsSuitesNumber + " tests suites generated");
	}

	public SeleniumTestCaseGenerator(String basedir, String outputdir) throws Exception {
		this.basedir = basedir;
		this.outputdir = outputdir;
	}

	public void run() throws Exception {
		generate(new File(basedir), "");
	}

	public void generate(File directoryFile, String path) throws Exception {
		File[] files = directoryFile.listFiles();
		String testPackagePath = path.replaceAll(replaceSeparatorPattern, ".");
		for (int j = 0; j < files.length; j++) {
			File file = files[j];
			String filePath = path.length()>0 ? path + File.separator + file.getName() : file.getName();
			if (file.isDirectory() && !file.getName().startsWith(".")) {
				generate(new File(directoryFile, file.getName()), filePath);
			} else if (file.getName().endsWith(".html") && file.getName().startsWith(TEST_PATTERN)) {
				generateFile(filePath, testPackagePath);
			}
		}
	}

	public void generateFile(String seleniumFile, String testPackagePath) throws Exception {
		seleniumFile = seleniumFile.replaceAll(replaceSeparatorPattern, "/");

		int x = seleniumFile.lastIndexOf("/");
		int y = seleniumFile.indexOf(".");
		String testName = seleniumFile.substring(x + 1, y);
		String testMethodName = "test" + testName.substring(5);
		String testFileName = outputdir + "/" + seleniumFile.substring(0, y) + ".java";

		// Write each Test in one file
		StringBuffer sb = new StringBuffer();

sb.append("package " + testPackagePath + ";\n\n");
sb.append("import static org.junit.Assert.*;\n\n");
sb.append("import org.openqa.selenium.htmlunit.*;\n");
sb.append("import org.openqa.selenium.firefox.*;\n");
sb.append("import org.openqa.selenium.chrome.*;\n");
sb.append("import org.openqa.selenium.ie.*; \n\n");

sb.append("import com.thoughtworks.selenium.Selenium;\n");
sb.append("import org.openqa.selenium.*;\n");
sb.append("import org.junit.*;\n\n");


sb.append("public class " + testName + " {\n\n");


		// setSpeed & setUp
		appendCommonMethods(sb);

		// testMethod
		appendTest(sb, seleniumFile, testName, testMethodName);
		sb.append("}\n");

		String content = sb.toString();
		writeFile(testFileName, content);

		testsNumber++;
	}

	private void appendCommonMethods(StringBuffer sb) {
                sb.append("WebDriver driver;\n");
		sb.append("Selenium selenium;\n\n");

		sb.append("public String timeout = \"30000\";\n");
		sb.append("public int timeoutSecInt = 30;\n\n");

//sb.append("public void setSpeed() {\n  selenium.setSpeed(speed);\n}\n\n");

sb.append("@Before\n");
sb.append(" public void startSelenium() {\n");
	sb.append("  driver = new FirefoxDriver();\n");
	sb.append(" selenium = new WebDriverBackedSelenium(driver, \"http://192.168.3.7:18180\");\n");
	sb.append("}\n\n");

sb.append("@After\n");
sb.append("  public void stopSelenium() {\n");
	sb.append("  driver.close();\n}\n\n");
	}

	private void appendTest(StringBuffer sb, String scriptFile, String testName, String testMethodName) throws Exception {

		String xml = FileUtils.readFileToString(new File(basedir + "/" + scriptFile), "UTF-8");

		System.out.println("* " + basedir + "/" + scriptFile);

		// Method
sb.append("@Test\n");
		sb.append("public void " + testMethodName + "() throws InterruptedException {\n");
		//sb.append("  setSpeed();\n");

		if ((xml.indexOf("<title>" + testName + "</title>") == -1)
		      || (xml.indexOf("colspan=\"3\">" + testName + "</td>") == -1)) {
			System.out.println("[WARN] The test name inside the file should be the file name.");
		}

		if (xml.indexOf("&quot;") != -1) {
			xml = replace(xml, "&quot;", "\"");
			//writeFile(outputdir + "/" + scriptFile, xml);
		}

		int x = xml.indexOf("<tbody>");
		int y = xml.indexOf("</tbody>");

		xml = xml.substring(x, y + 8);

		x = 0;
		y = 0;
		int count = 0;

		while (true) {
			x = xml.indexOf("<tr>", x);
			y = xml.indexOf("\n</tr>", x);
			if ((x == -1) || (y == -1)) {
				break;
			}

			x += 6;
			y++;
			count++;

			String step = xml.substring(x, y);
			String[] params = getParams(step);

			String param1 = params[0];
			String param2 = fixParam(params[1]);
//variables management
			param2 = param2.replaceAll("\\$\\{([a-z0-9A-Z]*)\\}", "\" + $1 + \"");
			param2 = param2.replaceAll("storedVars\\['([a-z0-9A-Z]*)'\\]", "'\" + $1 + \"'");
			String param3 = fixParam(params[2]);
//variables management
			param3 = param3.replaceAll("\\$\\{([a-z0-9A-Z]*)\\}", "\" + $1 + \"");
			param3 = param3.replaceAll("storedVars\\['([a-z0-9A-Z]*)'\\]", "'\" + $1 + \"'");

			sb.append("\n  //" + count + ": " + param1 + " | " + param2 + " | " + param3 + "\n");
			if (param1.equals("open")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");
                        } else if (param1.equals("click")) {
                                sb.append("selenium.mouseOver");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");

				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");
			} else if (param1.equals("clickAndWait")) {
                                sb.append("selenium.mouseOver");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");

				sb.append("selenium.click(\"");
				sb.append(param2);
				sb.append("\");\n");
                                //sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("waitForElementPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n if (selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\")) \nbreak; }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("uncheck")) {
				sb.append("selenium.uncheck(\"").append(param2).append("\");\n");
			} else if (param1.equals("check")) {
				sb.append("selenium.check(\"").append(param2).append("\");\n");
			} else if (param1.equals("verifyChecked")) {
				sb.append("assertTrue(selenium.isChecked");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
                        } else if ( param1.equals("type")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
			}else if (param1.equals("windowMaximize")) {
				sb.append("selenium.windowMaximize()").append(";\n");
			}else if (param1.equals("echo")) {
				sb.append("System.out.println(\"" + param2 + "\");\n");
			}
		}
		sb.append("}\n\n");
	}

	private String getTimeoutMessage(String param1) {
		return "if (second >= timeoutSecInt)\n fail(\"" + param1 +" reached a timeout (\" + timeoutSecInt + \"s)\");\n";
	}
	
	public static void writeFile(String file, String content) throws IOException {
		System.out.println("[INFO] Writing file : " + file);
		FileUtils.writeStringToFile(new File(file), content);
	}

	private String[] getParams(String step) throws Exception {
		String[] params = new String[3];

		int x = 0;
		int y = 0;

		for (int i = 0; i < 3; i++) {
			x = step.indexOf("<td>", x) + 4;
			y = step.indexOf("\n", x);
			y = step.lastIndexOf("</td>", y);
			params[i] = StringEscapeUtils.unescapeHtml(step.substring(x, y));
		}

		return params;
	}

	private String replace(String s, String oldSub, String newSub) {
		if ((s == null) || (oldSub == null) || (newSub == null)) {
			return null;
		}
		int y = s.indexOf(oldSub);
		if (y >= 0) {
			StringBuffer sb = new StringBuffer(s.length() + 5 * newSub.length());
			int length = oldSub.length();
			int x = 0;
			while (x <= y) {
				sb.append(s.substring(x, y));
				sb.append(newSub);
				x = y + length;
				y = s.indexOf(oldSub, x);
			}
			sb.append(s.substring(x));
			return sb.toString();
		} else {
			return s;
		}
	}

	private String fixParam(String param) {
		StringBuffer sb = new StringBuffer();

		char[] array = param.toCharArray();

		for (int i = 0; i < array.length; ++i) {
			char c = array[i];

			if (c == '\\') {
				sb.append("\\\\");
			} else if (c == '"') {
				sb.append("\\\"");
			} else if (Character.isWhitespace(c)) {
				sb.append(c);
			} /*
				 * else if ((c < 0x0020) || (c > 0x007e)) { sb.append("\\u");
				 * sb.append(UnicodeFormatter.charToHex(c)); }
				 */else {
				sb.append(c);
			}
		}
		return replace(sb.toString(), _FIX_PARAM_OLD_SUBS, _FIX_PARAM_NEW_SUBS);
	}

	private String replace(String s, String[] oldSubs, String[] newSubs) {
		if ((s == null) || (oldSubs == null) || (newSubs == null)) {
			return null;
		}

		if (oldSubs.length != newSubs.length) {
			return s;
		}

		for (int i = 0; i < oldSubs.length; i++) {
			s = replace(s, oldSubs[i], newSubs[i]);
		}

		return s;
	}

	private static final String replaceSeparatorPattern = File.separator.equals("\\") ? "\\\\" : File.separator;
	private static final String[] _FIX_PARAM_OLD_SUBS = new String[] { "\\\\n", "<br />" };
	private static final String[] _FIX_PARAM_NEW_SUBS = new String[] { "\\n", "\\n" };
	public static final String SLASH = "/";

}
